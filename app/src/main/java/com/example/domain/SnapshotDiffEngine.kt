package com.example.domain

import com.example.data.local.CapabilitySnapshotEntity
import com.example.data.repository.ServiceBundle
import com.example.data.seed.SyntheticSeedData
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.UsageModel
import org.json.JSONArray

enum class DeltaKind { ADDED, REMOVED, CHANGED }

data class SnapshotFieldChange(
    val fieldName: String,
    val beforeValue: String,
    val afterValue: String
)

data class CapabilityDeltaItem(
    val kind: DeltaKind,
    val capabilityName: String,
    val capabilityType: String,
    val summary: String,
    val fieldChanges: List<SnapshotFieldChange> = emptyList()
)

data class SnapshotDiffResult(
    val baselineLabel: String,
    val targetLabel: String,
    val planChanges: List<SnapshotFieldChange>,
    val capabilityDeltas: List<CapabilityDeltaItem>
) {
    val addedCount: Int get() = capabilityDeltas.count { it.kind == DeltaKind.ADDED }
    val removedCount: Int get() = capabilityDeltas.count { it.kind == DeltaKind.REMOVED }
    val changedCount: Int get() = capabilityDeltas.count { it.kind == DeltaKind.CHANGED } + planChanges.size
    val hasAnyChanges: Boolean get() = planChanges.isNotEmpty() || capabilityDeltas.isNotEmpty()
}

private data class ParsedCapItem(
    val name: String,
    val type: String,
    val quota: String,
    val usageModel: String,
    val confidence: String,
    val restriction: String
)

object SnapshotDiffEngine {

    fun diffSnapshots(
        baseline: CapabilitySnapshotEntity,
        target: CapabilitySnapshotEntity
    ): SnapshotDiffResult {
        val planChanges = mutableListOf<SnapshotFieldChange>()
        if (baseline.planName != target.planName) {
            planChanges += SnapshotFieldChange("Plan", baseline.planName, target.planName)
        }
        if (baseline.entitlementStatus != target.entitlementStatus) {
            planChanges += SnapshotFieldChange(
                "Entitlement",
                baseline.entitlementStatus.label,
                target.entitlementStatus.label
            )
        }
        if (baseline.monthlyCostUsd != target.monthlyCostUsd) {
            val beforeCost = baseline.monthlyCostUsd?.let { "$${"%.2f".format(it)}/mo" } ?: "Unverified"
            val afterCost = target.monthlyCostUsd?.let { "$${"%.2f".format(it)}/mo" } ?: "Unverified"
            planChanges += SnapshotFieldChange("Monthly Fixed Cost", beforeCost, afterCost)
        }

        val baseCaps = parseCapabilities(baseline.capabilitiesJson).associateBy { it.name.lowercase() }
        val targetCaps = parseCapabilities(target.capabilitiesJson).associateBy { it.name.lowercase() }

        val deltas = mutableListOf<CapabilityDeltaItem>()

        // Added capabilities
        targetCaps.forEach { (key, afterCap) ->
            if (key !in baseCaps) {
                deltas += CapabilityDeltaItem(
                    kind = DeltaKind.ADDED,
                    capabilityName = afterCap.name,
                    capabilityType = prettyEnum(afterCap.type),
                    summary = "${afterCap.usageModel} · ${afterCap.quota}"
                )
            }
        }

        // Removed capabilities
        baseCaps.forEach { (key, beforeCap) ->
            if (key !in targetCaps) {
                deltas += CapabilityDeltaItem(
                    kind = DeltaKind.REMOVED,
                    capabilityName = beforeCap.name,
                    capabilityType = prettyEnum(beforeCap.type),
                    summary = "Previously ${beforeCap.usageModel} · ${beforeCap.quota}"
                )
            }
        }

        // Changed capabilities
        targetCaps.forEach { (key, afterCap) ->
            val beforeCap = baseCaps[key] ?: return@forEach
            val changes = mutableListOf<SnapshotFieldChange>()
            if (beforeCap.quota != afterCap.quota) {
                changes += SnapshotFieldChange("Quota", beforeCap.quota, afterCap.quota)
            }
            if (beforeCap.usageModel != afterCap.usageModel) {
                changes += SnapshotFieldChange("Usage Model", beforeCap.usageModel, afterCap.usageModel)
            }
            if (beforeCap.confidence != afterCap.confidence) {
                changes += SnapshotFieldChange("Confidence", prettyEnum(beforeCap.confidence), prettyEnum(afterCap.confidence))
            }
            if (beforeCap.restriction != afterCap.restriction) {
                changes += SnapshotFieldChange(
                    "Restriction",
                    beforeCap.restriction.ifBlank { "None" },
                    afterCap.restriction.ifBlank { "None" }
                )
            }
            if (changes.isNotEmpty()) {
                deltas += CapabilityDeltaItem(
                    kind = DeltaKind.CHANGED,
                    capabilityName = afterCap.name,
                    capabilityType = prettyEnum(afterCap.type),
                    summary = "${changes.size} field(s) changed",
                    fieldChanges = changes
                )
            }
        }

        return SnapshotDiffResult(
            baselineLabel = baseline.snapshotLabel,
            targetLabel = target.snapshotLabel,
            planChanges = planChanges,
            capabilityDeltas = deltas.sortedBy { it.kind.ordinal }
        )
    }

    fun liveSnapshotForBundle(bundle: ServiceBundle, nowEpochMs: Long = System.currentTimeMillis()): CapabilitySnapshotEntity {
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
        return CapabilitySnapshotEntity(
            id = -1L,
            serviceId = bundle.service.id,
            snapshotLabel = "Current Live Inventory",
            capturedAtEpochMs = nowEpochMs,
            planName = plan?.planName ?: "No Plan Recorded",
            entitlementStatus = plan?.entitlementStatus ?: EntitlementStatus.NOT_SUBSCRIBED,
            monthlyCostUsd = plan?.monthlyFixedCostUsd,
            capabilitiesJson = SyntheticSeedData.buildSnapshotCapabilitiesJson(entries),
            verificationSourceUrl = bundle.service.officialDocsUrl,
            notes = "Live Room state"
        )
    }

    private fun parseCapabilities(json: String): List<ParsedCapItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                ParsedCapItem(
                    name = obj.optString("name", "Unnamed"),
                    type = obj.optString("type", CapabilityType.MODEL_INFERENCE.name),
                    quota = obj.optString("quota", ""),
                    usageModel = obj.optString("usageModel", UsageModel.FREE.name),
                    confidence = obj.optString("confidence", ConfidenceLevel.UNVERIFIED.name),
                    restriction = obj.optString("restriction", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun prettyEnum(raw: String): String {
        return raw.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}
