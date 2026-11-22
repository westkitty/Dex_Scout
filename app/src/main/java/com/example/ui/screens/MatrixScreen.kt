package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.ServiceBundle
import com.example.domain.CapabilityMatrixRow
import com.example.domain.FreshnessPolicy
import com.example.domain.MatrixFilterState
import com.example.domain.model.CapabilityType
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.MatrixSortOption
import com.example.domain.model.UsageModel
import com.example.ui.ScoutUiState
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.EntitlementBadge
import com.example.ui.components.ExecutionModeBadge
import com.example.ui.components.TelemetryBadge
import com.example.ui.components.UsageCostBadge
import com.example.ui.theme.LocalScoutColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatrixScreen(
    state: ScoutUiState,
    onUpdateFilter: ((MatrixFilterState) -> MatrixFilterState) -> Unit,
    onResetFilter: () -> Unit,
    onOpenServiceDetail: (Long) -> Unit,
    onAddServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    var showServicesInventory by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val filter = state.matrixFilter

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mode Switch + Sort + Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = !showServicesInventory,
                            onClick = { showServicesInventory = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("toggle_matrix_view")
                        ) {
                            Text("Matrix (${state.matrixRows.size})", style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = showServicesInventory,
                            onClick = { showServicesInventory = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("toggle_inventory_view")
                        ) {
                            Text("Services (${state.inventory.bundles.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    androidx.compose.foundation.layout.Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("matrix_sort_button")
                        ) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Sort matrix")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            MatrixSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (filter.sortOption == opt) "✓ ${opt.label}" else opt.label,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        onUpdateFilter { it.copy(sortOption = opt) }
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matrix_search_input"),
                    placeholder = { Text("Filter capability, vendor, quota, restriction...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filter.query.isNotEmpty()) {
                            IconButton(onClick = { onUpdateFilter { it.copy(query = "") } }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )

                // Powerful Filter Rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    FilterChip(
                        selected = filter.verifiedOnly,
                        onClick = { onUpdateFilter { it.copy(verifiedOnly = !it.verifiedOnly) } },
                        label = { Text("Verified High") },
                        modifier = Modifier.testTag("filter_verified_only")
                    )
                    FilterChip(
                        selected = filter.remoteComputeOnly,
                        onClick = { onUpdateFilter { it.copy(remoteComputeOnly = !it.remoteComputeOnly) } },
                        label = { Text("Remote Exec/Build") },
                        modifier = Modifier.testTag("filter_remote_compute")
                    )
                    FilterChip(
                        selected = filter.staleOnly,
                        onClick = { onUpdateFilter { it.copy(staleOnly = !it.staleOnly, hideStale = false) } },
                        label = { Text("Stale >30d") },
                        modifier = Modifier.testTag("filter_stale_only")
                    )
                    FilterChip(
                        selected = filter.verifiedEntitlementOnly,
                        onClick = { onUpdateFilter { it.copy(verifiedEntitlementOnly = !it.verifiedEntitlementOnly) } },
                        label = { Text("Verified Access") },
                        modifier = Modifier.testTag("filter_verified_entitlement")
                    )
                    UsageModel.entries.forEach { usage ->
                        FilterChip(
                            selected = filter.usageModel == usage,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(usageModel = if (it.usageModel == usage) null else usage)
                                }
                            },
                            label = { Text(usage.label) },
                            modifier = Modifier.testTag("filter_usage_${usage.name}")
                        )
                    }
                    CapabilityType.entries.take(5).forEach { capType ->
                        FilterChip(
                            selected = filter.capabilityType == capType,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(capabilityType = if (it.capabilityType == capType) null else capType)
                                }
                            },
                            label = { Text(capType.shortLabel) },
                            modifier = Modifier.testTag("filter_cap_${capType.name}")
                        )
                    }
                    if (filter != MatrixFilterState()) {
                        TextButton(onClick = onResetFilter, modifier = Modifier.testTag("filter_reset")) {
                            Text("Reset")
                        }
                    }
                }
            }

            if (!showServicesInventory) {
                if (state.matrixRows.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No capabilities match current filters.", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onResetFilter) { Text("Clear all filters") }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("capability_matrix_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.matrixRows, key = { "row_${it.capability.id}" }) { row ->
                            MatrixRowCard(row = row, onClick = { onOpenServiceDetail(row.bundle.service.id) })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("service_inventory_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.inventory.bundles, key = { "svc_${it.service.id}" }) { bundle ->
                        ServiceInventoryCard(bundle = bundle, onClick = { onOpenServiceDetail(bundle.service.id) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddServiceClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_service_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add service to inventory")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatrixRowCard(
    row: CapabilityMatrixRow,
    onClick: () -> Unit
) {
    val scout = LocalScoutColors.current
    val cap = row.capability
    val svc = row.bundle.service
    val borderColor = when {
        row.isStale -> scout.staleWarning.copy(alpha = 0.55f)
        row.hasInferredEntitlement -> scout.alert.copy(alpha = 0.55f)
        else -> scout.hairlineBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("matrix_row_${cap.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cap.capabilityName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${svc.name} · ${cap.executionEnvironment.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(8.dp))
                UsageCostBadge(
                    usageModel = cap.usageModel,
                    costUsd = cap.incrementalCostEstimateUsd
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TelemetryBadge(
                    text = cap.capabilityType.shortLabel.uppercase(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                ExecutionModeBadge(
                    isInferenceOnly = svc.supportsModelInferenceOnly,
                    supportsRemoteCompute = cap.capabilityType.isRemoteComputeExecution
                )
                ConfidenceBadge(
                    confidence = cap.confidence,
                    isStale = row.isStale,
                    daysOld = row.daysOld
                )
                row.bundle.primaryPlan?.let { plan ->
                    EntitlementBadge(status = plan.entitlementStatus)
                }
            }

            Text(
                text = "Quota: ${cap.quotaSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (cap.restrictionSummary.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = scout.staleWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = cap.restrictionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceInventoryCard(
    bundle: ServiceBundle,
    onClick: () -> Unit
) {
    val scout = LocalScoutColors.current
    val s = bundle.service
    val plan = bundle.primaryPlan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("service_card_${s.id}"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${s.vendor} · ${s.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val costLabel = plan?.monthlyFixedCostUsd?.let {
                    if (it == 0.0) "$0/mo" else "$${"%.0f".format(it)}/mo"
                } ?: "Cost unverified"
                TelemetryBadge(
                    text = costLabel,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = s.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExecutionModeBadge(
                    isInferenceOnly = s.supportsModelInferenceOnly,
                    supportsRemoteCompute = s.supportsRemoteBuild || s.supportsRemoteTest || s.supportsRemoteRender || s.supportsRemoteCompute
                )
                EntitlementBadge(plan?.entitlementStatus ?: EntitlementStatus.NOT_SUBSCRIBED)
                TelemetryBadge(
                    text = "${bundle.capabilities.size} caps · ${bundle.snapshots.size} snaps",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TelemetryBadge(
                    text = "Updated ${FreshnessPolicy.formatDate(s.updatedAtEpochMs)}",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
