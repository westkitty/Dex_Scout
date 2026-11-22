package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAnalysisState
import com.example.ai.GeminiScoutAnalyzer
import com.example.data.local.CapabilityEntity
import com.example.data.local.DiscoveryEntity
import com.example.data.local.ExperimentEntity
import com.example.data.local.FavoriteComboEntity
import com.example.data.local.SavedAssessmentEntity
import com.example.data.local.ScoutDatabase
import com.example.data.local.ServiceEntity
import com.example.data.local.SubscriptionPlanEntity
import com.example.data.local.VerificationSourceEntity
import com.example.data.repository.InventorySnapshot
import com.example.data.repository.ScoutRepository
import com.example.data.repository.ServiceBundle
import com.example.domain.CapabilityMatrixRow
import com.example.domain.ExportEngine
import com.example.domain.ExportFormat
import com.example.domain.FreshnessPolicy
import com.example.domain.GlobalSearchResult
import com.example.domain.MatrixFilterEngine
import com.example.domain.MatrixFilterState
import com.example.domain.RankedCandidate
import com.example.domain.RecommendationEngine
import com.example.domain.RedundancyAnalyzer
import com.example.domain.RedundancyFinding
import com.example.domain.SnapshotDiffEngine
import com.example.domain.SnapshotDiffResult
import com.example.domain.TaskSpecification
import com.example.ui.util.AndroidActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeltaSpotlight(
    val serviceId: Long,
    val serviceName: String,
    val diff: SnapshotDiffResult
)

data class ScoutUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = false,
    val darkTheme: Boolean = true,
    val inventory: InventorySnapshot = InventorySnapshot(),
    val matrixFilter: MatrixFilterState = MatrixFilterState(),
    val matrixRows: List<CapabilityMatrixRow> = emptyList(),
    val redundancyFindings: List<RedundancyFinding> = emptyList(),
    val taskSpec: TaskSpecification = TaskSpecification(
        taskTitle = "Remote Android Build & Test",
        taskDescription = "Compile Kotlin/Compose project and run headless tests from a thin client under $5 incremental spend."
    ),
    val rankedCandidates: List<RankedCandidate> = emptyList(),
    val globalSearchQuery: String = "",
    val globalSearchResults: List<GlobalSearchResult> = emptyList(),
    val geminiState: GeminiAnalysisState = GeminiAnalysisState.Idle,
    val refreshingSourceId: Long? = null,
    val statusBannerMessage: String? = null,
    val verifiedCapabilityCount: Int = 0,
    val staleItemCount: Int = 0,
    val inferredEntitlementCount: Int = 0,
    val verifiedMonthlyFixedSpendUsd: Double = 0.0,
    val deltaSpotlights: List<DeltaSpotlight> = emptyList(),
    val hasSyntheticSamples: Boolean = false
)

class ScoutViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ScoutDatabase.getInstance(application)
    val repository = ScoutRepository(database.scoutDao())
    private val geminiAnalyzer = GeminiScoutAnalyzer()

    private val matrixFilterFlow = MutableStateFlow(MatrixFilterState())
    private val taskSpecFlow = MutableStateFlow(
        TaskSpecification(
            taskTitle = "Remote Android Build & Test",
            taskDescription = "Compile Kotlin/Compose project and run headless tests from a thin client under $5 incremental spend."
        )
    )
    private val globalSearchFlow = MutableStateFlow("")
    private val geminiStateFlow = MutableStateFlow<GeminiAnalysisState>(GeminiAnalysisState.Idle)
    private val refreshingSourceIdFlow = MutableStateFlow<Long?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val darkThemeFlow = MutableStateFlow(true)
    private val isOnlineFlow = AndroidActions.observeNetworkOnline(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var geminiJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private data class AuxiliaryState(
        val searchQuery: String,
        val geminiState: GeminiAnalysisState,
        val refreshingSourceId: Long?,
        val statusMessage: String?,
        val isOnline: Boolean,
        val darkTheme: Boolean
    )

    private val auxiliaryFlow = combine(
        globalSearchFlow,
        geminiStateFlow,
        refreshingSourceIdFlow,
        statusMessageFlow,
        isOnlineFlow,
        darkThemeFlow
    ) { arr ->
        AuxiliaryState(
            searchQuery = arr[0] as String,
            geminiState = arr[1] as GeminiAnalysisState,
            refreshingSourceId = arr[2] as Long?,
            statusMessage = arr[3] as String?,
            isOnline = arr[4] as Boolean,
            darkTheme = arr[5] as Boolean
        )
    }

    val uiState: StateFlow<ScoutUiState> = combine(
        repository.inventoryFlow,
        matrixFilterFlow,
        taskSpecFlow,
        auxiliaryFlow
    ) { inventory, filter, taskSpec, aux ->
        val now = System.currentTimeMillis()
        val matrixRows = MatrixFilterEngine.filterAndSort(inventory.bundles, filter, now)
        val redundancies = RedundancyAnalyzer.analyze(inventory.bundles)
        val ranked = RecommendationEngine.rank(inventory.bundles, taskSpec, now)
        val searchResults = MatrixFilterEngine.fullTextSearch(inventory, aux.searchQuery)

        val allCaps = inventory.bundles.flatMap { it.capabilities }
        val verifiedCaps = allCaps.count { it.isVerified }
        val staleCaps = allCaps.count { FreshnessPolicy.isStale(it.verificationDateEpochMs, now) }
        val staleSources = inventory.bundles.flatMap { it.sources }
            .count { FreshnessPolicy.isStale(it.lastVerifiedDateEpochMs, now) }
        val inferredPlans = inventory.bundles.flatMap { it.plans }
            .count { !it.entitlementStatus.isVerifiedAccess && it.entitlementStatus.name == "INFERRED_UNVERIFIED" }
        val monthlySpend = inventory.bundles.sumOf { it.verifiedMonthlyFixedCostUsd }

        val spotlights = inventory.bundles.mapNotNull { bundle ->
            if (bundle.snapshots.size >= 2) {
                val sorted = bundle.snapshots.sortedBy { it.capturedAtEpochMs }
                val diff = SnapshotDiffEngine.diffSnapshots(sorted[sorted.size - 2], sorted.last())
                if (diff.hasAnyChanges) DeltaSpotlight(bundle.service.id, bundle.service.name, diff) else null
            } else null
        }

        ScoutUiState(
            isLoading = false,
            isOnline = aux.isOnline,
            darkTheme = aux.darkTheme,
            inventory = inventory,
            matrixFilter = filter,
            matrixRows = matrixRows,
            redundancyFindings = redundancies,
            taskSpec = taskSpec,
            rankedCandidates = ranked,
            globalSearchQuery = aux.searchQuery,
            globalSearchResults = searchResults,
            geminiState = aux.geminiState,
            refreshingSourceId = aux.refreshingSourceId,
            statusBannerMessage = aux.statusMessage,
            verifiedCapabilityCount = verifiedCaps,
            staleItemCount = staleCaps + staleSources,
            inferredEntitlementCount = inferredPlans,
            verifiedMonthlyFixedSpendUsd = monthlySpend,
            deltaSpotlights = spotlights,
            hasSyntheticSamples = inventory.bundles.any { it.service.isSyntheticSample }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScoutUiState()
    )

    fun toggleTheme() {
        darkThemeFlow.update { !it }
    }

    fun dismissStatusMessage() {
        statusMessageFlow.value = null
    }

    fun updateMatrixFilter(transform: (MatrixFilterState) -> MatrixFilterState) {
        matrixFilterFlow.update(transform)
    }

    fun resetMatrixFilter() {
        matrixFilterFlow.value = MatrixFilterState()
    }

    fun updateTaskSpec(transform: (TaskSpecification) -> TaskSpecification) {
        taskSpecFlow.update(transform)
        geminiStateFlow.value = GeminiAnalysisState.Idle
    }

    fun updateGlobalSearch(query: String) {
        globalSearchFlow.value = query
    }

    fun addServiceBundle(
        service: ServiceEntity,
        plan: SubscriptionPlanEntity?,
        capability: CapabilityEntity?,
        source: VerificationSourceEntity?
    ) {
        viewModelScope.launch {
            repository.addServiceWithInitialData(service, plan, capability, source)
            statusMessageFlow.value = "Saved ${service.name} offline to Room inventory."
        }
    }

    fun deleteService(serviceId: Long) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
            statusMessageFlow.value = "Removed service from inventory."
        }
    }

    fun upsertPlan(plan: SubscriptionPlanEntity) {
        viewModelScope.launch {
            repository.upsertPlan(plan)
            statusMessageFlow.value = "Updated plan '${plan.planName}'."
        }
    }

    fun upsertCapability(capability: CapabilityEntity) {
        viewModelScope.launch {
            repository.upsertCapability(capability)
            statusMessageFlow.value = "Saved capability '${capability.capabilityName}'."
        }
    }

    fun markCapabilityVerifiedNow(capability: CapabilityEntity) {
        viewModelScope.launch {
            repository.upsertCapability(
                capability.copy(
                    verificationDateEpochMs = System.currentTimeMillis(),
                    isVerified = true,
                    confidence = com.example.domain.model.ConfidenceLevel.VERIFIED_HIGH
                )
            )
            statusMessageFlow.value = "Marked '${capability.capabilityName}' verified today."
        }
    }

    fun upsertSource(source: VerificationSourceEntity) {
        viewModelScope.launch {
            repository.upsertSource(source)
            statusMessageFlow.value = "Saved verification link '${source.title}'."
        }
    }

    fun refreshPublicSource(source: VerificationSourceEntity) {
        viewModelScope.launch {
            refreshingSourceIdFlow.value = source.id
            val result = repository.refreshPublicSourceMetadata(source)
            refreshingSourceIdFlow.value = null
            statusMessageFlow.value = if (result.errorMessage != null) {
                "Header check failed: ${result.errorMessage}"
            } else {
                "HTTP ${result.httpStatus}: ${result.fetchedTitle ?: "Reachable"} (prices/quotas unchanged — verify manually)."
            }
        }
    }

    fun captureSnapshot(bundle: ServiceBundle, label: String, notes: String) {
        viewModelScope.launch {
            repository.captureSnapshot(bundle, label, notes)
            statusMessageFlow.value = "Captured snapshot '$label' for ${bundle.service.name}."
        }
    }

    fun addExperiment(experiment: ExperimentEntity) {
        viewModelScope.launch {
            repository.addExperiment(experiment)
            statusMessageFlow.value = "Logged experiment '${experiment.title}'."
        }
    }

    fun deleteExperiment(id: Long) {
        viewModelScope.launch { repository.deleteExperiment(id) }
    }

    fun addDiscovery(discovery: DiscoveryEntity) {
        viewModelScope.launch {
            repository.addDiscovery(discovery)
            statusMessageFlow.value = "Recorded discovery '${discovery.title}'."
        }
    }

    fun updateDiscovery(discovery: DiscoveryEntity) {
        viewModelScope.launch { repository.updateDiscovery(discovery) }
    }

    fun deleteDiscovery(id: Long) {
        viewModelScope.launch { repository.deleteDiscovery(id) }
    }

    fun addFavoriteCombo(combo: FavoriteComboEntity) {
        viewModelScope.launch {
            repository.addFavoriteCombo(combo)
            statusMessageFlow.value = "Saved tool stack '${combo.name}'."
        }
    }

    fun deleteFavoriteCombo(id: Long) {
        viewModelScope.launch { repository.deleteFavoriteCombo(id) }
    }

    fun saveCurrentAssessmentOffline(customTitle: String? = null) {
        val state = uiState.value
        val spec = state.taskSpec
        val ranked = state.rankedCandidates
        val topPick = ranked.firstOrNull { !it.isDisqualified }?.bundle?.service?.name
            ?: ranked.firstOrNull()?.bundle?.service?.name
            ?: "None"

        val md = buildString {
            appendLine("# ${customTitle?.takeIf { it.isNotBlank() } ?: spec.taskTitle.ifBlank { "Task Comparison" }}")
            appendLine("Task: ${spec.taskDescription.ifBlank { "Custom specification" }}")
            appendLine("Required: ${spec.requiredCapabilities.joinToString { it.label }}")
            appendLine()
            ranked.forEachIndexed { idx, r ->
                val status = if (r.isDisqualified) "DISQUALIFIED" else "RANK #${idx + 1}"
                val cost = r.estimatedIncrementalCostUsd?.let { "$${"%.2f".format(it)} est." } ?: "Unverified cost"
                appendLine("- **$status · ${r.bundle.service.name}** (${r.totalScore}/100 | ${r.recommendedEnvironment.label} | $cost)")
                if (r.disqualifierReasons.isNotEmpty()) {
                    appendLine("  - Blockers: ${r.disqualifierReasons.joinToString("; ")}")
                }
                if (r.warnings.isNotEmpty()) {
                    appendLine("  - Warnings: ${r.warnings.joinToString("; ")}")
                }
            }
        }

        val geminiText = when (val g = state.geminiState) {
            is GeminiAnalysisState.Success -> g.text
            is GeminiAnalysisState.Streaming -> g.text
            else -> null
        }

        viewModelScope.launch {
            repository.saveAssessmentOffline(
                SavedAssessmentEntity(
                    title = customTitle?.takeIf { it.isNotBlank() } ?: spec.taskTitle.ifBlank { "Saved Task Assessment" },
                    taskQuery = spec.taskDescription.ifBlank { spec.requiredCapabilities.joinToString { it.label } },
                    requirementsSummary = "Req: ${spec.requiredCapabilities.joinToString { it.shortLabel }} | Cloud: ${if (spec.requireCloudExecution) "Yes" else "Optional"} | HW: ${spec.maxLocalHardware.label}",
                    topRecommendationName = topPick,
                    deterministicSummaryMarkdown = md,
                    optionalGeminiAnalysis = geminiText,
                    isOfflinePinned = true
                )
            )
            statusMessageFlow.value = "Saved assessment for offline reading."
        }
    }

    fun deleteAssessment(id: Long) {
        viewModelScope.launch { repository.deleteAssessment(id) }
    }

    fun runOptionalGeminiBrief() {
        val state = uiState.value
        if (state.rankedCandidates.isEmpty()) {
            geminiStateFlow.value = GeminiAnalysisState.Error("Add at least one service to your inventory before running optional Gemini analysis.")
            return
        }
        geminiJob?.cancel()
        geminiJob = viewModelScope.launch {
            geminiAnalyzer.streamTaskAnalysis(state.taskSpec, state.rankedCandidates).collect { aiState ->
                geminiStateFlow.value = aiState
            }
        }
    }

    fun clearGeminiAnalysis() {
        geminiJob?.cancel()
        geminiStateFlow.value = GeminiAnalysisState.Idle
    }

    fun resetToSyntheticSample() {
        viewModelScope.launch {
            repository.resetToSyntheticSample()
            statusMessageFlow.value = "Reset inventory to Synthetic Sample Dataset."
        }
    }

    fun buildExportPayload(format: ExportFormat): String {
        return ExportEngine.export(uiState.value.inventory, format)
    }
}
