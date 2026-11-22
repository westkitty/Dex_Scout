package com.example.domain

import com.example.data.local.CapabilityEntity
import com.example.data.repository.InventorySnapshot
import com.example.data.repository.ServiceBundle
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.MatrixSortOption
import com.example.domain.model.ServiceCategory
import com.example.domain.model.UsageModel

data class MatrixFilterState(
    val query: String = "",
    val category: ServiceCategory? = null,
    val capabilityType: CapabilityType? = null,
    val usageModel: UsageModel? = null,
    val verifiedOnly: Boolean = false,
    val staleOnly: Boolean = false,
    val hideStale: Boolean = false,
    val remoteComputeOnly: Boolean = false,
    val verifiedEntitlementOnly: Boolean = false,
    val sortOption: MatrixSortOption = MatrixSortOption.CONFIDENCE_DESC
)

data class CapabilityMatrixRow(
    val bundle: ServiceBundle,
    val capability: CapabilityEntity,
    val daysOld: Long,
    val isStale: Boolean,
    val verifiedMonthlyCostUsd: Double?,
    val hasVerifiedEntitlement: Boolean,
    val hasInferredEntitlement: Boolean
)

data class GlobalSearchResult(
    val typeBadge: String,
    val title: String,
    val subtitle: String,
    val serviceId: Long? = null,
    val assessmentId: Long? = null
)

object MatrixFilterEngine {

    fun filterAndSort(
        bundles: List<ServiceBundle>,
        filter: MatrixFilterState,
        nowEpochMs: Long = System.currentTimeMillis(),
        staleThresholdDays: Long = FreshnessPolicy.DEFAULT_STALE_DAYS
    ): List<CapabilityMatrixRow> {
        val normalizedQuery = filter.query.trim().lowercase()

        val rows = bundles.flatMap { bundle ->
            val primaryPlan = bundle.primaryPlan
            val verifiedEntitlement = bundle.plans.any {
                it.entitlementStatus.isVerifiedAccess && it.isUserVerifiedEntitlement
            }
            val inferredEntitlement = !verifiedEntitlement && bundle.plans.any {
                !it.entitlementStatus.isVerifiedAccess && it.entitlementStatus.name == "INFERRED_UNVERIFIED"
            }

            bundle.capabilities.map { cap ->
                val daysOld = FreshnessPolicy.daysSince(cap.verificationDateEpochMs, nowEpochMs)
                CapabilityMatrixRow(
                    bundle = bundle,
                    capability = cap,
                    daysOld = daysOld,
                    isStale = daysOld > staleThresholdDays,
                    verifiedMonthlyCostUsd = primaryPlan?.monthlyFixedCostUsd,
                    hasVerifiedEntitlement = verifiedEntitlement,
                    hasInferredEntitlement = inferredEntitlement
                )
            }
        }

        val filtered = rows.filter { row ->
            val svc = row.bundle.service
            val cap = row.capability
            val matchesQuery = normalizedQuery.isBlank() ||
                svc.name.lowercase().contains(normalizedQuery) ||
                svc.vendor.lowercase().contains(normalizedQuery) ||
                cap.capabilityName.lowercase().contains(normalizedQuery) ||
                cap.quotaSummary.lowercase().contains(normalizedQuery) ||
                cap.restrictionSummary.lowercase().contains(normalizedQuery) ||
                cap.alternativesNote.lowercase().contains(normalizedQuery)

            val matchesCategory = filter.category == null || svc.category == filter.category
            val matchesCapType = filter.capabilityType == null || cap.capabilityType == filter.capabilityType
            val matchesUsage = filter.usageModel == null || cap.usageModel == filter.usageModel
            val matchesVerified = !filter.verifiedOnly || (cap.isVerified && cap.confidence == ConfidenceLevel.VERIFIED_HIGH)
            val matchesStaleOnly = !filter.staleOnly || row.isStale
            val matchesHideStale = !filter.hideStale || !row.isStale
            val matchesRemoteCompute = !filter.remoteComputeOnly || cap.capabilityType.isRemoteComputeExecution
            val matchesEntitlement = !filter.verifiedEntitlementOnly || row.hasVerifiedEntitlement

            matchesQuery && matchesCategory && matchesCapType && matchesUsage &&
                matchesVerified && matchesStaleOnly && matchesHideStale &&
                matchesRemoteCompute && matchesEntitlement
        }

        return when (filter.sortOption) {
            MatrixSortOption.NAME_ASC -> filtered.sortedWith(
                compareBy<CapabilityMatrixRow> { it.bundle.service.name }.thenBy { it.capability.capabilityName }
            )
            MatrixSortOption.COST_ASC -> filtered.sortedWith(
                compareBy<CapabilityMatrixRow> { it.capability.incrementalCostEstimateUsd ?: Double.MAX_VALUE }
                    .thenBy { it.verifiedMonthlyCostUsd ?: Double.MAX_VALUE }
                    .thenBy { it.bundle.service.name }
            )
            MatrixSortOption.CONFIDENCE_DESC -> filtered.sortedWith(
                compareByDescending<CapabilityMatrixRow> { it.capability.confidence.scoreWeight }
                    .thenBy { it.daysOld }
                    .thenBy { it.bundle.service.name }
            )
            MatrixSortOption.FRESHNESS_DESC -> filtered.sortedBy { it.daysOld }
            MatrixSortOption.STALEST_FIRST -> filtered.sortedByDescending { it.daysOld }
        }
    }

