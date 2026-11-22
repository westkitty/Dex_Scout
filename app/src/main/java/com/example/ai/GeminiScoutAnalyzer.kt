package com.example.ai

import android.util.Log
import com.example.domain.FreshnessPolicy
import com.example.domain.RankedCandidate
import com.example.domain.TaskSpecification
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiScoutAnalyzer"

sealed interface GeminiAnalysisState {
    data object Idle : GeminiAnalysisState
    data object Loading : GeminiAnalysisState
    data class Streaming(val text: String) : GeminiAnalysisState
    data class Success(val text: String) : GeminiAnalysisState
    data class Error(val message: String) : GeminiAnalysisState
}

/**
 * Optional Gemini analysis layer using Firebase AI Logic (`Firebase.ai`).
 * Strictly optional: inventory, filtering, snapshot diffs, and rankings run 100% offline.
 */
class GeminiScoutAnalyzer {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are an analytical assistant inside DEX//SCOUT, a personal capability and cost intelligence tool.
                    STRICT RULES:
                    1. NEVER invent live vendor prices, quotas, entitlements, or subscription access.
                    2. Use ONLY the verified and recorded evidence provided in the prompt context.
                    3. NEVER treat INFERRED_UNVERIFIED entitlements as confirmed active access; flag them as needing user verification.
                    4. Explicitly distinguish pure MODEL_INFERENCE tools from actual REMOTE_BUILD, REMOTE_TEST, REMOTE_RENDER, or CLOUD_COMPUTE execution environments.
                    5. Highlight stale sources (>30 days old) and suggest concrete verification steps or experiments.
                    Keep output crisp, structured in short Markdown bullets, and concise.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamTaskAnalysis(
        spec: TaskSpecification,
        rankedCandidates: List<RankedCandidate>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Flow<GeminiAnalysisState> = flow {
        emit(GeminiAnalysisState.Loading)
        val prompt = buildGroundedPrompt(spec, rankedCandidates, nowEpochMs)
        val accumulated = StringBuilder()

        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(GeminiAnalysisState.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(GeminiAnalysisState.Success(accumulated.toString()))
            } else {
                emit(GeminiAnalysisState.Error("Gemini returned an empty analysis."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini analysis timed out", e)
            emit(GeminiAnalysisState.Error("Optional Gemini analysis timed out after 60 seconds. Your deterministic offline ranking remains valid."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini analysis failed", e)
            emit(
                GeminiAnalysisState.Error(
                    e.localizedMessage ?: "Optional Gemini analysis unavailable (offline or App Check pending)."
                )
            )
        }
    }

    private fun buildGroundedPrompt(
        spec: TaskSpecification,
        rankedCandidates: List<RankedCandidate>,
        nowEpochMs: Long
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TASK SPECIFICATION:")
        sb.appendLine("- Title: ${spec.taskTitle.ifBlank { "Custom Capability Evaluation" }}")
        sb.appendLine("- Description: ${spec.taskDescription.ifBlank { "Evaluate best tool combination from recorded inventory." }}")
        sb.appendLine("- Required Capabilities: ${spec.requiredCapabilities.joinToString { it.label }}")
        sb.appendLine("- Max Incremental Budget: ${spec.maxIncrementalCostUsd?.let { "$$it" } ?: "Unspecified"}")
        sb.appendLine("- Require Cloud Execution: ${spec.requireCloudExecution}")
        sb.appendLine("- Max Local Hardware: ${spec.maxLocalHardware.label}")
        sb.appendLine("- Max Local Storage: ${spec.maxStorageGb} GB")
        sb.appendLine("- Min Privacy Tier: ${spec.minPrivacyTier.label}")
        sb.appendLine()
        sb.appendLine("RECORDED LOCAL INVENTORY (DETERMINISTIC RANKING):")
        rankedCandidates.take(6).forEachIndexed { idx, candidate ->
            val s = candidate.bundle.service
            val plan = candidate.bundle.primaryPlan
            sb.appendLine("${idx + 1}. ${s.name} — Deterministic Score: ${candidate.totalScore}/100 | Disqualified: ${candidate.isDisqualified}")
            sb.appendLine("   - Category: ${s.category.label} | InferenceOnly: ${s.supportsModelInferenceOnly} | RemoteBuild: ${s.supportsRemoteBuild} | RemoteTest: ${s.supportsRemoteTest} | RemoteRender: ${s.supportsRemoteRender}")
            sb.appendLine("   - Plan: ${plan?.planName ?: "None"} | Entitlement: ${plan?.entitlementStatus?.label ?: "Not Subscribed"} | VerifiedFixedCost: ${plan?.monthlyFixedCostUsd ?: "Unknown"}")
            sb.appendLine("   - Est Incremental: ${candidate.estimatedIncrementalCostUsd ?: "Unknown"} | Env: ${candidate.recommendedEnvironment.label} | Privacy: ${s.privacyTier.label}")
            if (candidate.disqualifierReasons.isNotEmpty()) {
                sb.appendLine("   - Disqualifiers: ${candidate.disqualifierReasons.joinToString("; ")}")
            }
            if (candidate.warnings.isNotEmpty()) {
                sb.appendLine("   - Warnings: ${candidate.warnings.joinToString("; ")}")
            }
            candidate.bundle.capabilities.forEach { cap ->
                sb.appendLine(
                    "   - Cap: ${cap.capabilityName} [${cap.capabilityType.shortLabel}, ${cap.usageModel.label}, ${cap.confidence.label}, verified ${FreshnessPolicy.formatDate(cap.verificationDateEpochMs)} (${FreshnessPolicy.daysSince(cap.verificationDateEpochMs, nowEpochMs)}d ago)] — Quota: ${cap.quotaSummary} | Restrictions: ${cap.restrictionSummary}"
                )
            }
        }
        sb.appendLine()
        sb.appendLine("Provide: (1) Recommended execution architecture or two-tool pairing from the recorded inventory, (2) Cost & entitlement cautions (strictly no invented prices), (3) Verification actions for any stale or inferred entries.")
        return sb.toString()
    }
}
