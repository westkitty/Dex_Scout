package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.example.data.local.CapabilityEntity
import com.example.data.local.VerificationSourceEntity
import com.example.data.repository.ServiceBundle
import com.example.domain.FreshnessPolicy
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.SourceType
import com.example.domain.model.UsageModel
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.EntitlementBadge
import com.example.ui.components.ExecutionModeBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.components.UsageCostBadge
import com.example.ui.theme.LocalScoutColors
import com.example.ui.util.AndroidActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreen(
    bundle: ServiceBundle,
    refreshingSourceId: Long?,
    isOnline: Boolean,
    onBack: () -> Unit,
    onDeleteService: (Long) -> Unit,
    onMarkCapabilityVerifiedNow: (CapabilityEntity) -> Unit,
    onAddCapability: (CapabilityEntity) -> Unit,
    onAddSource: (VerificationSourceEntity) -> Unit,
    onRefreshPublicSource: (VerificationSourceEntity) -> Unit,
    onCaptureSnapshot: (ServiceBundle, String, String) -> Unit,
    onOpenDeltaForService: (Long) -> Unit,
    onNotify: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scout = LocalScoutColors.current
    val s = bundle.service

    var showAddCapDialog by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onCaptureSnapshot(
                                bundle,
                                "Snap ${FreshnessPolicy.formatDate(System.currentTimeMillis())}",
                                "Captured from service detail"
                            )
                        },
                        modifier = Modifier.testTag("detail_capture_snapshot")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Capture snapshot")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete service")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("service_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity & Execution Boundary Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, scout.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${s.vendor} · ${s.category.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = scout.verified
                        )
                        Text(s.summary, style = MaterialTheme.typography.bodyMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExecutionModeBadge(
                                isInferenceOnly = s.supportsModelInferenceOnly,
                                supportsRemoteCompute = s.supportsRemoteBuild || s.supportsRemoteTest || s.supportsRemoteRender || s.supportsRemoteCompute
                            )
                            TelemetryBadge(
                                text = "PRIVACY: ${s.privacyTier.label.uppercase()}",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            TelemetryBadge(
                                text = "HW: ${s.localHardwareRequirement.label}",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TelemetryBadge(
                                text = "DISK: ${s.storageRequirementGb} GB",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (!AndroidActions.openExternalUrl(context, s.officialDocsUrl)) {
                                        onNotify("Invalid official docs URL.")
                                    }
                                },
                                modifier = Modifier.testTag("open_official_docs_button")
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Official Docs")
                            }
                            if (bundle.snapshots.isNotEmpty()) {
                                OutlinedButton(onClick = { onOpenDeltaForService(s.id) }) {
                                    Text("View ${bundle.snapshots.size} Snapshots")
                                }
                            }
                        }
                    }
                }
            }

            // Subscription Plans
            item {
                SectionHeader(
                    title = "Subscription Plans & Entitlements",
                    subtitle = "Inferred entitlements are never counted as verified active access"
                )
            }
            items(bundle.plans, key = { "plan_${it.id}" }) { plan ->
                val isInferred = plan.entitlementStatus == EntitlementStatus.INFERRED_UNVERIFIED || !plan.isUserVerifiedEntitlement
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (isInferred) scout.alert.copy(alpha = 0.55f) else scout.hairlineBorder)
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
                            Text(plan.planName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            EntitlementBadge(plan.entitlementStatus)
                        }
                        val fixedText = plan.monthlyFixedCostUsd?.let { "$${"%.2f".format(it)}/mo fixed" } ?: "Fixed cost unverified"
                        Text(
                            text = "${plan.billingType.label} · $fixedText",
                            style = MaterialTheme.typography.labelMedium,
                            color = scout.verified
                        )
                        if (plan.meteredUnitCostDesc.isNotBlank()) {
                            Text(plan.meteredUnitCostDesc, style = MaterialTheme.typography.bodySmall)
                        }
                        if (plan.notes.isNotBlank()) {
                            Text(plan.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Capabilities
            item {
                SectionHeader(
                    title = "Capabilities & Quotas (${bundle.capabilities.size})",
                    trailing = {
                        TextButton(onClick = { showAddCapDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add")
                        }
                    }
                )
            }
            items(bundle.capabilities, key = { "cap_${it.id}" }) { cap ->
                val daysOld = FreshnessPolicy.daysSince(cap.verificationDateEpochMs)
                val isStale = FreshnessPolicy.isStale(cap.verificationDateEpochMs)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (isStale) scout.staleWarning.copy(alpha = 0.55f) else scout.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = cap.capabilityName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            UsageCostBadge(cap.usageModel, cap.incrementalCostEstimateUsd)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TelemetryBadge(cap.capabilityType.label, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
                            ConfidenceBadge(cap.confidence, isStale, daysOld)
                        }
                        Text("Quota: ${cap.quotaSummary}", style = MaterialTheme.typography.bodySmall)
                        if (cap.restrictionSummary.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = scout.staleWarning, modifier = Modifier.size(15.dp))
                                Text("Restriction: ${cap.restrictionSummary}", style = MaterialTheme.typography.bodySmall, color = scout.staleWarning)
                            }
                        }
                        if (cap.alternativesNote.isNotBlank()) {
                            Text("Alternative: ${cap.alternativesNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isStale || !cap.isVerified) {
                            OutlinedButton(
                                onClick = { onMarkCapabilityVerifiedNow(cap) },
                                modifier = Modifier.testTag("mark_verified_${cap.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Mark Re-Verified Today")
                            }
                        }
                    }
                }
            }

            // Verification Sources (with Open Link & Optional Public Header Refresh)
            item {
                SectionHeader(
                    title = "Verification Sources (${bundle.sources.size})",
                    subtitle = "Open official docs or check HTTP reachability & page title when online",
                    trailing = {
                        TextButton(onClick = { showAddSourceDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Link")
                        }
                    }
                )
            }
            items(bundle.sources, key = { "src_${it.id}" }) { src ->
                val daysOld = FreshnessPolicy.daysSince(src.lastVerifiedDateEpochMs)
                val isStale = FreshnessPolicy.isStale(src.lastVerifiedDateEpochMs)
                val isRefreshing = refreshingSourceId == src.id

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (isStale) scout.staleWarning.copy(alpha = 0.55f) else scout.hairlineBorder)
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
                            Text(src.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            ConfidenceBadge(src.confidence, isStale, daysOld)
                        }
                        Text(src.url, style = MaterialTheme.typography.labelSmall, color = scout.verified)
                        if (src.excerptNote.isNotBlank()) {
                            Text(src.excerptNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (src.lastHttpStatus != null) {
                            Text(
                                text = "Last public check (${FreshnessPolicy.formatDate(src.lastCheckedEpochMs ?: 0L)}): HTTP ${src.lastHttpStatus} · ${src.lastFetchedTitle ?: "OK"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (!AndroidActions.openExternalUrl(context, src.url)) {
                                        onNotify("Could not open link.")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Open")
                            }
                            OutlinedButton(
                                enabled = !isRefreshing && isOnline,
                                onClick = { onRefreshPublicSource(src) },
                                modifier = Modifier.testTag("refresh_source_${src.id}")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.size(4.dp))
                                Text(if (isOnline) "Check Header" else "Offline")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${s.name}?") },
            text = { Text("This removes the service, its plans, capabilities, sources, and snapshots from your local Room database.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDeleteService(s.id)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showAddCapDialog) {
        var name by remember { mutableStateOf("") }
        var quota by remember { mutableStateOf("") }
        var restriction by remember { mutableStateOf("") }
        var costStr by remember { mutableStateOf("") }
        var capType by remember { mutableStateOf(CapabilityType.MODEL_INFERENCE) }
        var usage by remember { mutableStateOf(UsageModel.FREE) }

        AlertDialog(
            onDismissRequest = { showAddCapDialog = false },
            title = { Text("Add Verified Capability") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Capability Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = quota, onValueChange = { quota = it }, label = { Text("Quota Summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Est. Incremental Cost USD (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = restriction, onValueChange = { restriction = it }, label = { Text("Restrictions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CapabilityType.entries.take(5).forEach { t ->
                            FilterChip(selected = capType == t, onClick = { capType = t }, label = { Text(t.shortLabel) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UsageModel.entries.forEach { u ->
                            FilterChip(selected = usage == u, onClick = { usage = u }, label = { Text(u.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddCapability(
                            CapabilityEntity(
                                serviceId = s.id,
                                capabilityName = name.trim(),
                                capabilityType = capType,
                                executionEnvironment = if (s.isCloudHosted) ExecutionEnvironment.CLOUD_MANAGED else ExecutionEnvironment.LOCAL_DEVICE,
                                quotaSummary = quota.trim().ifBlank { "User-entered quota" },
                                usageModel = usage,
                                incrementalCostEstimateUsd = costStr.toDoubleOrNull(),
                                costUnitLabel = if (usage == UsageModel.FREE) "free" else "estimated",
                                verificationSourceUrl = s.officialDocsUrl,
                                verificationDateEpochMs = System.currentTimeMillis(),
                                confidence = ConfidenceLevel.VERIFIED_HIGH,
                                isVerified = true,
                                restrictionSummary = restriction.trim(),
                                alternativesNote = "",
                                notes = ""
                            )
                        )
                        showAddCapDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCapDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddSourceDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSourceDialog = false },
            title = { Text("Add Official Verification Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Source Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Official Docs / Pricing URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Verified Excerpt / Note") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && url.startsWith("http")) {
                        onAddSource(
                            VerificationSourceEntity(
                                serviceId = s.id,
                                title = title.trim(),
                                url = url.trim(),
                                sourceType = SourceType.OFFICIAL_DOCS,
                                lastVerifiedDateEpochMs = System.currentTimeMillis(),
                                confidence = ConfidenceLevel.VERIFIED_HIGH,
                                excerptNote = note.trim()
                            )
                        )
                        showAddSourceDialog = false
                    }
                }) { Text("Save Link") }
            },
            dismissButton = { TextButton(onClick = { showAddSourceDialog = false }) { Text("Cancel") } }
        )
    }
}
