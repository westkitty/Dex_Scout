package com.example.domain

import com.example.data.local.CapabilityEntity
import com.example.data.repository.ServiceBundle
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.HardwareTier
import com.example.domain.model.PrivacyTier
import com.example.domain.model.UsageModel
import kotlin.math.roundToInt

data class TaskSpecification(
    val taskTitle: String = "",
    val taskDescription: String = "",
    val requiredCapabilities: Set<CapabilityType> = setOf(CapabilityType.REMOTE_BUILD),
    val maxIncrementalCostUsd: Double? = 5.0,
    val preferExistingSubscriptions: Boolean = true,
    val requireCloudExecution: Boolean = true,
    val maxLocalHardware: HardwareTier = HardwareTier.NONE,
    val maxStorageGb: Double = 4.0,
    val minPrivacyTier: PrivacyTier = PrivacyTier.STANDARD_CLOUD,
    val verifiedOnly: Boolean = false,
    val staleThresholdDays: Long = FreshnessPolicy.DEFAULT_STALE_DAYS
)

data class FactorScore(
    val label: String,
    val points: Int,
    val maxPoints: Int,
    val detail: String
)

data class RankedCandidate(
    val bundle: ServiceBundle,
    val totalScore: Int,
    val recommendedEnvironment: ExecutionEnvironment,
    val matchedCapabilities: List<CapabilityEntity>,
    val missingCapabilities: Set<CapabilityType>,
    val estimatedIncrementalCostUsd: Double?,
    val hasVerifiedEntitlement: Boolean,
    val hasInferredEntitlementWarning: Boolean,
    val hasStaleEvidenceWarning: Boolean,
    val isInferenceOnlyMismatch: Boolean,
    val isDisqualified: Boolean,
    val disqualifierReasons: List<String>,
    val warnings: List<String>,
    val factorBreakdown: List<FactorScore>
)

/**
 * Deterministic, offline-first recommendation and execution environment ranking engine.
 * Ranks candidate services across all 8 required dimensions without any AI requirement.
 */
object RecommendationEngine {

