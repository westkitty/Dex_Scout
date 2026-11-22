package com.example.domain

import com.example.data.repository.InventorySnapshot
import org.json.JSONArray
import org.json.JSONObject

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    MARKDOWN("Markdown (.md)", "md", "text/markdown"),
    CSV("CSV Matrix (.csv)", "csv", "text/csv"),
    JSON("Structured JSON (.json)", "json", "application/json")
}

object ExportEngine {

    fun export(
        snapshot: InventorySnapshot,
        format: ExportFormat,
        nowEpochMs: Long = System.currentTimeMillis()
    ): String {
        return when (format) {
            ExportFormat.MARKDOWN -> toMarkdown(snapshot, nowEpochMs)
            ExportFormat.CSV -> toCsv(snapshot, nowEpochMs)
            ExportFormat.JSON -> toJson(snapshot, nowEpochMs)
        }
    }

    fun toMarkdown(snapshot: InventorySnapshot, nowEpochMs: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.appendLine("# DEX//SCOUT Intelligence Export")
        sb.appendLine("Generated: ${FreshnessPolicy.formatDate(nowEpochMs)}")
        sb.appendLine()

        val redundancies = RedundancyAnalyzer.analyze(snapshot.bundles)
        sb.appendLine("## Summary")
        sb.appendLine("- Services tracked: ${snapshot.bundles.size}")
        sb.appendLine("- Capabilities logged: ${snapshot.bundles.sumOf { it.capabilities.size }}")
        sb.appendLine("- Experiments recorded: ${snapshot.experiments.size}")
        sb.appendLine("- Redundancy alerts: ${redundancies.size}")
        sb.appendLine()

        sb.appendLine("## Capability Matrix")
        sb.appendLine("| Service | Plan & Entitlement | Capability | Type | Usage | Est. Cost | Confidence | Verified Date | Stale? | Source |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|")
        snapshot.bundles.forEach { bundle ->
            val plan = bundle.primaryPlan
            val planText = if (plan == null) "None" else "${plan.planName} (${plan.entitlementStatus.label})"
            bundle.capabilities.forEach { cap ->
                val stale = if (FreshnessPolicy.isStale(cap.verificationDateEpochMs, nowEpochMs)) "STALE" else "Fresh"
                val costStr = cap.incrementalCostEstimateUsd?.let { "$${"%.2f".format(it)} ${cap.costUnitLabel}" } ?: "Unverified"
                sb.appendLine(
                    "| ${escapeMd(bundle.service.name)} | ${escapeMd(planText)} | ${escapeMd(cap.capabilityName)} | ${cap.capabilityType.shortLabel} | ${cap.usageModel.label} | ${escapeMd(costStr)} | ${cap.confidence.label} | ${FreshnessPolicy.formatDate(cap.verificationDateEpochMs)} | $stale | ${cap.verificationSourceUrl} |"
                )
            }
        }
        sb.appendLine()

        if (redundancies.isNotEmpty()) {
            sb.appendLine("## Potential Redundant Subscriptions")
            redundancies.forEach { r ->
                sb.appendLine("- **${r.overlappingCapability.label}**: ${r.rationale} Potential verified savings: $${"%.2f".format(r.potentialSavingsUsd)}/mo.")
            }
            sb.appendLine()
        }

        if (snapshot.experiments.isNotEmpty()) {
            sb.appendLine("## Experiments & Observed Results")
            snapshot.experiments.forEach { exp ->
                sb.appendLine("- **${exp.title}** [${exp.outcome.label}] (${exp.executionEnvironment.label}): ${exp.observedResult}")
            }
            sb.appendLine()
        }

        if (snapshot.savedAssessments.isNotEmpty()) {
            sb.appendLine("## Saved Assessments")
            snapshot.savedAssessments.forEach { a ->
                sb.appendLine("### ${a.title}")
                sb.appendLine("Top Pick: ${a.topRecommendationName}")
                sb.appendLine(a.deterministicSummaryMarkdown)
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    fun toCsv(snapshot: InventorySnapshot, nowEpochMs: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.appendLine(
            "service_name,vendor,category,plan_name,entitlement_status,monthly_fixed_usd,capability_name,capability_type,execution_env,usage_model,incremental_cost_usd,quota_summary,confidence,is_verified,verified_date,is_stale,restriction,source_url"
        )
        snapshot.bundles.forEach { bundle ->
            val s = bundle.service
            val p = bundle.primaryPlan
            bundle.capabilities.forEach { cap ->
                val stale = FreshnessPolicy.isStale(cap.verificationDateEpochMs, nowEpochMs)
                val cols = listOf(
                    s.name,
                    s.vendor,
                    s.category.name,
                    p?.planName ?: "",
                    p?.entitlementStatus?.name ?: "NOT_SUBSCRIBED",
                    p?.monthlyFixedCostUsd?.toString() ?: "",
                    cap.capabilityName,
                    cap.capabilityType.name,
                    cap.executionEnvironment.name,
                    cap.usageModel.name,
                    cap.incrementalCostEstimateUsd?.toString() ?: "",
                    cap.quotaSummary,
                    cap.confidence.name,
                    cap.isVerified.toString(),
                    FreshnessPolicy.formatDate(cap.verificationDateEpochMs),
                    stale.toString(),
                    cap.restrictionSummary,
                    cap.verificationSourceUrl
                )
                sb.appendLine(cols.joinToString(",") { escapeCsv(it) })
            }
        }
        return sb.toString()
    }

    fun toJson(snapshot: InventorySnapshot, nowEpochMs: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("schema", "dex-scout-export-v1")
        root.put("generatedAt", FreshnessPolicy.formatDate(nowEpochMs))

        val servicesArray = JSONArray()
        snapshot.bundles.forEach { bundle ->
            val s = bundle.service
            val serviceObj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("vendor", s.vendor)
                put("category", s.category.name)
                put("isCloudHosted", s.isCloudHosted)
                put("supportsModelInferenceOnly", s.supportsModelInferenceOnly)
                put("supportsRemoteBuild", s.supportsRemoteBuild)
                put("supportsRemoteTest", s.supportsRemoteTest)
                put("supportsRemoteRender", s.supportsRemoteRender)
                put("localHardwareRequirement", s.localHardwareRequirement.name)
                put("storageRequirementGb", s.storageRequirementGb)
                put("privacyTier", s.privacyTier.name)
                put("officialDocsUrl", s.officialDocsUrl)
                put("isSyntheticSample", s.isSyntheticSample)
            }

            val plansArray = JSONArray()
            bundle.plans.forEach { p ->
                plansArray.put(
                    JSONObject().apply {
                        put("planName", p.planName)
                        put("entitlementStatus", p.entitlementStatus.name)
                        put("isUserVerifiedEntitlement", p.isUserVerifiedEntitlement)
                        put("billingType", p.billingType.name)
                        if (p.monthlyFixedCostUsd != null) put("monthlyFixedCostUsd", p.monthlyFixedCostUsd) else put("monthlyFixedCostUsd", JSONObject.NULL)
                        put("meteredUnitCostDesc", p.meteredUnitCostDesc)
                    }
                )
            }
            serviceObj.put("plans", plansArray)

            val capsArray = JSONArray()
            bundle.capabilities.forEach { cap ->
                capsArray.put(
                    JSONObject().apply {
                        put("capabilityName", cap.capabilityName)
                        put("capabilityType", cap.capabilityType.name)
                        put("executionEnvironment", cap.executionEnvironment.name)
                        put("usageModel", cap.usageModel.name)
                        put("quotaSummary", cap.quotaSummary)
                        if (cap.incrementalCostEstimateUsd != null) put("incrementalCostEstimateUsd", cap.incrementalCostEstimateUsd) else put("incrementalCostEstimateUsd", JSONObject.NULL)
                        put("confidence", cap.confidence.name)
                        put("isVerified", cap.isVerified)
                        put("verifiedDate", FreshnessPolicy.formatDate(cap.verificationDateEpochMs))
                        put("isStale", FreshnessPolicy.isStale(cap.verificationDateEpochMs, nowEpochMs))
                        put("restrictionSummary", cap.restrictionSummary)
                        put("verificationSourceUrl", cap.verificationSourceUrl)
                    }
                )
            }
            serviceObj.put("capabilities", capsArray)
            servicesArray.put(serviceObj)
        }
        root.put("services", servicesArray)

        val expArray = JSONArray()
        snapshot.experiments.forEach { exp ->
            expArray.put(
                JSONObject().apply {
                    put("title", exp.title)
                    put("outcome", exp.outcome.name)
                    put("executionEnvironment", exp.executionEnvironment.name)
                    put("observedResult", exp.observedResult)
                    if (exp.measuredCostUsd != null) put("measuredCostUsd", exp.measuredCostUsd)
                }
            )
        }
        root.put("experiments", expArray)

        val combosArray = JSONArray()
        snapshot.favoriteCombos.forEach { c ->
            combosArray.put(
                JSONObject().apply {
                    put("name", c.name)
                    put("taskProfile", c.taskProfile)
                    put("serviceIdsCsv", c.serviceIdsCsv)
                    put("rationale", c.rationale)
                }
            )
        }
        root.put("favoriteCombos", combosArray)

        return root.toString(2)
    }

    private fun escapeCsv(raw: String): String {
        val needsQuotes = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        return if (needsQuotes) "\"${raw.replace("\"", "\"\"")}\"" else raw
    }

    private fun escapeMd(raw: String): String {
        return raw.replace("|", "\\|").replace("\n", " ")
    }
}
