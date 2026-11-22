package com.example.data.repository

import com.example.data.local.CapabilityEntity
import com.example.data.local.CapabilitySnapshotEntity
import com.example.data.local.DiscoveryEntity
import com.example.data.local.ExperimentEntity
import com.example.data.local.FavoriteComboEntity
import com.example.data.local.SavedAssessmentEntity
import com.example.data.local.ScoutDao
import com.example.data.local.ServiceEntity
import com.example.data.local.SubscriptionPlanEntity
import com.example.data.local.VerificationSourceEntity
import com.example.data.seed.SyntheticSeedData
import com.example.domain.model.EntitlementStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ServiceBundle(
    val service: ServiceEntity,
    val plans: List<SubscriptionPlanEntity>,
    val capabilities: List<CapabilityEntity>,
    val sources: List<VerificationSourceEntity>,
    val snapshots: List<CapabilitySnapshotEntity>
) {
    val primaryPlan: SubscriptionPlanEntity?
        get() = plans.firstOrNull { it.entitlementStatus.isVerifiedAccess } ?: plans.firstOrNull()

    val verifiedMonthlyFixedCostUsd: Double
        get() = plans
            .filter { it.entitlementStatus == EntitlementStatus.VERIFIED_ACTIVE && it.isUserVerifiedEntitlement }
            .mapNotNull { it.monthlyFixedCostUsd }
            .sum()
}

data class InventorySnapshot(
    val bundles: List<ServiceBundle> = emptyList(),
    val experiments: List<ExperimentEntity> = emptyList(),
    val discoveries: List<DiscoveryEntity> = emptyList(),
    val favoriteCombos: List<FavoriteComboEntity> = emptyList(),
    val savedAssessments: List<SavedAssessmentEntity> = emptyList()
)

data class PublicSourceRefreshResult(
    val httpStatus: Int?,
    val fetchedTitle: String?,
    val lastModifiedHeader: String?,
    val errorMessage: String?
)