    fun rank(
        bundles: List<ServiceBundle>,
        spec: TaskSpecification,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<RankedCandidate> {
        return bundles
            .map { bundle -> evaluateCandidate(bundle, spec, nowEpochMs) }
            .sortedWith(
                compareBy<RankedCandidate> { it.isDisqualified }
                    .thenByDescending { it.totalScore }
                    .thenBy { it.estimatedIncrementalCostUsd ?: Double.MAX_VALUE }
                    .thenBy { it.bundle.service.name }
            )
    }

    private fun evaluateCandidate(
        bundle: ServiceBundle,
        spec: TaskSpecification,
        nowEpochMs: Long
    ): RankedCandidate {
        val service = bundle.service
        val disqualifiers = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val factors = mutableListOf<FactorScore>()

        val required = spec.requiredCapabilities
        val needsRemoteCompute = required.any { it.isRemoteComputeExecution }

        // 1. Verified Capability Match (Max 28 pts)
        val matchingCaps = if (required.isEmpty()) {
            bundle.capabilities
        } else {
            bundle.capabilities.filter { it.capabilityType in required }
        }
        val coveredTypes = matchingCaps.map { it.capabilityType }.toSet()
        val missingTypes = required - coveredTypes

        val inferenceOnlyMismatch = needsRemoteCompute && service.supportsModelInferenceOnly &&
            !service.supportsRemoteBuild && !service.supportsRemoteTest &&
            !service.supportsRemoteRender && !service.supportsRemoteCompute

        if (inferenceOnlyMismatch) {
            disqualifiers += "Model inference only — cannot run remote build, test, render, or compute."
        }
        if (required.isNotEmpty() && missingTypes.isNotEmpty()) {
            disqualifiers += "Missing required capability: ${missingTypes.joinToString { it.shortLabel }}"
        }

        val verifiedMatchingCaps = matchingCaps.filter { it.isVerified && it.confidence != ConfidenceLevel.UNVERIFIED }
        if (spec.verifiedOnly && required.isNotEmpty() && verifiedMatchingCaps.isEmpty()) {
            disqualifiers += "No verified capability evidence matches strict verified-only mode."
        }

        val capabilityPoints = when {
            inferenceOnlyMismatch -> 0
            required.isEmpty() && verifiedMatchingCaps.isNotEmpty() -> 24
            required.isEmpty() -> 12
            else -> {
                val coverageRatio = coveredTypes.size.toDouble() / required.size.toDouble()
                val verifiedRatio = if (matchingCaps.isEmpty()) 0.0 else {
                    verifiedMatchingCaps.size.toDouble() / matchingCaps.size.toDouble()
                }
                ((coverageRatio * 20.0) + (verifiedRatio * 8.0)).roundToInt()
            }
        }
        factors += FactorScore(
            label = "Verified Capability",
            points = capabilityPoints,
            maxPoints = 28,
            detail = if (required.isEmpty()) "${verifiedMatchingCaps.size} verified" else "${coveredTypes.size}/${required.size} matched (${verifiedMatchingCaps.size} verified)"
        )

        // 2. Existing Subscription Entitlement (Max 14 pts)
        // CRITICAL: Never treat INFERRED_UNVERIFIED as verified access!
        val activeVerifiedPlan = bundle.plans.firstOrNull {
            it.entitlementStatus.isVerifiedAccess && it.isUserVerifiedEntitlement
        }
        val inferredPlan = bundle.plans.firstOrNull {
            it.entitlementStatus == EntitlementStatus.INFERRED_UNVERIFIED || !it.isUserVerifiedEntitlement
        }
        val hasVerifiedEntitlement = activeVerifiedPlan != null
        val hasInferredWarning = !hasVerifiedEntitlement && inferredPlan != null &&
            inferredPlan.entitlementStatus == EntitlementStatus.INFERRED_UNVERIFIED

        if (hasInferredWarning) {
            warnings += "Entitlement is INFERRED/UNVERIFIED — never counted as active access."
        }

        val entitlementPoints = when {
            !spec.preferExistingSubscriptions -> if (hasVerifiedEntitlement) 10 else 6
            hasVerifiedEntitlement -> 14
            hasInferredWarning -> 0
            else -> 2
        }
        factors += FactorScore(
            label = "Subscription Entitlement",
            points = entitlementPoints,
            maxPoints = 14,
            detail = when {
                activeVerifiedPlan != null -> activeVerifiedPlan.entitlementStatus.label
                hasInferredWarning -> "Inferred (0 credit)"
                else -> "Not subscribed"
            }
        )

        // 3. Incremental Cost (Max 16 pts)
        val incrementalCosts = matchingCaps.mapNotNull { it.incrementalCostEstimateUsd }
        val estimatedIncremental: Double? = when {
            incrementalCosts.isNotEmpty() -> incrementalCosts.sum()
            matchingCaps.any { it.usageModel == UsageModel.FREE || it.usageModel == UsageModel.INCLUDED } -> 0.0
            else -> null
        }

        val costPoints = when {
            estimatedIncremental == null -> {
                warnings += "Incremental cost is unverified/unknown — verify pricing source."
                5
            }
            estimatedIncremental == 0.0 -> 16
            spec.maxIncrementalCostUsd != null && estimatedIncremental <= spec.maxIncrementalCostUsd -> {
                val ratio = 1.0 - (estimatedIncremental / spec.maxIncrementalCostUsd.coerceAtLeast(0.01))
                (8 + (ratio * 7.0)).roundToInt().coerceIn(7, 15)
            }
            spec.maxIncrementalCostUsd != null && estimatedIncremental > spec.maxIncrementalCostUsd -> {
                warnings += "Estimated incremental cost ($${"%.2f".format(estimatedIncremental)}) exceeds budget ($${"%.2f".format(spec.maxIncrementalCostUsd)})."
                2
            }
            else -> 10
        }
        factors += FactorScore(
            label = "Incremental Cost",
            points = costPoints,
            maxPoints = 16,
            detail = estimatedIncremental?.let { if (it == 0.0) "$0.00 (Free/Included)" else "$${"%.2f".format(it)} est." } ?: "Unverified cost"
        )

        // 4. Cloud Execution Availability (Max 10 pts)
        val recommendedEnv = matchingCaps.firstOrNull()?.executionEnvironment
            ?: if (service.isCloudHosted) ExecutionEnvironment.CLOUD_MANAGED else ExecutionEnvironment.LOCAL_DEVICE
        val cloudPoints = if (spec.requireCloudExecution) {
            if (service.isCloudHosted && recommendedEnv.isCloud) {
                10
            } else {
                disqualifiers += "Requires cloud execution, but service is local-only."
                0
            }
        } else {
            if (service.isCloudHosted) 8 else 10
        }
        factors += FactorScore(
            label = "Cloud Execution",
            points = cloudPoints,
            maxPoints = 10,
            detail = recommendedEnv.label
        )

        // 5. Local Hardware Fit (Max 8 pts)
        val hwFits = service.localHardwareRequirement.rank <= spec.maxLocalHardware.rank
        val hwPoints = if (hwFits) {
            8
        } else {
            disqualifiers += "Requires ${service.localHardwareRequirement.label} (exceeds ${spec.maxLocalHardware.label})."
            0
        }
        factors += FactorScore(
            label = "Local Hardware",
            points = hwPoints,
            maxPoints = 8,
            detail = service.localHardwareRequirement.label
        )

        // 6. Storage Requirement Fit (Max 6 pts)
        val storageFits = service.storageRequirementGb <= spec.maxStorageGb
        val storagePoints = if (storageFits) {
            6
        } else {
            warnings += "Needs ${service.storageRequirementGb}GB storage (limit ${spec.maxStorageGb}GB)."
            1
        }
        factors += FactorScore(
            label = "Storage Fit",
            points = storagePoints,
            maxPoints = 6,
            detail = "${service.storageRequirementGb} GB"
        )

        // 7. Privacy Constraints (Max 8 pts)
        val privacyFits = service.privacyTier.rank >= spec.minPrivacyTier.rank
        val privacyPoints = if (privacyFits) {
            8
        } else {
            disqualifiers += "Privacy tier (${service.privacyTier.label}) is below required ${spec.minPrivacyTier.label}."
            0
        }
        factors += FactorScore(
            label = "Privacy Tier",
            points = privacyPoints,
            maxPoints = 8,
            detail = service.privacyTier.label
        )

        // 8. Evidence Confidence & Freshness (Max 10 pts)
        val capsToCheck = matchingCaps.ifEmpty { bundle.capabilities }
        val avgConfidence = if (capsToCheck.isEmpty()) 0.1 else {
            capsToCheck.map { it.confidence.scoreWeight }.average()
        }
        val staleCaps = capsToCheck.filter {
            FreshnessPolicy.isStale(it.verificationDateEpochMs, nowEpochMs, spec.staleThresholdDays)
        }
        val staleSources = bundle.sources.filter {
            FreshnessPolicy.isStale(it.lastVerifiedDateEpochMs, nowEpochMs, spec.staleThresholdDays)
        }
        val hasStaleWarning = staleCaps.isNotEmpty() || staleSources.isNotEmpty()
        if (hasStaleWarning) {
            val oldestDays = (staleCaps.map { FreshnessPolicy.daysSince(it.verificationDateEpochMs, nowEpochMs) } +
                staleSources.map { FreshnessPolicy.daysSince(it.lastVerifiedDateEpochMs, nowEpochMs) })
                .maxOrNull() ?: spec.staleThresholdDays
            warnings += "Stale verification evidence (${oldestDays}d old > ${spec.staleThresholdDays}d limit)."
        }

        val freshnessPenalty = if (hasStaleWarning) 4 else 0
        val confidencePoints = ((avgConfidence * 10.0).roundToInt() - freshnessPenalty).coerceIn(0, 10)
        factors += FactorScore(
            label = "Evidence Confidence",
            points = confidencePoints,
            maxPoints = 10,
            detail = if (hasStaleWarning) "Stale evidence penalized" else "${(avgConfidence * 100).roundToInt()}% confidence"
        )

        val rawScore = factors.sumOf { it.points }.coerceIn(0, 100)
        val finalScore = if (disqualifiers.isNotEmpty()) (rawScore * 0.45).roundToInt() else rawScore

        return RankedCandidate(
            bundle = bundle,
            totalScore = finalScore,
            recommendedEnvironment = recommendedEnv,
            matchedCapabilities = matchingCaps,
            missingCapabilities = missingTypes,
            estimatedIncrementalCostUsd = estimatedIncremental,
            hasVerifiedEntitlement = hasVerifiedEntitlement,
            hasInferredEntitlementWarning = hasInferredWarning,
            hasStaleEvidenceWarning = hasStaleWarning,
            isInferenceOnlyMismatch = inferenceOnlyMismatch,
            isDisqualified = disqualifiers.isNotEmpty(),
            disqualifierReasons = disqualifiers,
            warnings = warnings,
            factorBreakdown = factors
        )
    }
}
