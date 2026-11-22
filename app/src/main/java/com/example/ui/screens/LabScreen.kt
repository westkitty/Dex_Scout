package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DiscoveryEntity
import com.example.data.local.ExperimentEntity
import com.example.data.local.SavedAssessmentEntity
import com.example.domain.ExportFormat
import com.example.domain.FreshnessPolicy
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DiscoveryStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.ExperimentOutcome
import com.example.domain.model.ServiceCategory
import com.example.ui.ScoutUiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalScoutColors
import com.example.ui.util.AndroidActions

enum class LabSubTab(val label: String) {
    EXPERIMENTS("Experiments"),
    DISCOVERIES("Discoveries"),
    OFFLINE("Saved"),
    EXPORT("Export")
}

@Composable
fun LabScreen(
    state: ScoutUiState,
    initialSubTab: LabSubTab = LabSubTab.EXPERIMENTS,
    onAddExperiment: (ExperimentEntity) -> Unit,
    onDeleteExperiment: (Long) -> Unit,
    onAddDiscovery: (DiscoveryEntity) -> Unit,
    onUpdateDiscoveryStatus: (DiscoveryEntity, DiscoveryStatus) -> Unit,
    onOpenAssessment: (SavedAssessmentEntity) -> Unit,
    onDeleteAssessment: (Long) -> Unit,
    onBuildExport: (ExportFormat) -> String,
    onResetSyntheticSample: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scout = LocalScoutColors.current
    var selectedTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var showAddExperiment by remember { mutableStateOf(false) }
    var showAddDiscovery by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            LabSubTab.entries.forEachIndexed { idx, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    shape = SegmentedButtonDefaults.itemShape(idx, LabSubTab.entries.size),
                    modifier = Modifier.testTag("lab_tab_${tab.name}")
                ) {
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        when (selectedTab) {
            LabSubTab.EXPERIMENTS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("experiments_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Observed Experiments",
                            subtitle = "Record real build, quota, latency, and cost test runs",
                            trailing = {
                                Button(
                                    onClick = { showAddExperiment = true },
                                    modifier = Modifier.testTag("add_experiment_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Log")
                                }
                            }
                        )
                    }
                    items(state.inventory.experiments, key = { "exp_${it.id}" }) { exp ->
                        val serviceName = state.inventory.bundles
                            .firstOrNull { it.service.id == exp.serviceId }?.service?.name ?: "Cross-Tool"
                        val (bg, fg) = when (exp.outcome) {
                            ExperimentOutcome.PASSED -> scout.freeCostContainer to scout.freeCost
                            ExperimentOutcome.PARTIAL -> scout.staleWarningContainer to scout.staleWarning
                            ExperimentOutcome.BLOCKED_RESTRICTION, ExperimentOutcome.FAILED_QUOTA -> scout.alertContainer to scout.alert
                            ExperimentOutcome.INCONCLUSIVE -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = exp.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TelemetryBadge(exp.outcome.label.uppercase(), bg, fg)
                                }
                                Text(
                                    text = "$serviceName · ${exp.executionEnvironment.label} · ${FreshnessPolicy.formatDate(exp.recordedAtEpochMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(exp.observedResult, style = MaterialTheme.typography.bodySmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        exp.measuredCostUsd?.let {
                                            TelemetryBadge(
                                                "COST $${"%.2f".format(it)}",
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        exp.measuredLatencyMs?.let {
                                            TelemetryBadge(
                                                "${"%.1f".format(it / 1000.0)}s",
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteExperiment(exp.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete experiment")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LabSubTab.DISCOVERIES -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("discoveries_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Discovery Radar",
                            subtitle = "Track candidate alternatives before promoting to verified inventory",
                            trailing = {
                                Button(
                                    onClick = { showAddDiscovery = true },
                                    modifier = Modifier.testTag("add_discovery_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Lead")
                                }
                            }
                        )
                    }
                    items(state.inventory.discoveries, key = { "disc_${it.id}" }) { disc ->
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
                                    Text(
                                        text = disc.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TelemetryBadge(
                                        disc.status.label.uppercase(),
                                        scout.verifiedContainer,
                                        scout.verified
                                    )
                                }
                                Text(disc.summary, style = MaterialTheme.typography.bodySmall)
                                if (disc.potentialAlternativeTo.isNotBlank()) {
                                    Text(
                                        text = "Alternative to: ${disc.potentialAlternativeTo}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scout.staleWarning
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        val ok = AndroidActions.openExternalUrl(context, disc.sourceUrl)
                                        if (!ok) onNotify("Invalid URL.")
                                    }) {
                                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Open Link")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (disc.status != DiscoveryStatus.VERIFIED_INTO_INVENTORY) {
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdateDiscoveryStatus(disc, DiscoveryStatus.VERIFIED_INTO_INVENTORY)
                                                }
                                            ) {
                                                Text("Mark Verified")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LabSubTab.OFFLINE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("offline_library_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Saved Offline Assessments",
                            subtitle = "Stored locally in Room SQLite for airplane / zero-network reading"
                        )
                    }
                    items(state.inventory.savedAssessments, key = { "lib_${it.id}" }) { assessment ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAssessment(assessment) },
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
                                        Icon(Icons.Filled.OfflinePin, contentDescription = null, tint = scout.verified, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = assessment.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteAssessment(assessment.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete saved assessment")
                                    }
                                }
                                Text("Top pick: ${assessment.topRecommendationName}", style = MaterialTheme.typography.labelMedium, color = scout.verified)
                                Text(assessment.requirementsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            LabSubTab.EXPORT -> {
                val previewPayload = remember(state.inventory, selectedExportFormat) {
                    onBuildExport(selectedExportFormat)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("export_screen_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Export Intelligence",
                            subtitle = "Deterministic local export to Markdown, CSV, or JSON"
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportFormat.entries.forEach { fmt ->
                                FilterChip(
                                    selected = selectedExportFormat == fmt,
                                    onClick = { selectedExportFormat = fmt },
                                    label = { Text(fmt.label) },
                                    modifier = Modifier.testTag("export_format_${fmt.name}")
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val shared = AndroidActions.shareExportFile(context, selectedExportFormat, previewPayload)
                                    if (!shared) onNotify("Could not launch share sheet.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_export_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Share File")
                            }
                            OutlinedButton(
                                onClick = {
                                    AndroidActions.copyToClipboard(context, "DEX//SCOUT Export", previewPayload)
                                    onNotify("Copied ${selectedExportFormat.label} to clipboard.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_export_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Copy Text")
                            }
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_preview_box"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, scout.hairlineBorder)
                        ) {
                            Text(
                                text = previewPayload.take(3_500),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Synthetic Sample Inventory", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Restore the synthetic benchmark dataset for testing rankings, stale warnings, and deltas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = onResetSyntheticSample,
                                    modifier = Modifier.testTag("reset_sample_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Reset")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAddExperiment) {
        AddExperimentDialog(
            bundles = state.inventory.bundles,
            onDismiss = { showAddExperiment = false },
            onSave = {
                onAddExperiment(it)
                showAddExperiment = false
            }
        )
    }

    if (showAddDiscovery) {
        AddDiscoveryDialog(
            onDismiss = { showAddDiscovery = false },
            onSave = {
                onAddDiscovery(it)
                showAddDiscovery = false
            }
        )
    }
}

@Composable
private fun AddExperimentDialog(
    bundles: List<com.example.data.repository.ServiceBundle>,
    onDismiss: () -> Unit,
    onSave: (ExperimentEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var taskContext by remember { mutableStateOf("") }
    var observed by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("0.0") }
    var outcome by remember { mutableStateOf(ExperimentOutcome.PASSED) }
    var env by remember { mutableStateOf(ExecutionEnvironment.REMOTE_CONTAINER) }
    var serviceId by remember { mutableStateOf(bundles.firstOrNull()?.service?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Experiment Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Experiment Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskContext,
                    onValueChange = { taskContext = it },
                    label = { Text("Task Context") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observed,
                    onValueChange = { observed = it },
                    label = { Text("Observed Result") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Measured Cost (USD, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExperimentOutcome.entries.forEach { o ->
                        FilterChip(
                            selected = outcome == o,
                            onClick = { outcome = o },
                            label = { Text(o.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && observed.isNotBlank()) {
                        onSave(
                            ExperimentEntity(
                                serviceId = serviceId,
                                title = title.trim(),
                                taskContext = taskContext.trim().ifBlank { "Manual benchmark" },
                                executionEnvironment = env,
                                hypothesis = "User benchmark",
                                observedResult = observed.trim(),
                                outcome = outcome,
                                measuredCostUsd = costText.toDoubleOrNull(),
                                measuredLatencyMs = null
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddDiscoveryDialog(
    onDismiss: () -> Unit,
    onSave: (DiscoveryEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var altTo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Tool Discovery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = altTo, onValueChange = { altTo = it }, label = { Text("Alternative To (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Official URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            DiscoveryEntity(
                                title = title.trim(),
                                category = ServiceCategory.DEV_PLATFORM,
                                summary = summary.trim().ifBlank { "Candidate lead for evaluation." },
                                sourceUrl = url.trim(),
                                confidence = ConfidenceLevel.INFERRED_LOW,
                                status = DiscoveryStatus.NEW,
                                potentialAlternativeTo = altTo.trim()
                            )
                        )
                    }
                }
            ) {
                Text("Save Lead")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
