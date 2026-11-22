package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FavoriteComboEntity
import com.example.data.local.SavedAssessmentEntity
import com.example.domain.FreshnessPolicy
import com.example.ui.DeltaSpotlight
import com.example.ui.ScoutUiState
import com.example.ui.components.DeltaKindBadge
import com.example.ui.components.KpiTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalScoutColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: ScoutUiState,
    onOpenDelta: (Long?) -> Unit,
    onOpenMatrixStaleOnly: () -> Unit,
    onOpenMatrixAll: () -> Unit,
    onOpenServiceDetail: (Long) -> Unit,
    onOpenAssessment: (SavedAssessmentEntity) -> Unit,
    onOpenScout: () -> Unit,
    onAddComboClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Synthetic dataset transparency notice
        if (state.hasSyntheticSamples) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = scout.verifiedContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, scout.verified.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = scout.verified,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SYNTHETIC SAMPLE INVENTORY ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = scout.verified
                            )
                            Text(
                                text = "Sample costs & quotas are synthetic placeholders. Add your verified tools or update entries anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Telemetry KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Verified Spend",
                        value = "$${"%.0f".format(state.verifiedMonthlyFixedSpendUsd)}/mo",
                        accentColor = scout.verified,
                        sublabel = "${state.inventory.bundles.size} services tracked",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenMatrixAll)
                            .testTag("kpi_spend")
                    )
                    KpiTile(
                        label = "Capabilities",
                        value = "${state.verifiedCapabilityCount}",
                        accentColor = scout.freeCost,
                        sublabel = "${state.inventory.bundles.sumOf { it.capabilities.size }} total logged",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenMatrixAll)
                            .testTag("kpi_capabilities")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Stale Alerts",
                        value = "${state.staleItemCount}",
                        accentColor = if (state.staleItemCount > 0) scout.staleWarning else scout.freeCost,
                        sublabel = if (state.staleItemCount > 0) ">30d unverified" else "All fresh",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenMatrixStaleOnly)
                            .testTag("kpi_stale")
                    )
                    KpiTile(
                        label = "Inferred Plans",
                        value = "${state.inferredEntitlementCount}",
                        accentColor = if (state.inferredEntitlementCount > 0) scout.alert else scout.freeCost,
                        sublabel = if (state.inferredEntitlementCount > 0) "Never treated active" else "All confirmed",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenMatrixAll)
                            .testTag("kpi_inferred")
                    )
                }
            }
        }

        // Prominent Capability Delta Spotlight
        item {
            SectionHeader(
                title = "Capability Delta Spotlight",
                subtitle = "Historical snapshot changes across your tracked stack",
                trailing = {
                    TextButton(
                        onClick = { onOpenDelta(null) },
                        modifier = Modifier.testTag("open_delta_button")
                    ) {
                        Text("Inspect Deltas")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }

        if (state.deltaSpotlights.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, scout.hairlineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = scout.verified)
                        Text(
                            text = "Capture at least two snapshots on a service to track quota and capability deltas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.deltaSpotlights, key = { "delta_${it.serviceId}" }) { spotlight ->
                DeltaSpotlightCard(
                    spotlight = spotlight,
                    onClick = { onOpenDelta(spotlight.serviceId) }
                )
            }
        }

        // Redundancy & Subscription Overlap Radar
        item {
            SectionHeader(
                title = "Redundancy Radar",
                subtitle = "Overlapping verified paid subscriptions with potential consolidation savings"
            )
        }

        if (state.redundancyFindings.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, scout.hairlineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Savings, contentDescription = null, tint = scout.freeCost)
                        Text(
                            text = "No overlapping paid subscriptions detected in verified inventory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.redundancyFindings, key = { "red_${it.overlappingCapability.name}" }) { finding ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("redundancy_card_${finding.overlappingCapability.name}"),
                    shape = MaterialTheme.shapes.medium,
                    color = scout.alertContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, scout.alert.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = scout.alert,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${finding.overlappingCapability.label} Overlap",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TelemetryBadge(
                                text = "SAVE ~$${"%.0f".format(finding.potentialSavingsUsd)}/MO",
                                containerColor = scout.alertContainer,
                                contentColor = scout.alert
                            )
                        }
                        Text(
                            text = finding.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            finding.overlappingBundles.forEach { bundle ->
                                TelemetryBadge(
                                    text = "${bundle.service.name} ($${"%.0f".format(bundle.verifiedMonthlyFixedCostUsd)}/mo)",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable { onOpenServiceDetail(bundle.service.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorite Tool Combinations
        item {
            SectionHeader(
                title = "Favorite Tool Combinations",
                subtitle = "Verified multi-tool pairings separating inference from compute",
                trailing = {
                    TextButton(
                        onClick = onAddComboClick,
                        modifier = Modifier.testTag("add_combo_button")
                    ) {
                        Text("+ Stack")
                    }
                }
            )
        }

        items(state.inventory.favoriteCombos, key = { "combo_${it.id}" }) { combo ->
            FavoriteComboCard(combo = combo, state = state, onOpenService = onOpenServiceDetail)
        }

        // Saved Offline Assessments
        item {
            SectionHeader(
                title = "Offline Assessment Library",
                subtitle = "Saved rankings and optional Gemini briefs readable with zero network",
                trailing = {
                    FilledTonalButton(
                        onClick = onOpenScout,
                        modifier = Modifier.testTag("new_comparison_button")
                    ) {
                        Text("Compare")
                    }
                }
            )
        }

        items(state.inventory.savedAssessments, key = { "assess_${it.id}" }) { assessment ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAssessment(assessment) }
                    .testTag("saved_assessment_${assessment.id}"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, scout.hairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OfflinePin,
                                contentDescription = null,
                                tint = scout.verified,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = assessment.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = FreshnessPolicy.formatDate(assessment.createdAtEpochMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Top pick: ${assessment.topRecommendationName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scout.verified
                    )
                    Text(
                        text = assessment.requirementsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeltaSpotlightCard(
    spotlight: DeltaSpotlight,
    onClick: () -> Unit
) {
    val scout = LocalScoutColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("delta_spotlight_${spotlight.serviceId}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, scout.staleWarning.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spotlight.serviceName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${spotlight.diff.baselineLabel} → ${spotlight.diff.targetLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (spotlight.diff.addedCount > 0) {
                        TelemetryBadge(
                            text = "+${spotlight.diff.addedCount}",
                            containerColor = scout.freeCostContainer,
                            contentColor = scout.freeCost
                        )
                    }
                    if (spotlight.diff.changedCount > 0) {
                        TelemetryBadge(
                            text = "~${spotlight.diff.changedCount}",
                            containerColor = scout.staleWarningContainer,
                            contentColor = scout.staleWarning
                        )
                    }
                    if (spotlight.diff.removedCount > 0) {
                        TelemetryBadge(
                            text = "-${spotlight.diff.removedCount}",
                            containerColor = scout.alertContainer,
                            contentColor = scout.alert
                        )
                    }
                }
            }

            spotlight.diff.capabilityDeltas.take(2).forEach { delta ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeltaKindBadge(delta.kind)
                    Text(
                        text = "${delta.capabilityName} — ${delta.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteComboCard(
    combo: FavoriteComboEntity,
    state: ScoutUiState,
    onOpenService: (Long) -> Unit
) {
    val scout = LocalScoutColors.current
    val ids = combo.serviceIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    val matchingBundles = state.inventory.bundles.filter { it.service.id in ids }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("combo_card_${combo.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, scout.hairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = scout.staleWarning, modifier = Modifier.size(16.dp))
                    Text(
                        text = combo.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                combo.estimatedMonthlyCostUsd?.let { cost ->
                    TelemetryBadge(
                        text = if (cost == 0.0) "$0/mo" else "$${"%.0f".format(cost)}/mo",
                        containerColor = if (cost == 0.0) scout.freeCostContainer else scout.verifiedContainer,
                        contentColor = if (cost == 0.0) scout.freeCost else scout.verified
                    )
                }
            }
            Text(
                text = combo.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                matchingBundles.forEach { b ->
                    TelemetryBadge(
                        text = b.service.name,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = Icons.Filled.Layers,
                        modifier = Modifier.clickable { onOpenService(b.service.id) }
                    )
                }
            }
        }
    }
}