    fun fullTextSearch(snapshot: InventorySnapshot, query: String): List<GlobalSearchResult> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val results = mutableListOf<GlobalSearchResult>()

        snapshot.bundles.forEach { bundle ->
            val s = bundle.service
            if (s.name.lowercase().contains(q) || s.vendor.lowercase().contains(q) || s.summary.lowercase().contains(q)) {
                results += GlobalSearchResult(
                    typeBadge = "SERVICE",
                    title = s.name,
                    subtitle = "${s.category.label} · ${s.vendor}",
                    serviceId = s.id
                )
            }
            bundle.capabilities.forEach { cap ->
                if (cap.capabilityName.lowercase().contains(q) ||
                    cap.quotaSummary.lowercase().contains(q) ||
                    cap.restrictionSummary.lowercase().contains(q) ||
                    cap.alternativesNote.lowercase().contains(q)
                ) {
                    results += GlobalSearchResult(
                        typeBadge = "CAPABILITY",
                        title = "${cap.capabilityName} (${s.name})",
                        subtitle = "${cap.usageModel.label} · ${cap.quotaSummary}",
                        serviceId = s.id
                    )
                }
            }
            bundle.sources.forEach { src ->
                if (src.title.lowercase().contains(q) || src.url.lowercase().contains(q) || src.excerptNote.lowercase().contains(q)) {
                    results += GlobalSearchResult(
                        typeBadge = "SOURCE",
                        title = src.title,
                        subtitle = src.url,
                        serviceId = s.id
                    )
                }
            }
        }

        snapshot.experiments.forEach { exp ->
            if (exp.title.lowercase().contains(q) || exp.observedResult.lowercase().contains(q) || exp.taskContext.lowercase().contains(q)) {
                results += GlobalSearchResult(
                    typeBadge = "EXPERIMENT",
                    title = exp.title,
                    subtitle = "${exp.outcome.label} · ${exp.observedResult.take(80)}",
                    serviceId = exp.serviceId
                )
            }
        }

        snapshot.discoveries.forEach { disc ->
            if (disc.title.lowercase().contains(q) || disc.summary.lowercase().contains(q) || disc.potentialAlternativeTo.lowercase().contains(q)) {
                results += GlobalSearchResult(
                    typeBadge = "DISCOVERY",
                    title = disc.title,
                    subtitle = "${disc.status.label} · ${disc.summary.take(80)}"
                )
            }
        }

        snapshot.savedAssessments.forEach { a ->
            if (a.title.lowercase().contains(q) || a.taskQuery.lowercase().contains(q) || a.deterministicSummaryMarkdown.lowercase().contains(q)) {
                results += GlobalSearchResult(
                    typeBadge = "ASSESSMENT",
                    title = a.title,
                    subtitle = "Top pick: ${a.topRecommendationName}",
                    assessmentId = a.id
                )
            }
        }

        return results
    }
}
