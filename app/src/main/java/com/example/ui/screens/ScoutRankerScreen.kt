package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiAnalysisState
import com.example.domain.RankedCandidate
import com.example.domain.TaskSpecification
import com.example.domain.model.CapabilityType
import com.example.domain.model.HardwareTier
import com.example.domain.model.PrivacyTier
import com.example.ui.ScoutUiState
import com.example.ui.components.ExecutionModeBadge
import com.example.ui.components.OptionalAiBadge
import com.example.ui.components.ScoreMeter
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalScoutColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoutRankerScreen(
    state: ScoutUiState,
    onUpdateTaskSpec: ((TaskSpecification) -> TaskSpecification) -> Unit,
    onSaveAssessmentOffline: () -> Unit,
    onRunOptionalGemini: () -> Unit,
    onClearGemini: () -> Unit,
    onOpenServiceDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    val spec = state.taskSpec
    var constraintsExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("scout_ranker_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Quick Task Presets
        item {
            SectionHeader(
                title = "Task & Execution Ranker",
                subtitle = "100% deterministic offline ranking across 8 verified factors",
                trailing = {
                    OutlinedButton(
                        onClick = onSaveAssessmentOffline,
                        modifier = Modifier.testTag("save_assessment_offline_button")
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Save Offline")
                    }
                }
            )
        }

        // Quick Task Presets
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = CapabilityType.REMOTE_BUILD in spec.requiredCapabilities,
                    onClick = {
                        onUpdateTaskSpec {
                            TaskSpecification(
                                taskTitle = "Remote Android Build & Test",
                                taskDescription = "Compile Kotlin/Compose APK and run unit tests from thin client.",
                                requiredCapabilities = setOf(CapabilityType.REMOTE_BUILD, CapabilityType.REMOTE_TEST),
                                maxIncrementalCostUsd = 5.0,
                                preferExistingSubscriptions = true,
                                requireCloudExecution = true,
                                maxLocalHardware = HardwareTier.NONE,
                                maxStorageGb = 4.0,
                                minPrivacyTier = PrivacyTier.STANDARD_CLOUD
                            )
                        }
                    },
                    label = { Text("Remote Build + Test") },
                    modifier = Modifier.testTag("preset_remote_build")
                )
                FilterChip(
                    selected = spec.minPrivacyTier == PrivacyTier.LOCAL_ONLY,
                    onClick = {
                        onUpdateTaskSpec {
                            TaskSpecification(
                                taskTitle = "Air-Gapped Private Synthesis",
                                taskDescription = "Zero-egress private document synthesis on local workstation GPU.",
                                requiredCapabilities = setOf(CapabilityType.MODEL_INFERENCE),
                                maxIncrementalCostUsd = 0.0,
                                preferExistingSubscriptions = true,
                                requireCloudExecution = false,
                                maxLocalHardware = HardwareTier.HIGH_GPU,
                                maxStorageGb = 32.0,
                                minPrivacyTier = PrivacyTier.LOCAL_ONLY
                            )
                        }
                    },
                    label = { Text("Air-Gapped Local AI") },
                    modifier = Modifier.testTag("preset_airgapped")
                )
                FilterChip(
                    selected = CapabilityType.REMOTE_RENDER in spec.requiredCapabilities,
                    onClick = {
                        onUpdateTaskSpec {
                            TaskSpecification(
                                taskTitle = "Cloud GPU Frame Render",
                                taskDescription = "Batch GPU frame rendering without local GPU hardware.",
                                requiredCapabilities = setOf(CapabilityType.REMOTE_RENDER),
                                maxIncrementalCostUsd = 5.0,
                                preferExistingSubscriptions = true,
                                requireCloudExecution = true,
                                maxLocalHardware = HardwareTier.NONE,
                                maxStorageGb = 4.0,
                                minPrivacyTier = PrivacyTier.STANDARD_CLOUD
                            )
                        }
                    },
                    label = { Text("Remote GPU Render") },
                    modifier = Modifier.testTag("preset_remote_render")
                )
            }
        }

        // Task Constraint Builder Card
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, scout.hairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { constraintsExpanded = !constraintsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TASK CONSTRAINTS & EXECUTION BOUNDARIES",
                            style = MaterialTheme.typography.labelMedium,
                            color = scout.verified
                        )
                        Icon(
                            imageVector = if (constraintsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle constraints"
                        )
                    }

                    AnimatedVisibility(visible = constraintsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = spec.taskTitle,
                                onValueChange = { v -> onUpdateTaskSpec { it.copy(taskTitle = v) } },
                                label = { Text("Task Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_title_input")
                            )

                            Text(
                                text = "Required Capabilities (Inference vs. Remote Compute):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CapabilityType.entries.forEach { capType ->
                                    val selected = capType in spec.requiredCapabilities
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            onUpdateTaskSpec { current ->
                                                val next = if (selected) current.requiredCapabilities - capType
                                                else current.requiredCapabilities + capType
                                                current.copy(requiredCapabilities = next)
                                            }
                                        },
                                        label = { Text(capType.shortLabel) },
                                        modifier = Modifier.testTag("req_cap_${capType.name}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Cloud Execution", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = spec.requireCloudExecution,
                                    onCheckedChange = { c -> onUpdateTaskSpec { it.copy(requireCloudExecution = c) } },
                                    modifier = Modifier.testTag("switch_require_cloud")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Strict Verified Capabilities Only", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = spec.verifiedOnly,
                                    onCheckedChange = { v -> onUpdateTaskSpec { it.copy(verifiedOnly = v) } },
                                    modifier = Modifier.testTag("switch_verified_only")
                                )
                            }

                            // Incremental Budget Slider
                            val budget = spec.maxIncrementalCostUsd ?: 10.0
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Max Incremental Cost", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "$${"%.2f".format(budget)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = scout.verified
                                    )
                                }
                                Slider(
                                    value = budget.toFloat().coerceIn(0f, 50f),
                                    onValueChange = { v -> onUpdateTaskSpec { it.copy(maxIncrementalCostUsd = v.toDouble()) } },
                                    valueRange = 0f..50f
                                )
                            }

                            // Local Hardware Ceiling
                            Text("Local Hardware Available:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HardwareTier.entries.forEach { hw ->
                                    FilterChip(
                                        selected = spec.maxLocalHardware == hw,
                                        onClick = { onUpdateTaskSpec { it.copy(maxLocalHardware = hw) } },
                                        label = { Text(hw.label) }
                                    )
                                }
                            }

                            // Privacy Tier
                            Text("Minimum Privacy Tier:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrivacyTier.entries.forEach { priv ->
                                    FilterChip(
                                        selected = spec.minPrivacyTier == priv,
                                        onClick = { onUpdateTaskSpec { it.copy(minPrivacyTier = priv) } },
                                        label = { Text(priv.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ranked Execution Environments
        item {
            SectionHeader(
                title = "Ranked Execution Environments",
                subtitle = "Sorted by verified capability, entitlement, incremental cost, and evidence freshness"
            )
        }

        itemsIndexed(state.rankedCandidates, key = { _, item -> "rank_${item.bundle.service.id}" }) { index, candidate ->
            RankedCandidateCard(
                rank = index + 1,
                candidate = candidate,
                onOpenServiceDetail = { onOpenServiceDetail(candidate.bundle.service.id) }
            )
        }

        // Optional Gemini Analysis Card (Firebase AI Logic — strictly optional!)
        item {
            OptionalGeminiCard(
                geminiState = state.geminiState,
                isOnline = state.isOnline,
                onRunGemini = onRunOptionalGemini,
                onClear = onClearGemini,
                onSaveOffline = onSaveAssessmentOffline
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankedCandidateCard(
    rank: Int,
    candidate: RankedCandidate,
    onOpenServiceDetail: () -> Unit
) {
    val scout = LocalScoutColors.current
    var expanded by remember { mutableStateOf(rank == 1) }
    val s = candidate.bundle.service

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ranked_candidate_${s.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            when {
                candidate.isDisqualified -> scout.alert.copy(alpha = 0.45f)
                rank == 1 -> scout.verified.copy(alpha = 0.65f)
                else -> scout.hairlineBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (candidate.isDisqualified) "BLOCKED · ${s.name}" else "#$rank · ${s.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (candidate.isDisqualified) scout.alert else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Env: ${candidate.recommendedEnvironment.label} · ${s.privacyTier.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val costText = candidate.estimatedIncrementalCostUsd?.let {
                    if (it == 0.0) "$0.00 inc." else "$${"%.2f".format(it)} inc."
                } ?: "Cost unverified"
                TelemetryBadge(
                    text = costText,
                    containerColor = if (candidate.estimatedIncrementalCostUsd == 0.0) scout.freeCostContainer else scout.staleWarningContainer,
                    contentColor = if (candidate.estimatedIncrementalCostUsd == 0.0) scout.freeCost else scout.staleWarning
                )
            }

            ScoreMeter(score = candidate.totalScore, isDisqualified = candidate.isDisqualified)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExecutionModeBadge(
                    isInferenceOnly = s.supportsModelInferenceOnly,
                    supportsRemoteCompute = s.supportsRemoteBuild || s.supportsRemoteTest || s.supportsRemoteRender || s.supportsRemoteCompute
                )
                if (candidate.hasVerifiedEntitlement) {
                    TelemetryBadge("VERIFIED ENTITLEMENT", scout.verifiedContainer, scout.verified)
                }
                if (candidate.hasInferredEntitlementWarning) {
                    TelemetryBadge("INFERRED — NOT VERIFIED", scout.alertContainer, scout.alert, icon = Icons.Filled.WarningAmber)
                }
                if (candidate.hasStaleEvidenceWarning) {
                    TelemetryBadge("STALE SOURCE", scout.staleWarningContainer, scout.staleWarning, icon = Icons.Filled.WarningAmber)
                }
            }

            candidate.disqualifierReasons.forEach { reason ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, tint = scout.alert, modifier = Modifier.size(15.dp))
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = scout.alert)
                }
            }

            candidate.warnings.forEach { warn ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = scout.staleWarning, modifier = Modifier.size(15.dp))
                    Text(warn, style = MaterialTheme.typography.bodySmall, color = scout.staleWarning)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "8-FACTOR DETERMINISTIC BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    candidate.factorBreakdown.forEach { f ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${f.label} (${f.detail})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${f.points}/${f.maxPoints}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onOpenServiceDetail) {
                            Text("Inspect Sources & Plan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionalGeminiCard(
    geminiState: GeminiAnalysisState,
    isOnline: Boolean,
    onRunGemini: () -> Unit,
    onClear: () -> Unit,
    onSaveOffline: () -> Unit
) {
    val scout = LocalScoutColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("optional_gemini_card"),
        shape = MaterialTheme.shapes.medium,
        color = scout.aiAccentContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, scout.aiAccent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OptionalAiBadge()
                Text(
                    text = if (isOnline) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) scout.freeCost else scout.staleWarning
                )
            }
            Text(
                text = "Optional synthesis grounded strictly in your local Room inventory. Never required for inventory, filtering, or rankings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (geminiState) {
                is GeminiAnalysisState.Idle -> {
                    Button(
                        onClick = onRunGemini,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scout.aiAccent,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.testTag("run_gemini_brief_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Run Optional Gemini Brief")
                    }
                }

                is GeminiAnalysisState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = scout.aiAccent)
                        Text("Synthesizing grounded comparison via Firebase AI Logic...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                is GeminiAnalysisState.Streaming -> {
                    Text(geminiState.text, style = MaterialTheme.typography.bodyMedium)
                }

                is GeminiAnalysisState.Success -> {
                    Text(geminiState.text, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSaveOffline) {
                            Text("Save Brief Offline")
                        }
                        TextButton(onClick = onClear) {
                            Text("Clear")
                        }
                    }
                }

                is GeminiAnalysisState.Error -> {
                    Text(
                        text = geminiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = scout.alert
                    )
                    OutlinedButton(onClick = onRunGemini) {
                        Text("Retry Optional Gemini")
                    }
                }
            }
        }
    }
}
