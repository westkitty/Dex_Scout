package com.example.domain

import com.example.data.repository.ServiceBundle
import com.example.domain.model.CapabilityType
import com.example.domain.model.EntitlementStatus

data class RedundancyFinding(
    val overlappingCapability: CapabilityType,
    val overlappingBundles: List<ServiceBundle>,
    val combinedVerifiedMonthlyCostUsd: Double,
    val potentialSavingsUsd: Double,
    val rationale: String
)

/**
 * Identifies potentially redundant paid subscriptions overlapping on the same verified capability.
 * Uses only user-entered verified fixed subscription costs; never invents prices.
 */
object RedundancyAnalyzer {

    fun analyze(bundles: List<ServiceBundle>): List<RedundancyFinding> {
        val paidActiveBundles = bundles.filter { bundle ->
            bundle.plans.any { plan ->
                plan.entitlementStatus == EntitlementStatus.VERIFIED_ACTIVE &&
                    plan.isUserVerifiedEntitlement &&
                    (plan.monthlyFixedCostUsd ?: 0.0) > 0.0
            }
        }

        val byCapability = mutableMapOf<CapabilityType, MutableList<ServiceBundle>>()
        paidActiveBundles.forEach { bundle ->
            val capTypes = bundle.capabilities.map { it.capabilityType }.toSet()
            capTypes.forEach { capType ->
                byCapability.getOrPut(capType) { mutableListOf() }.add(bundle)
            }
        }

        return byCapability.entries
            .filter { (_, list) -> list.size >= 2 }
            .map { (capType, list) ->
                val sorted = list.sortedByDescending { it.verifiedMonthlyFixedCostUsd }
                val totalCost = sorted.sumOf { it.verifiedMonthlyFixedCostUsd }
                // Potential savings if consolidating down to the highest-coverage single paid tool
                val savings = sorted.drop(1).sumOf { it.verifiedMonthlyFixedCostUsd }
                RedundancyFinding(
                    overlappingCapability = capType,
                    overlappingBundles = sorted,
                    combinedVerifiedMonthlyCostUsd = totalCost,
                    potentialSavingsUsd = savings,
                    rationale = "${sorted.size} verified paid subscriptions overlap on ${capType.label}: ${sorted.joinToString { it.service.name }}."
                )
            }
            .sortedByDescending { it.potentialSavingsUsd }
    }
}
