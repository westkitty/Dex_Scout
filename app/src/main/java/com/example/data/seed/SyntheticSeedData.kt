package com.example.data.seed

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
import com.example.domain.model.BillingType
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiscoveryStatus
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.ExperimentOutcome
import com.example.domain.model.HardwareTier
import com.example.domain.model.PrivacyTier
import com.example.domain.model.ServiceCategory
import com.example.domain.model.SourceType
import com.example.domain.model.UsageModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Populates an explicitly labeled SYNTHETIC SAMPLE INVENTORY so users and automated tests
 * can explore rankings, stale-source warnings, inferred-entitlement guards, snapshot deltas,
 * redundancy detection, and exports without inventing any real live vendor pricing or quotas.
 */
object SyntheticSeedData {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun buildSnapshotCapabilitiesJson(items: List<SnapshotCapabilityEntry>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("type", item.type.name)
                    put("quota", item.quota)
                    put("usageModel", item.usageModel.name)
                    put("confidence", item.confidence.name)
                    put("restriction", item.restriction)
                }
            )
        }
        return array.toString()
    }

    data class SnapshotCapabilityEntry(
        val name: String,
        val type: CapabilityType,
        val quota: String,
        val usageModel: UsageModel,
        val confidence: ConfidenceLevel,
        val restriction: String
    )

    suspend fun populateIfEmpty(dao: ScoutDao, nowEpochMs: Long = System.currentTimeMillis()) {
        if (dao.getServiceCount() > 0) return
        populateSampleInventory(dao, nowEpochMs)
    }

    suspend fun resetToSyntheticSample(dao: ScoutDao, nowEpochMs: Long = System.currentTimeMillis()) {
        dao.clearAllServices()
        dao.clearAllDiscoveries()
        dao.clearAllCombos()
        dao.clearAllAssessments()
        dao.clearAllExperiments()
        populateSampleInventory(dao, nowEpochMs)
    }

    suspend fun populateSampleInventory(dao: ScoutDao, nowEpochMs: Long = System.currentTimeMillis()) {
        val recentDate = nowEpochMs - (4L * DAY_MS)
        val mediumDate = nowEpochMs - (14L * DAY_MS)
        val staleDate = nowEpochMs - (68L * DAY_MS)
        val oldSnapshotDate = nowEpochMs - (45L * DAY_MS)
        val midSnapshotDate = nowEpochMs - (12L * DAY_MS)

        // 1. NovaLLM Studio (Sample) — Model Inference ONLY (does NOT execute remote builds/tests!)
        val novaId = dao.insertService(
            ServiceEntity(
                name = "NovaLLM Studio [Sample]",
                vendor = "Synthetic Labs",
                category = ServiceCategory.AI_MODEL,
                summary = "Synthetic sample LLM inference API & chat workspace. Strictly model text/vision inference — cannot run remote compilation, Gradle builds, or GPU rendering.",
                officialDocsUrl = "https://developer.android.com/docs",
                pricingUrl = "https://firebase.google.com/docs/ai-logic",
                isCloudHosted = true,
                supportsModelInferenceOnly = true,
                supportsRemoteBuild = false,
                supportsRemoteTest = false,
                supportsRemoteRender = false,
                supportsRemoteCompute = false,
                localHardwareRequirement = HardwareTier.NONE,
                storageRequirementGb = 0.2,
                privacyTier = PrivacyTier.ZERO_DATA_RETENTION,
                isSyntheticSample = true,
                createdAtEpochMs = oldSnapshotDate,
                updatedAtEpochMs = recentDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = novaId,
                planName = "Sample Pro Seat (Synthetic)",
                entitlementStatus = EntitlementStatus.VERIFIED_ACTIVE,
                billingType = BillingType.PAID_FIXED,
                monthlyFixedCostUsd = 20.0,
                meteredUnitCostDesc = "Included web seat; synthetic API overflow metered",
                isUserVerifiedEntitlement = true,
                notes = "Synthetic user-verified active seat for long-context reasoning."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = novaId,
                capabilityName = "Long-Context Code & Research Inference",
                capabilityType = CapabilityType.MODEL_INFERENCE,
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                quotaSummary = "Synthetic 120 prompts / 3h rolling window",
                usageModel = UsageModel.INCLUDED,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "included in seat",
                verificationSourceUrl = "https://firebase.google.com/docs/ai-logic",
                verificationDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "Inference only: no persistent shell, no remote build/test execution.",
                alternativesNote = "Pair with ForgeRunner Cloud CI for actual compilation & tests.",
                notes = "Verified via synthetic sample runbook."
            )
        )
        dao.insertSource(
            VerificationSourceEntity(
                serviceId = novaId,
                title = "Synthetic Model Capability Reference",
                url = "https://firebase.google.com/docs/ai-logic",
                sourceType = SourceType.OFFICIAL_DOCS,
                lastVerifiedDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                excerptNote = "Synthetic entry documenting inference-only boundaries and retention controls."
            )
        )
        dao.insertSnapshot(
            CapabilitySnapshotEntity(
                serviceId = novaId,
                snapshotLabel = "2026-Q3 Baseline",
                capturedAtEpochMs = oldSnapshotDate,
                planName = "Sample Pro Seat (Synthetic)",
                entitlementStatus = EntitlementStatus.VERIFIED_ACTIVE,
                monthlyCostUsd = 20.0,
                capabilitiesJson = buildSnapshotCapabilitiesJson(
                    listOf(
                        SnapshotCapabilityEntry(
                            name = "Long-Context Code & Research Inference",
                            type = CapabilityType.MODEL_INFERENCE,
                            quota = "Synthetic 60 prompts / 3h rolling window",
                            usageModel = UsageModel.INCLUDED,
                            confidence = ConfidenceLevel.PROBABLE_MEDIUM,
                            restriction = "Inference only; 64k context cap in sample baseline."
                        ),
                        SnapshotCapabilityEntry(
                            name = "Legacy Batch Summarizer",
                            type = CapabilityType.MODEL_INFERENCE,
                            quota = "Synthetic 10 jobs/day",
                            usageModel = UsageModel.FREE,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "Deprecated in Q4 sample update."
                        )
                    )
                ),
                verificationSourceUrl = "https://firebase.google.com/docs/ai-logic",
                notes = "Baseline synthetic snapshot before Q4 quota expansion."
            )
        )
        dao.insertSnapshot(
            CapabilitySnapshotEntity(
                serviceId = novaId,
                snapshotLabel = "2026-Q4 Verified",
                capturedAtEpochMs = midSnapshotDate,
                planName = "Sample Pro Seat (Synthetic)",
                entitlementStatus = EntitlementStatus.VERIFIED_ACTIVE,
                monthlyCostUsd = 20.0,
                capabilitiesJson = buildSnapshotCapabilitiesJson(
                    listOf(
                        SnapshotCapabilityEntry(
                            name = "Long-Context Code & Research Inference",
                            type = CapabilityType.MODEL_INFERENCE,
                            quota = "Synthetic 120 prompts / 3h rolling window",
                            usageModel = UsageModel.INCLUDED,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "Inference only: no persistent shell, no remote build/test execution."
                        ),
                        SnapshotCapabilityEntry(
                            name = "Structured JSON Extraction",
                            type = CapabilityType.MODEL_INFERENCE,
                            quota = "Synthetic 500 req/day",
                            usageModel = UsageModel.METERED,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "Requires schema definition; no code execution."
                        )
                    )
                ),
                verificationSourceUrl = "https://firebase.google.com/docs/ai-logic",
                notes = "Q4 update doubled rolling window and replaced legacy batch mode with structured JSON."
            )
        )

        // 2. PrismCode AI Plus [Sample] — Second paid AI_MODEL seat to demonstrate Redundancy Detection!
        val prismId = dao.insertService(
            ServiceEntity(
                name = "PrismCode Assistant [Sample]",
                vendor = "Synthetic DevTools",
                category = ServiceCategory.AI_MODEL,
                summary = "Synthetic IDE chat and inference subscription overlapping with NovaLLM Studio on model inference, plus limited sandboxed script preview.",
                officialDocsUrl = "https://developer.android.com/studio",
                pricingUrl = "https://developer.android.com/studio",
                isCloudHosted = true,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = false,
                supportsRemoteTest = false,
                supportsRemoteRender = false,
                supportsRemoteCompute = true,
                localHardwareRequirement = HardwareTier.LOW,
                storageRequirementGb = 1.5,
                privacyTier = PrivacyTier.STANDARD_CLOUD,
                isSyntheticSample = true,
                createdAtEpochMs = oldSnapshotDate,
                updatedAtEpochMs = mediumDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = prismId,
                planName = "Sample Dev Monthly (Synthetic)",
                entitlementStatus = EntitlementStatus.VERIFIED_ACTIVE,
                billingType = BillingType.PAID_FIXED,
                monthlyFixedCostUsd = 19.0,
                meteredUnitCostDesc = "Fixed synthetic subscription",
                isUserVerifiedEntitlement = true,
                notes = "Potential overlap with NovaLLM Studio for pure inference tasks."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = prismId,
                capabilityName = "Inline Code Completion & Chat",
                capabilityType = CapabilityType.MODEL_INFERENCE,
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                quotaSummary = "Synthetic unlimited autocomplete + 300 chat turns/mo",
                usageModel = UsageModel.INCLUDED,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "included",
                verificationSourceUrl = "https://developer.android.com/studio",
                verificationDateEpochMs = mediumDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "Cannot execute full Android/Gradle remote builds.",
                alternativesNote = "Overlaps with NovaLLM Studio for chat inference.",
                notes = "Synthetic sample entry."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = prismId,
                capabilityName = "Ephemeral Python Sandbox",
                capabilityType = CapabilityType.AGENT_SANDBOX,
                executionEnvironment = ExecutionEnvironment.REMOTE_CONTAINER,
                quotaSummary = "Synthetic 20 runs/day, 60s timeout, no outbound network",
                usageModel = UsageModel.INCLUDED,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "included",
                verificationSourceUrl = "https://developer.android.com/studio",
                verificationDateEpochMs = mediumDate,
                confidence = ConfidenceLevel.PROBABLE_MEDIUM,
                isVerified = true,
                restrictionSummary = "No Docker, no GPU, 512MB RAM cap.",
                alternativesNote = "Use ForgeRunner Cloud CI for full container builds.",
                notes = "Verified for lightweight script checks only."
            )
        )

        // 3. ForgeRunner Cloud CI [Sample] — Real Remote Build + Remote Test runner (Verified Free + Metered)
        val forgeId = dao.insertService(
            ServiceEntity(
                name = "ForgeRunner Cloud CI [Sample]",
                vendor = "Synthetic Forge",
                category = ServiceCategory.BUILD_CI,
                summary = "Synthetic Linux/JVM cloud build and automated test runner. Executes real remote Gradle builds and unit test suites with zero local hardware load.",
                officialDocsUrl = "https://docs.gradle.org",
                pricingUrl = "https://docs.gradle.org",
                isCloudHosted = true,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = true,
                supportsRemoteTest = true,
                supportsRemoteRender = false,
                supportsRemoteCompute = true,
                localHardwareRequirement = HardwareTier.NONE,
                storageRequirementGb = 0.5,
                privacyTier = PrivacyTier.ENTERPRISE_SOC2,
                isSyntheticSample = true,
                createdAtEpochMs = oldSnapshotDate,
                updatedAtEpochMs = recentDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = forgeId,
                planName = "Sample Builder Free + Metered",
                entitlementStatus = EntitlementStatus.FREE_TIER,
                billingType = BillingType.HYBRID,
                monthlyFixedCostUsd = 0.0,
                meteredUnitCostDesc = "Synthetic 2,000 free Linux min/mo; $0.008/min metered overflow",
                isUserVerifiedEntitlement = true,
                notes = "Verified free minutes sufficient for personal Android/Kotlin builds."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = forgeId,
                capabilityName = "Remote Android & Kotlin Gradle Build",
                capabilityType = CapabilityType.REMOTE_BUILD,
                executionEnvironment = ExecutionEnvironment.REMOTE_CONTAINER,
                quotaSummary = "Synthetic 2,000 Linux runner min/mo included",
                usageModel = UsageModel.FREE,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "within free tier",
                verificationSourceUrl = "https://docs.gradle.org",
                verificationDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "Max 6h job timeout; 14GB runner disk cap.",
                alternativesNote = "EdgeBox Local Runtime for offline builds.",
                notes = "Actual remote compilation verified by experiment."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = forgeId,
                capabilityName = "Automated Headless Test Matrix",
                capabilityType = CapabilityType.REMOTE_TEST,
                executionEnvironment = ExecutionEnvironment.REMOTE_CONTAINER,
                quotaSummary = "Synthetic shared 2,000 min pool; parallel shards metered",
                usageModel = UsageModel.METERED,
                incrementalCostEstimateUsd = 0.40,
                costUnitLabel = "per 50-min matrix run",
                verificationSourceUrl = "https://docs.gradle.org",
                verificationDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "No physical hardware sensor access.",
                alternativesNote = "Local Robolectric JVM suite for fast feedback.",
                notes = "Verified remote execution."
            )
        )
        dao.insertSource(
            VerificationSourceEntity(
                serviceId = forgeId,
                title = "Synthetic Runner Quota & Minutes Doc",
                url = "https://docs.gradle.org",
                sourceType = SourceType.PRICING_PAGE,
                lastVerifiedDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                excerptNote = "Synthetic runner specification confirming 4-vCPU Linux containers and 2,000 free min/mo."
            )
        )
        dao.insertSnapshot(
            CapabilitySnapshotEntity(
                serviceId = forgeId,
                snapshotLabel = "2026-Q3 Runner Spec",
                capturedAtEpochMs = oldSnapshotDate,
                planName = "Sample Builder Free + Metered",
                entitlementStatus = EntitlementStatus.FREE_TIER,
                monthlyCostUsd = 0.0,
                capabilitiesJson = buildSnapshotCapabilitiesJson(
                    listOf(
                        SnapshotCapabilityEntry(
                            name = "Remote Android & Kotlin Gradle Build",
                            type = CapabilityType.REMOTE_BUILD,
                            quota = "Synthetic 1,500 Linux runner min/mo",
                            usageModel = UsageModel.FREE,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "Max 3h job timeout; 10GB disk cap."
                        )
                    )
                ),
                verificationSourceUrl = "https://docs.gradle.org",
                notes = "Prior Q3 runner limits before free quota increase."
            )
        )
        dao.insertSnapshot(
            CapabilitySnapshotEntity(
                serviceId = forgeId,
                snapshotLabel = "2026-Q4 Runner Spec",
                capturedAtEpochMs = recentDate,
                planName = "Sample Builder Free + Metered",
                entitlementStatus = EntitlementStatus.FREE_TIER,
                monthlyCostUsd = 0.0,
                capabilitiesJson = buildSnapshotCapabilitiesJson(
                    listOf(
                        SnapshotCapabilityEntry(
                            name = "Remote Android & Kotlin Gradle Build",
                            type = CapabilityType.REMOTE_BUILD,
                            quota = "Synthetic 2,000 Linux runner min/mo included",
                            usageModel = UsageModel.FREE,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "Max 6h job timeout; 14GB runner disk cap."
                        ),
                        SnapshotCapabilityEntry(
                            name = "Automated Headless Test Matrix",
                            type = CapabilityType.REMOTE_TEST,
                            quota = "Synthetic shared 2,000 min pool; parallel shards metered",
                            usageModel = UsageModel.METERED,
                            confidence = ConfidenceLevel.VERIFIED_HIGH,
                            restriction = "No physical hardware sensor access."
                        )
                    )
                ),
                verificationSourceUrl = "https://docs.gradle.org",
                notes = "Expanded free minutes and added headless test matrix capability."
            )
        )

        // 4. AetherRender Grid [Sample] — Remote GPU Rendering, DELIBERATELY STALE SOURCE (68 days old) + INFERRED_UNVERIFIED plan!
        val aetherId = dao.insertService(
            ServiceEntity(
                name = "AetherRender Grid [Sample]",
                vendor = "Synthetic Compute Corp",
                category = ServiceCategory.RENDER_MEDIA,
                summary = "Synthetic cloud GPU farm for 3D scene rendering, video transcoding, and batch compute. Demonstrates stale-source warnings and unverified inferred entitlement protection.",
                officialDocsUrl = "https://developer.android.com/games",
                pricingUrl = "https://developer.android.com/games",
                isCloudHosted = true,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = false,
                supportsRemoteTest = false,
                supportsRemoteRender = true,
                supportsRemoteCompute = true,
                localHardwareRequirement = HardwareTier.NONE,
                storageRequirementGb = 2.0,
                privacyTier = PrivacyTier.STANDARD_CLOUD,
                isSyntheticSample = true,
                createdAtEpochMs = staleDate,
                updatedAtEpochMs = staleDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = aetherId,
                planName = "Sample Cloud Credits (Inferred)",
                entitlementStatus = EntitlementStatus.INFERRED_UNVERIFIED,
                billingType = BillingType.METERED,
                monthlyFixedCostUsd = null,
                meteredUnitCostDesc = "Unverified estimate ~$1.20/GPU-hr (stale note)",
                isUserVerifiedEntitlement = false,
                notes = "INFERRED from old team bundle email — MUST NOT be treated as verified active access."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = aetherId,
                capabilityName = "Remote GPU Frame Rendering",
                capabilityType = CapabilityType.REMOTE_RENDER,
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                quotaSummary = "Synthetic pay-per-GPU-minute; spot preemption risk",
                usageModel = UsageModel.METERED,
                incrementalCostEstimateUsd = 3.60,
                costUnitLabel = "per 3h render batch (unverified)",
                verificationSourceUrl = "https://developer.android.com/games",
                verificationDateEpochMs = staleDate,
                confidence = ConfidenceLevel.INFERRED_LOW,
                isVerified = false,
                restrictionSummary = "Source is 68 days stale; spot nodes may abort mid-render.",
                alternativesNote = "Verify current billing console before scheduling production renders.",
                notes = "Intentionally stale synthetic item to exercise stale-source alerts."
            )
        )
        dao.insertSource(
            VerificationSourceEntity(
                serviceId = aetherId,
                title = "Synthetic GPU Pricing Archive (Stale)",
                url = "https://developer.android.com/games",
                sourceType = SourceType.PRICING_PAGE,
                lastVerifiedDateEpochMs = staleDate,
                confidence = ConfidenceLevel.INFERRED_LOW,
                excerptNote = "Archived synthetic note over 60 days old — requires fresh verification."
            )
        )

        // 5. EdgeBox Local Runtime [Sample] — Air-gapped Local Execution (High Privacy, High Local HW)
        val edgeId = dao.insertService(
            ServiceEntity(
                name = "EdgeBox Local Runtime [Sample]",
                vendor = "Open Synthetic Stack",
                category = ServiceCategory.CLOUD_COMPUTE,
                summary = "Synthetic local workstation container + quantized local model runner. Full offline air-gapped privacy, $0 incremental spend, requires local GPU & 24GB disk.",
                officialDocsUrl = "https://kotlinlang.org/docs/home.html",
                pricingUrl = "https://kotlinlang.org/docs/home.html",
                isCloudHosted = false,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = false,
                supportsRemoteTest = false,
                supportsRemoteRender = false,
                supportsRemoteCompute = false,
                localHardwareRequirement = HardwareTier.HIGH_GPU,
                storageRequirementGb = 24.0,
                privacyTier = PrivacyTier.LOCAL_ONLY,
                isSyntheticSample = true,
                createdAtEpochMs = oldSnapshotDate,
                updatedAtEpochMs = recentDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = edgeId,
                planName = "Open Local License",
                entitlementStatus = EntitlementStatus.FREE_TIER,
                billingType = BillingType.FREE,
                monthlyFixedCostUsd = 0.0,
                meteredUnitCostDesc = "$0 API cost (local electricity/hardware only)",
                isUserVerifiedEntitlement = true,
                notes = "Verified local installation on dev workstation."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = edgeId,
                capabilityName = "Air-Gapped Local Inference & Scripting",
                capabilityType = CapabilityType.MODEL_INFERENCE,
                executionEnvironment = ExecutionEnvironment.LOCAL_DEVICE,
                quotaSummary = "Unlimited local runs (hardware bound)",
                usageModel = UsageModel.FREE,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "local hardware",
                verificationSourceUrl = "https://kotlinlang.org/docs/home.html",
                verificationDateEpochMs = recentDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "Not cloud-hosted; requires 16GB+ VRAM and 24GB SSD for weights.",
                alternativesNote = "Use NovaLLM Studio when mobile/thin-client cloud execution is needed.",
                notes = "Ideal for sensitive offline datasets."
            )
        )

        // 6. PulseVector Cloud DB [Sample] — Storage & Vector Index
        val pulseId = dao.insertService(
            ServiceEntity(
                name = "PulseVector Store [Sample]",
                vendor = "Synthetic Data Systems",
                category = ServiceCategory.STORAGE_DB,
                summary = "Synthetic managed vector + document store with free developer tier and predictable metered reads.",
                officialDocsUrl = "https://developer.android.com/training/data-storage/room",
                pricingUrl = "https://developer.android.com/training/data-storage/room",
                isCloudHosted = true,
                supportsModelInferenceOnly = false,
                supportsRemoteBuild = false,
                supportsRemoteTest = false,
                supportsRemoteRender = false,
                supportsRemoteCompute = false,
                localHardwareRequirement = HardwareTier.NONE,
                storageRequirementGb = 0.1,
                privacyTier = PrivacyTier.ENTERPRISE_SOC2,
                isSyntheticSample = true,
                createdAtEpochMs = oldSnapshotDate,
                updatedAtEpochMs = mediumDate
            )
        )
        dao.insertPlan(
            SubscriptionPlanEntity(
                serviceId = pulseId,
                planName = "Sample Starter Free",
                entitlementStatus = EntitlementStatus.FREE_TIER,
                billingType = BillingType.FREE,
                monthlyFixedCostUsd = 0.0,
                meteredUnitCostDesc = "Synthetic 500MB free; metered above 1M reads/mo",
                isUserVerifiedEntitlement = true,
                notes = "Verified active free project."
            )
        )
        dao.insertCapability(
            CapabilityEntity(
                serviceId = pulseId,
                capabilityName = "Hybrid Keyword + Vector Retrieval",
                capabilityType = CapabilityType.VECTOR_STORAGE,
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                quotaSummary = "Synthetic 500MB storage / 1M queries/mo free",
                usageModel = UsageModel.FREE,
                incrementalCostEstimateUsd = 0.0,
                costUnitLabel = "within free tier",
                verificationSourceUrl = "https://developer.android.com/training/data-storage/room",
                verificationDateEpochMs = mediumDate,
                confidence = ConfidenceLevel.VERIFIED_HIGH,
                isVerified = true,
                restrictionSummary = "Pauses inactive free instances after 14 days.",
                alternativesNote = "Local SQLite/Room for strict offline-only records.",
                notes = "Synthetic sample entry."
            )
        )

        // Experiments
        dao.insertExperiment(
            ExperimentEntity(
                serviceId = forgeId,
                title = "Cold Android APK Build + Unit Tests",
                taskContext = "Verify actual remote build time and cache hit rate on ForgeRunner Linux container",
                executionEnvironment = ExecutionEnvironment.REMOTE_CONTAINER,
                hypothesis = "Gradle build + Robolectric unit tests complete under 4 minutes within free quota.",
                observedResult = "Cold run finished in 3m 42s (222s); warm cache run finished in 1m 18s. Consumed 4 free runner minutes, $0.00 incremental cost.",
                outcome = ExperimentOutcome.PASSED,
                measuredCostUsd = 0.0,
                measuredLatencyMs = 222_000L,
                recordedAtEpochMs = recentDate
            )
        )
        dao.insertExperiment(
            ExperimentEntity(
                serviceId = novaId,
                title = "Architecture Review vs. Compilation Boundary Check",
                taskContext = "Confirm whether NovaLLM Studio can execute Gradle verification or only inspect source",
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                hypothesis = "NovaLLM can synthesize patch plans but cannot run remote JVM tests.",
                observedResult = "Confirmed inference-only: accurately flagged 2 Room DAO bugs in 8.4s, but cannot execute compiler or Robolectric suite. Requires ForgeRunner pairing.",
                outcome = ExperimentOutcome.PARTIAL,
                measuredCostUsd = 0.0,
                measuredLatencyMs = 8_400L,
                recordedAtEpochMs = mediumDate
            )
        )
        dao.insertExperiment(
            ExperimentEntity(
                serviceId = aetherId,
                title = "Spot GPU 4K Frame Batch Dry-Run",
                taskContext = "Test remote GPU render stability on spot tier",
                executionEnvironment = ExecutionEnvironment.CLOUD_MANAGED,
                hypothesis = "Batch finishes before preemption.",
                observedResult = "Job was preempted at 64% completion; entitlement credits could not be confirmed from current billing panel.",
                outcome = ExperimentOutcome.BLOCKED_RESTRICTION,
                measuredCostUsd = 1.45,
                measuredLatencyMs = 1_900_000L,
                recordedAtEpochMs = staleDate
            )
        )

        // Discoveries
        dao.insertDiscovery(
            DiscoveryEntity(
                title = "Self-Hosted ARM Build Cache Node [Sample Lead]",
                category = ServiceCategory.BUILD_CI,
                summary = "Potential low-cost ARM runner to offload overflow CI minutes if ForgeRunner exceeds 2,000 min/mo.",
                sourceUrl = "https://docs.gradle.org",
                confidence = ConfidenceLevel.INFERRED_LOW,
                status = DiscoveryStatus.NEW,
                potentialAlternativeTo = "ForgeRunner Cloud CI [Sample]",
                discoveredAtEpochMs = recentDate
            )
        )
        dao.insertDiscovery(
            DiscoveryEntity(
                title = "Zero-Retention Audio Transcription Endpoint [Sample Lead]",
                category = ServiceCategory.AI_MODEL,
                summary = "Candidate metered endpoint for meeting notes with strict zero-retention policy.",
                sourceUrl = "https://firebase.google.com/docs/ai-logic",
                confidence = ConfidenceLevel.PROBABLE_MEDIUM,
                status = DiscoveryStatus.TRIAGED,
                potentialAlternativeTo = "NovaLLM Studio [Sample]",
                discoveredAtEpochMs = mediumDate
            )
        )

        // Favorite Tool Combinations
        dao.insertCombo(
            FavoriteComboEntity(
                name = "Zero-Overflow Android Ship Stack",
                taskProfile = "Design, code synthesis, remote Gradle build, and automated Robolectric tests from thin client",
                serviceIdsCsv = "$novaId,$forgeId",
                rationale = "NovaLLM handles high-context reasoning (included in verified seat) while ForgeRunner handles actual remote build & test execution ($0 within free 2,000 min tier).",
                estimatedMonthlyCostUsd = 20.0,
                savedAtEpochMs = recentDate
            )
        )
        dao.insertCombo(
            FavoriteComboEntity(
                name = "Air-Gapped Private Research Stack",
                taskProfile = "Zero-cloud confidential document synthesis and local prototyping",
                serviceIdsCsv = "$edgeId",
                rationale = "100% local execution with $0 incremental API spend and zero external data egress.",
                estimatedMonthlyCostUsd = 0.0,
                savedAtEpochMs = mediumDate
            )
        )

        // Saved Offline Assessment (ready for offline reading on first launch!)
        dao.insertAssessment(
            SavedAssessmentEntity(
                title = "Remote Android Build + AI Audit Stack [Sample]",
                taskQuery = "Need automated remote Android Gradle compilation + unit tests from a thin mobile client under $5 incremental cost.",
                requirementsSummary = "Required: Remote Build, Remote Test | Cloud: Yes | Local HW: None | Privacy: Standard+ | Verified Only: Yes",
                topRecommendationName = "ForgeRunner Cloud CI [Sample]",
                deterministicSummaryMarkdown = """
                    # Offline Assessment — Remote Android Build + Test
                    
                    1. **ForgeRunner Cloud CI [Sample]** — Score: **94/100** (VERIFIED FREE TIER, $0.00 incremental)
                       - Verified remote container build & test execution.
                       - Zero local hardware dependency; SOC2 privacy tier.
                    2. **NovaLLM Studio [Sample]** — Disqualified for Build/Test (**Inference Only**)
                       - Useful companion for code review, NOT a remote compiler or test runner.
                    3. **AetherRender Grid [Sample]** — Flagged (**Stale 68d + Inferred Entitlement**)
                       - Do not rely on unverified credits or outdated pricing notes.
                """.trimIndent(),
                optionalGeminiAnalysis = null,
                createdAtEpochMs = recentDate,
                isOfflinePinned = true
            )
        )
    }
}