class ScoutRepository(
    private val dao: ScoutDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val coreBundlesFlow: Flow<List<ServiceBundle>> = combine(
        dao.observeAllServices(),
        dao.observeAllPlans(),
        dao.observeAllCapabilities(),
        dao.observeAllSources(),
        dao.observeAllSnapshots()
    ) { services, plans, capabilities, sources, snapshots ->
        val plansByService = plans.groupBy { it.serviceId }
        val capsByService = capabilities.groupBy { it.serviceId }
        val sourcesByService = sources.groupBy { it.serviceId }
        val snapsByService = snapshots.groupBy { it.serviceId }
        services.map { svc ->
            ServiceBundle(
                service = svc,
                plans = plansByService[svc.id].orEmpty(),
                capabilities = capsByService[svc.id].orEmpty(),
                sources = sourcesByService[svc.id].orEmpty(),
                snapshots = snapsByService[svc.id].orEmpty().sortedByDescending { it.capturedAtEpochMs }
            )
        }
    }

    val inventoryFlow: Flow<InventorySnapshot> = combine(
        coreBundlesFlow,
        dao.observeAllExperiments(),
        dao.observeAllDiscoveries(),
        dao.observeAllCombos(),
        dao.observeAllAssessments()
    ) { bundles, experiments, discoveries, combos, assessments ->
        InventorySnapshot(
            bundles = bundles,
            experiments = experiments,
            discoveries = discoveries,
            favoriteCombos = combos,
            savedAssessments = assessments
        )
    }

    suspend fun ensureSeeded(nowEpochMs: Long = System.currentTimeMillis()) {
        SyntheticSeedData.populateIfEmpty(dao, nowEpochMs)
    }

    suspend fun resetToSyntheticSample(nowEpochMs: Long = System.currentTimeMillis()) {
        SyntheticSeedData.resetToSyntheticSample(dao, nowEpochMs)
    }

    suspend fun addServiceWithInitialData(
        service: ServiceEntity,
        plan: SubscriptionPlanEntity?,
        capability: CapabilityEntity?,
        source: VerificationSourceEntity?
    ): Long {
        val serviceId = dao.insertService(service)
        plan?.let { dao.insertPlan(it.copy(serviceId = serviceId)) }
        capability?.let { dao.insertCapability(it.copy(serviceId = serviceId)) }
        source?.let { dao.insertSource(it.copy(serviceId = serviceId)) }
        return serviceId
    }

    suspend fun updateService(service: ServiceEntity) = dao.updateService(service)
    suspend fun deleteService(serviceId: Long) = dao.deleteServiceById(serviceId)

    suspend fun upsertPlan(plan: SubscriptionPlanEntity) = dao.insertPlan(plan)
    suspend fun deletePlan(planId: Long) = dao.deletePlanById(planId)

    suspend fun upsertCapability(capability: CapabilityEntity) = dao.insertCapability(capability)
    suspend fun deleteCapability(capabilityId: Long) = dao.deleteCapabilityById(capabilityId)

    suspend fun upsertSource(source: VerificationSourceEntity) = dao.insertSource(source)
    suspend fun deleteSource(sourceId: Long) = dao.deleteSourceById(sourceId)

    suspend fun captureSnapshot(
        bundle: ServiceBundle,
        label: String,
        notes: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Long {
        val entries = bundle.capabilities.map { cap ->
            SyntheticSeedData.SnapshotCapabilityEntry(
                name = cap.capabilityName,
                type = cap.capabilityType,
                quota = cap.quotaSummary,
                usageModel = cap.usageModel,
                confidence = cap.confidence,
                restriction = cap.restrictionSummary
            )
        }
        val plan = bundle.primaryPlan
        return dao.insertSnapshot(
            CapabilitySnapshotEntity(
                serviceId = bundle.service.id,
                snapshotLabel = label.ifBlank { "Snapshot" },
                capturedAtEpochMs = nowEpochMs,
                planName = plan?.planName ?: "No Plan Recorded",
                entitlementStatus = plan?.entitlementStatus ?: EntitlementStatus.NOT_SUBSCRIBED,
                monthlyCostUsd = plan?.monthlyFixedCostUsd,
                capabilitiesJson = SyntheticSeedData.buildSnapshotCapabilitiesJson(entries),
                verificationSourceUrl = bundle.service.officialDocsUrl,
                notes = notes
            )
        )
    }

    suspend fun deleteSnapshot(snapshotId: Long) = dao.deleteSnapshotById(snapshotId)

    suspend fun addExperiment(experiment: ExperimentEntity) = dao.insertExperiment(experiment)
    suspend fun deleteExperiment(experimentId: Long) = dao.deleteExperimentById(experimentId)

    suspend fun addDiscovery(discovery: DiscoveryEntity) = dao.insertDiscovery(discovery)
    suspend fun updateDiscovery(discovery: DiscoveryEntity) = dao.updateDiscovery(discovery)
    suspend fun deleteDiscovery(discoveryId: Long) = dao.deleteDiscoveryById(discoveryId)

    suspend fun addFavoriteCombo(combo: FavoriteComboEntity) = dao.insertCombo(combo)
    suspend fun deleteFavoriteCombo(comboId: Long) = dao.deleteComboById(comboId)

    suspend fun saveAssessmentOffline(assessment: SavedAssessmentEntity) = dao.insertAssessment(assessment)
    suspend fun deleteAssessment(assessmentId: Long) = dao.deleteAssessmentById(assessmentId)

    /**
     * Fetches public HTTP status and HTML <title> / Last-Modified metadata from an official
     * documentation link when network access exists. Never auto-mutates prices or entitlements.
     */
    suspend fun refreshPublicSourceMetadata(source: VerificationSourceEntity): PublicSourceRefreshResult =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", "DEX-SCOUT-Research-Client/1.0")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val status = response.code
                    val lastModified = response.header("Last-Modified")
                    val snippet = response.body?.string()?.take(12_000).orEmpty()
                    val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        .find(snippet)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.replace(Regex("\\s+"), " ")
                        ?.trim()
                        ?.take(120)

                    val now = System.currentTimeMillis()
                    dao.updateSource(
                        source.copy(
                            lastHttpStatus = status,
                            lastFetchedTitle = titleMatch ?: lastModified,
                            lastCheckedEpochMs = now
                        )
                    )
                    PublicSourceRefreshResult(
                        httpStatus = status,
                        fetchedTitle = titleMatch,
                        lastModifiedHeader = lastModified,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                PublicSourceRefreshResult(
                    httpStatus = null,
                    fetchedTitle = null,
                    lastModifiedHeader = null,
                    errorMessage = e.localizedMessage ?: "Unable to reach public documentation URL."
                )
            }
        }
}
