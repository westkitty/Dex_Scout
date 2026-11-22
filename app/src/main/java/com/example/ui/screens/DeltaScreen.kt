package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CapabilitySnapshotEntity
import com.example.data.repository.ServiceBundle
import com.example.domain.FreshnessPolicy
import com.example.domain.SnapshotDiffEngine
import com.example.ui.ScoutUiState
import com.example.ui.components.DeltaKindBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalScoutColors

@Composable
fun DeltaScreen(
    state: ScoutUiState,
    initialServiceId: Long?,
    onCaptureSnapshot: (ServiceBundle, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scout = LocalScoutColors.current
    val bundles = state.inventory.bundles

    var selectedServiceId by remember(initialServiceId, bundles) {
        mutableStateOf(
            initialServiceId?.takeIf { id -> bundles.any { it.service.id == id } }
                ?: bundles.firstOrNull { it.snapshots.size >= 2 }?.service?.id
                ?: bundles.firstOrNull()?.service?.id
        )
    }

    val selectedBundle = bundles.firstOrNull { it.service.id == selectedServiceId }
    val liveSnapshot = remember(selectedBundle) {
        selectedBundle?.let { SnapshotDiffEngine.liveSnapshotForBundle(it) }
    }
    val allComparableSnapshots: List<CapabilitySnapshotEntity> = remember(selectedBundle, liveSnapshot) {
        if (selectedBundle == null || liveSnapshot == null) emptyList()
        else selectedBundle.snapshots.sortedBy { it.capturedAtEpochMs } + liveSnapshot
    }

    var baselineIndex by remember(selectedServiceId, allComparableSnapshots.size) {
        mutableStateOf((allComparableSnapshots.size - 3).coerceAtLeast(0))
    }
    var targetIndex by remember(selectedServiceId, allComparableSnapshots.size) {
        mutableStateOf((allComparableSnapshots.size - 2).coerceAtLeast(0))
    }

    LaunchedEffect(selectedServiceId, allComparableSnapshots.size) {
        if (allComparableSnapshots.size >= 3) {
            baselineIndex = allComparableSnapshots.size - 3
            targetIndex = allComparableSnapshots.size - 2
        } else if (allComparableSnapshots.size == 2) {
            baselineIndex = 0
            targetIndex = 1
        } else {
            baselineIndex = 0
            targetIndex = 0
        }
    }

    var showCaptureDialog by remember { mutableStateOf(false) }

    val diffResult = remember(allComparableSnapshots, baselineIndex, targetIndex) {
        if (allComparableSnapshots.size >= 2 &&
            baselineIndex in allComparableSnapshots.indices &&
            targetIndex in allComparableSnapshots.indices
        ) {
            SnapshotDiffEngine.diffSnapshots(
                allComparableSnapshots[baselineIndex],
                allComparableSnapshots[targetIndex]
            )
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("delta_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Capability Delta Explorer",
                subtitle = "Compare historical capability snapshots against each other or live inventory",
                trailing = {
                    if (selectedBundle != null) {
                        Button(
                            onClick = { showCaptureDialog = true },
                            modifier = Modifier.testTag("capture_snapshot_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Snapshot")
                        }
                    }
                }
            )
        }

        // Service selector rail
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bundles.forEach { b ->
                    FilterChip(
                        selected = b.service.id == selectedServiceId,
                        onClick = { selectedServiceId = b.service.id },
                        label = { Text("${b.service.name} (${b.snapshots.size})") },
                        modifier = Modifier.testTag("delta_service_chip_${b.service.id}")
                    )
                }
            }
        }

        if (selectedBundle == null) {
            item {
                Text("Add a service to begin capturing capability snapshots.", style = MaterialTheme.typography.bodyMedium)
            }
        } else if (allComparableSnapshots.size < 2) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, scout.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No historical snapshots captured yet for ${selectedBundle.service.name}.", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Tap Snapshot above to freeze the current verified capabilities, quotas, and pricing as a baseline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Baseline & Target Selectors
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
                        Text("BASELINE SNAPSHOT (FROM)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allComparableSnapshots.forEachIndexed { idx, snap ->
                                FilterChip(
                                    selected = baselineIndex == idx,
                                    onClick = { baselineIndex = idx },
                                    label = {
                                        Text("${snap.snapshotLabel} (${FreshnessPolicy.formatDate(snap.capturedAtEpochMs)})")
                                    }
                                )
                            }
                        }

                        Text("TARGET SNAPSHOT (TO)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allComparableSnapshots.forEachIndexed { idx, snap ->
                                FilterChip(
                                    selected = targetIndex == idx,
                                    onClick = { targetIndex = idx },
                                    label = {
                                        Text("${snap.snapshotLabel} (${FreshnessPolicy.formatDate(snap.capturedAtEpochMs)})")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (diffResult != null) {
                // Summary Strip
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delta_summary_card"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, scout.verified.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = scout.verified)
                                Text(
                                    text = "${diffResult.baselineLabel} → ${diffResult.targetLabel}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TelemetryBadge(
                                    text = "+${diffResult.addedCount} ADDED",
                                    containerColor = scout.freeCostContainer,
                                    contentColor = scout.freeCost
                                )
                                TelemetryBadge(
                                    text = "~${diffResult.changedCount} CHANGED",
                                    containerColor = scout.staleWarningContainer,
                                    contentColor = scout.staleWarning
                                )
                                TelemetryBadge(
                                    text = "-${diffResult.removedCount} REMOVED",
                                    containerColor = scout.alertContainer,
                                    contentColor = scout.alert
                                )
                            }
                        }
                    }
                }

                if (!diffResult.hasAnyChanges) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, scout.hairlineBorder)
                        ) {
                            Text(
                                text = "Identical state — no plan, quota, confidence, or capability differences between these two snapshots.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    // Plan-level changes
                    if (diffResult.planChanges.isNotEmpty()) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, scout.staleWarning.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("PLAN & ENTITLEMENT CHANGES", style = MaterialTheme.typography.labelMedium, color = scout.staleWarning)
                                    diffResult.planChanges.forEach { change ->
                                        FieldDiffRow(
                                            fieldName = change.fieldName,
                                            before = change.beforeValue,
                                            after = change.afterValue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Capability-level deltas
                    items(diffResult.capabilityDeltas) { delta ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
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
                                        Text(
                                            text = delta.capabilityName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = delta.capabilityType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DeltaKindBadge(delta.kind)
                                }

                                if (delta.fieldChanges.isEmpty()) {
                                    Text(
                                        text = delta.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    delta.fieldChanges.forEach { fc ->
                                        FieldDiffRow(
                                            fieldName = fc.fieldName,
                                            before = fc.beforeValue,
                                            after = fc.afterValue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showCaptureDialog && selectedBundle != null) {
        var label by remember { mutableStateOf("Snapshot ${FreshnessPolicy.formatDate(System.currentTimeMillis())}") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text("Capture Capability Snapshot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Freezes current capabilities, quotas, plan cost, and restrictions for ${selectedBundle.service.name}.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Snapshot Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Verification Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCaptureSnapshot(selectedBundle, label.trim(), notes.trim())
                        showCaptureDialog = false
                    },
                    modifier = Modifier.testTag("confirm_capture_snapshot")
                ) {
                    Text("Save Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FieldDiffRow(
    fieldName: String,
    before: String,
    after: String
) {
    val scout = LocalScoutColors.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = fieldName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = before,
                style = MaterialTheme.typography.bodySmall,
                color = scout.alert,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = after,
                style = MaterialTheme.typography.bodySmall,
                color = scout.freeCost,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
