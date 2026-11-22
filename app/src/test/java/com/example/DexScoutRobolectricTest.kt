package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ScoutDatabase
import com.example.data.local.ServiceEntity
import com.example.data.repository.ScoutRepository
import com.example.domain.DeltaKind
import com.example.domain.ExportEngine
import com.example.domain.ExportFormat
import com.example.domain.FreshnessPolicy
import com.example.domain.MatrixFilterEngine
import com.example.domain.MatrixFilterState
import com.example.domain.RecommendationEngine
import com.example.domain.RedundancyAnalyzer
import com.example.domain.SnapshotDiffEngine
import com.example.domain.TaskSpecification
import com.example.domain.model.CapabilityType
import com.example.domain.model.HardwareTier
import com.example.domain.model.MatrixSortOption
import com.example.domain.model.PrivacyTier
import com.example.domain.model.ServiceCategory
import com.example.domain.model.UsageModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DexScoutRobolectricTest {

    private lateinit var db: ScoutDatabase
    private lateinit var repository: ScoutRepository
    private val fixedNowMs = 1_795_000_000_000L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ScoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScoutRepository(db.scoutDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun offlinePersistence_seedsSyntheticInventoryAndStoresNewRecords() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val initial = repository.inventoryFlow.first()

        assertTrue("Synthetic inventory should seed services offline", initial.bundles.size >= 6)
        assertTrue("Saved offline assessments should be readable without network", initial.savedAssessments.isNotEmpty())
        assertTrue("Experiments should be persisted offline", initial.experiments.isNotEmpty())

        val newId = repository.addServiceWithInitialData(
            service = ServiceEntity(
                name = "Offline Custom Runner",
                vendor = "Self Hosted",
                category = ServiceCategory.BUILD_CI,
                summary = "Local custom verification entry",
                officialDocsUrl = "https://docs.gradle.org",
                pricingUrl = "https://docs.gradle.org",
                isCloudHosted = false,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = true,
                supportsRemoteTest = true,
                supportsRemoteRender = false,
                supportsRemoteCompute = true,
                localHardwareRequirement = HardwareTier.MEDIUM,
                storageRequirementGb = 4.0,
                privacyTier = PrivacyTier.LOCAL_ONLY,
                isSyntheticSample = false,
                createdAtEpochMs = fixedNowMs,
                updatedAtEpochMs = fixedNowMs
            ),
            plan = null,
            capability = null,
            source = null
        )

        val updated = repository.inventoryFlow.first()
        assertTrue(updated.bundles.any { it.service.id == newId && it.service.name == "Offline Custom Runner" })
    }

    @Test
    fun rankingEngine_distinguishesInferenceOnlyFromRemoteBuildAndTest() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        val spec = TaskSpecification(
            taskTitle = "Remote Android Build & Test",
            requiredCapabilities = setOf(CapabilityType.REMOTE_BUILD, CapabilityType.REMOTE_TEST),
            maxIncrementalCostUsd = 5.0,
            preferExistingSubscriptions = true,
            requireCloudExecution = true,
            maxLocalHardware = HardwareTier.NONE,
            maxStorageGb = 4.0,
            minPrivacyTier = PrivacyTier.STANDARD_CLOUD
        )

        val ranked = RecommendationEngine.rank(inventory.bundles, spec, fixedNowMs)
        assertTrue(ranked.isNotEmpty())

        val top = ranked.first()
        assertEquals("ForgeRunner Cloud CI [Sample]", top.bundle.service.name)
        assertFalse("Top remote build service must not be disqualified", top.isDisqualified)
        assertTrue("Top remote build service should score highly", top.totalScore >= 80)

        val nova = ranked.first { it.bundle.service.name.contains("NovaLLM") }
        assertTrue("Inference-only service must be disqualified for remote build/test", nova.isDisqualified)
        assertTrue("Inference-only mismatch flag must be set", nova.isInferenceOnlyMismatch)
    }

    @Test
    fun rankingEngine_neverTreatsInferredEntitlementAsVerified_andFlagsStaleSources() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        val spec = TaskSpecification(
            taskTitle = "GPU Frame Rendering",
            requiredCapabilities = setOf(CapabilityType.REMOTE_RENDER),
            maxIncrementalCostUsd = 10.0,
            preferExistingSubscriptions = true,
            requireCloudExecution = true,
            maxLocalHardware = HardwareTier.NONE,
            maxStorageGb = 4.0,
            minPrivacyTier = PrivacyTier.STANDARD_CLOUD
        )

        val ranked = RecommendationEngine.rank(inventory.bundles, spec, fixedNowMs)
        val aether = ranked.first { it.bundle.service.name.contains("AetherRender") }

        assertFalse("Inferred plan MUST NEVER be treated as verified entitlement", aether.hasVerifiedEntitlement)
        assertTrue("Inferred entitlement warning must be emitted", aether.hasInferredEntitlementWarning)
        assertTrue("68-day-old source must trigger stale evidence warning", aether.hasStaleEvidenceWarning)
        assertTrue(aether.warnings.any { it.contains("Stale", ignoreCase = true) })
        assertTrue(aether.warnings.any { it.contains("INFERRED", ignoreCase = true) })
    }

    @Test
    fun rankingEngine_airGappedPrivacyPrefersLocalRuntime() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        val spec = TaskSpecification(
            taskTitle = "Air-Gapped Local Synthesis",
            requiredCapabilities = setOf(CapabilityType.MODEL_INFERENCE),
            maxIncrementalCostUsd = 0.0,
            requireCloudExecution = false,
            maxLocalHardware = HardwareTier.HIGH_GPU,
            maxStorageGb = 32.0,
            minPrivacyTier = PrivacyTier.LOCAL_ONLY
        )

        val ranked = RecommendationEngine.rank(inventory.bundles, spec, fixedNowMs)
        assertEquals("EdgeBox Local Runtime [Sample]", ranked.first().bundle.service.name)
        assertFalse(ranked.first().isDisqualified)
    }

    @Test
    fun filtersAndSearch_handleStaleOnlyRemoteComputeFreeUsageAndFullText() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        // Stale-only filter
        val staleRows = MatrixFilterEngine.filterAndSort(
            bundles = inventory.bundles,
            filter = MatrixFilterState(staleOnly = true),
            nowEpochMs = fixedNowMs
        )
        assertEquals(1, staleRows.size)
        assertTrue(staleRows.first().bundle.service.name.contains("AetherRender"))
        assertTrue(staleRows.first().isStale)
        assertTrue(FreshnessPolicy.isStale(staleRows.first().capability.verificationDateEpochMs, fixedNowMs))

        // Remote compute only filter excludes model-inference-only rows
        val remoteComputeRows = MatrixFilterEngine.filterAndSort(
            bundles = inventory.bundles,
            filter = MatrixFilterState(remoteComputeOnly = true),
            nowEpochMs = fixedNowMs
        )
        assertTrue(remoteComputeRows.isNotEmpty())
        assertTrue(remoteComputeRows.all { it.capability.capabilityType.isRemoteComputeExecution })

        // Free usage + cost sort
        val freeRows = MatrixFilterEngine.filterAndSort(
            bundles = inventory.bundles,
            filter = MatrixFilterState(usageModel = UsageModel.FREE, sortOption = MatrixSortOption.COST_ASC),
            nowEpochMs = fixedNowMs
        )
        assertTrue(freeRows.isNotEmpty())
        assertTrue(freeRows.all { it.capability.usageModel == UsageModel.FREE })

        // Full-text search across experiments & restrictions
        val searchResults = MatrixFilterEngine.fullTextSearch(inventory, "preempted")
        assertTrue("Full-text search should match experiment observations", searchResults.any { it.typeBadge == "EXPERIMENT" })
    }

    @Test
    fun snapshotDiff_andRedundancyAnalyzer_detectDeltasAndPaidOverlap() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        val nova = inventory.bundles.first { it.service.name.contains("NovaLLM") }
        val sortedSnaps = nova.snapshots.sortedBy { it.capturedAtEpochMs }
        assertTrue(sortedSnaps.size >= 2)

        val diff = SnapshotDiffEngine.diffSnapshots(sortedSnaps[0], sortedSnaps[1])
        assertTrue(diff.hasAnyChanges)
        assertEquals(1, diff.addedCount)
        assertEquals(1, diff.removedCount)
        assertTrue(diff.capabilityDeltas.any { it.kind == DeltaKind.CHANGED && it.capabilityName.contains("Long-Context") })

        val redundancies = RedundancyAnalyzer.analyze(inventory.bundles)
        assertTrue("Overlapping paid AI_MODEL subscriptions should trigger redundancy finding", redundancies.isNotEmpty())
        assertEquals(CapabilityType.MODEL_INFERENCE, redundancies.first().overlappingCapability)
        assertEquals(19.0, redundancies.first().potentialSavingsUsd, 0.01)
    }

    @Test
    fun exportEngine_generatesValidMarkdownCsvAndJson() = runTest {
        repository.ensureSeeded(fixedNowMs)
        val inventory = repository.inventoryFlow.first()

        val md = ExportEngine.export(inventory, ExportFormat.MARKDOWN, fixedNowMs)
        assertTrue(md.contains("# DEX//SCOUT Intelligence Export"))
        assertTrue(md.contains("| Service | Plan & Entitlement |"))
        assertTrue(md.contains("STALE"))

        val csv = ExportEngine.export(inventory, ExportFormat.CSV, fixedNowMs)
        val csvLines = csv.trim().lines()
        assertTrue(csvLines.size > 5)
        assertTrue(csvLines.first().startsWith("service_name,vendor,category"))
        assertTrue(csv.contains("INFERRED_UNVERIFIED"))

        val jsonText = ExportEngine.export(inventory, ExportFormat.JSON, fixedNowMs)
        val parsed = JSONObject(jsonText)
        assertEquals("dex-scout-export-v1", parsed.getString("schema"))
        val services = parsed.getJSONArray("services")
        assertTrue(services.length() >= 6)
        assertNotNull(parsed.getJSONArray("experiments"))
    }
}
