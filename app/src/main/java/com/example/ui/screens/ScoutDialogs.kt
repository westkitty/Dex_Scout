package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CapabilityEntity
import com.example.data.local.FavoriteComboEntity
import com.example.data.local.SavedAssessmentEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SubscriptionPlanEntity
import com.example.data.local.VerificationSourceEntity
import com.example.data.repository.ServiceBundle
import com.example.domain.FreshnessPolicy
import com.example.domain.GlobalSearchResult
import com.example.domain.model.BillingType
import com.example.domain.model.CapabilityType
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.EntitlementStatus
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.model.HardwareTier
import com.example.domain.model.PrivacyTier
import com.example.domain.model.ServiceCategory
import com.example.domain.model.SourceType
import com.example.domain.model.UsageModel
import com.example.ui.components.OptionalAiBadge
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalScoutColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddServiceDialog(
    onDismiss: () -> Unit,
    onSave: (ServiceEntity, SubscriptionPlanEntity?, CapabilityEntity?, VerificationSourceEntity?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ServiceCategory.DEV_PLATFORM) }
    var docsUrl by remember { mutableStateOf("https://") }
    var isCloud by remember { mutableStateOf(true) }
    var inferenceOnly by remember { mutableStateOf(false) }
    var remoteBuild by remember { mutableStateOf(false) }
    var planName by remember { mutableStateOf("Personal Plan") }
    var entitlement by remember { mutableStateOf(EntitlementStatus.VERIFIED_ACTIVE) }
    var monthlyCostText by remember { mutableStateOf("") }
    var capName by remember { mutableStateOf("") }
    var capType by remember { mutableStateOf(CapabilityType.MODEL_INFERENCE) }
    var usageModel by remember { mutableStateOf(UsageModel.FREE) }
    var quotaSummary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service to Inventory") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_service_name_input")
                )
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = docsUrl,
                    onValueChange = { docsUrl = it },
                    label = { Text("Official Documentation URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cloud Hosted", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isCloud, onCheckedChange = { isCloud = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Model Inference Only (No Remote Build)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = inferenceOnly, onCheckedChange = { inferenceOnly = it; if (it) remoteBuild = false })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Supports Remote Build / Exec", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = remoteBuild, onCheckedChange = { remoteBuild = it; if (it) inferenceOnly = false })
                }

                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Subscription Plan Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyCostText,
                    onValueChange = { monthlyCostText = it },
                    label = { Text("Verified Monthly Cost USD (blank = unknown)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Entitlement Verification", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EntitlementStatus.entries.forEach { e ->
                        FilterChip(selected = entitlement == e, onClick = { entitlement = e }, label = { Text(e.label) })
                    }
                }

                OutlinedTextField(
                    value = capName,
                    onValueChange = { capName = it },
                    label = { Text("Primary Capability Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quotaSummary,
                    onValueChange = { quotaSummary = it },
                    label = { Text("Verified Quota Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UsageModel.entries.forEach { u ->
                        FilterChip(selected = usageModel == u, onClick = { usageModel = u }, label = { Text(u.label) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CapabilityType.entries.take(5).forEach { ct ->
                        FilterChip(selected = capType == ct, onClick = { capType = ct }, label = { Text(ct.shortLabel) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        val cost = monthlyCostText.toDoubleOrNull()
                        val service = ServiceEntity(
                            name = name.trim(),
                            vendor = vendor.trim().ifBlank { "User Added" },
                            category = category,
                            summary = "User-managed ${category.label} entry.",
                            officialDocsUrl = docsUrl.trim(),
                            pricingUrl = docsUrl.trim(),
                            isCloudHosted = isCloud,
                            supportsModelInferenceOnly = inferenceOnly,
                            supportsRemoteBuild = remoteBuild,
                            supportsRemoteTest = remoteBuild,
                            supportsRemoteRender = false,
                            supportsRemoteCompute = remoteBuild,
                            localHardwareRequirement = if (isCloud) HardwareTier.NONE else HardwareTier.MEDIUM,
                            storageRequirementGb = if (isCloud) 0.5 else 8.0,
                            privacyTier = PrivacyTier.STANDARD_CLOUD,
                            isSyntheticSample = false,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now
                        )
                        val plan = SubscriptionPlanEntity(
                            serviceId = 0,
                            planName = planName.trim().ifBlank { "Recorded Plan" },
                            entitlementStatus = entitlement,
                            billingType = if ((cost ?: 0.0) > 0.0) BillingType.PAID_FIXED else BillingType.FREE,
                            monthlyFixedCostUsd = cost,
                            meteredUnitCostDesc = "",
                            isUserVerifiedEntitlement = entitlement.isVerifiedAccess,
                            notes = "Entered by user."
                        )
                        val cap = if (capName.isNotBlank()) {
                            CapabilityEntity(
                                serviceId = 0,
                                capabilityName = capName.trim(),
                                capabilityType = capType,
                                executionEnvironment = if (isCloud) ExecutionEnvironment.CLOUD_MANAGED else ExecutionEnvironment.LOCAL_DEVICE,
                                quotaSummary = quotaSummary.trim().ifBlank { "Not yet documented" },
                                usageModel = usageModel,
                                incrementalCostEstimateUsd = if (usageModel == UsageModel.FREE || usageModel == UsageModel.INCLUDED) 0.0 else null,
                                costUnitLabel = usageModel.label.lowercase(),
                                verificationSourceUrl = docsUrl.trim(),
                                verificationDateEpochMs = now,
                                confidence = if (entitlement.isVerifiedAccess) ConfidenceLevel.VERIFIED_HIGH else ConfidenceLevel.INFERRED_LOW,
                                isVerified = entitlement.isVerifiedAccess,
                                restrictionSummary = "",
                                alternativesNote = "",
                                notes = ""
                            )
                        } else null
                        val source = if (docsUrl.startsWith("http") && docsUrl.length > 10) {
                            VerificationSourceEntity(
                                serviceId = 0,
                                title = "${name.trim()} Official Docs",
                                url = docsUrl.trim(),
                                sourceType = SourceType.OFFICIAL_DOCS,
                                lastVerifiedDateEpochMs = now,
                                confidence = ConfidenceLevel.VERIFIED_HIGH,
                                excerptNote = "User-recorded verification link."
                            )
                        } else null

                        onSave(service, plan, cap, source)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_service")
            ) {
                Text("Save Offline")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddComboDialog(
    bundles: List<ServiceBundle>,
    onDismiss: () -> Unit,
    onSave: (FavoriteComboEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var taskProfile by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Favorite Tool Stack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Stack Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = taskProfile, onValueChange = { taskProfile = it }, label = { Text("Task Profile") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rationale, onValueChange = { rationale = it }, label = { Text("Why This Pairing Works") }, modifier = Modifier.fillMaxWidth())
                Text("Select Tools:", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    bundles.forEach { b ->
                        val isSelected = b.service.id in selectedIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedIds = if (isSelected) selectedIds - b.service.id else selectedIds + b.service.id
                            },
                            label = { Text(b.service.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedIds.isNotEmpty()) {
                        val estMonthly = bundles
                            .filter { it.service.id in selectedIds }
                            .sumOf { it.verifiedMonthlyFixedCostUsd }
                        onSave(
                            FavoriteComboEntity(
                                name = name.trim(),
                                taskProfile = taskProfile.trim().ifBlank { "Custom workflow" },
                                serviceIdsCsv = selectedIds.joinToString(","),
                                rationale = rationale.trim().ifBlank { "Verified user-curated tool stack." },
                                estimatedMonthlyCostUsd = estMonthly
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Stack") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    query: String,
    results: List<GlobalSearchResult>,
    onQueryChange: (String) -> Unit,
    onSelectService: (Long) -> Unit,
    onSelectAssessment: (Long) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val scout = LocalScoutColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full-Text Inventory Search") },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("search_close_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close search")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search services, quotas, restrictions, links, experiments...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            if (query.isBlank()) {
                Text(
                    "Instant offline full-text search across services, capabilities, restrictions, verification links, experiments, discoveries, and saved assessments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (results.isEmpty()) {
                Text("No offline records match \"$query\".", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(results) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        item.serviceId != null -> onSelectService(item.serviceId)
                                        item.assessmentId != null -> onSelectAssessment(item.assessmentId)
                                    }
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, scout.hairlineBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TelemetryBadge(
                                    text = item.typeBadge,
                                    containerColor = scout.verifiedContainer,
                                    contentColor = scout.verified
                                )
                                Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineAssessmentReaderScreen(
    assessment: SavedAssessmentEntity,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val scout = LocalScoutColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Assessment") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryBadge(
                text = "SAVED OFFLINE · ${FreshnessPolicy.formatDate(assessment.createdAtEpochMs)}",
                containerColor = scout.verifiedContainer,
                contentColor = scout.verified
            )
            Text(assessment.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Top recommendation: ${assessment.topRecommendationName}",
                style = MaterialTheme.typography.titleSmall,
                color = scout.verified
            )
            Text(assessment.requirementsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, scout.hairlineBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("DETERMINISTIC EVALUATION", style = MaterialTheme.typography.labelSmall, color = scout.verified)
                    Text(assessment.deterministicSummaryMarkdown, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!assessment.optionalGeminiAnalysis.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = scout.aiAccentContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, scout.aiAccent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionalAiBadge()
                        Text(assessment.optionalGeminiAnalysis, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
