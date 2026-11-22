package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SavedAssessmentEntity
import com.example.ui.components.TelemetryBadge
import com.example.ui.screens.AddComboDialog
import com.example.ui.screens.AddServiceDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeltaScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.LabScreen
import com.example.ui.screens.LabSubTab
import com.example.ui.screens.MatrixScreen
import com.example.ui.screens.OfflineAssessmentReaderScreen
import com.example.ui.screens.ScoutRankerScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.theme.DexScoutTheme
import com.example.ui.theme.LocalScoutColors
import kotlinx.coroutines.launch

enum class ScoutDestination(val label: String, val icon: ImageVector) {
    BRIEF("Brief", Icons.Filled.Dashboard),
    MATRIX("Matrix", Icons.Filled.GridView),
    DELTA("Delta", Icons.Filled.CompareArrows),
    SCOUT("Scout", Icons.Filled.Radar),
    LAB("Lab", Icons.Filled.Science)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScoutApp(
    viewModel: ScoutViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(ScoutDestination.BRIEF) }
    var deltaFocusServiceId by remember { mutableStateOf<Long?>(null) }
    var activeServiceDetailId by remember { mutableStateOf<Long?>(null) }
    var activeAssessmentReader by remember { mutableStateOf<SavedAssessmentEntity?>(null) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddComboDialog by remember { mutableStateOf(false) }
    var labInitialTab by remember { mutableStateOf(LabSubTab.EXPERIMENTS) }

    LaunchedEffect(state.statusBannerMessage) {
        val msg = state.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    DexScoutTheme(darkTheme = state.darkTheme) {
        val scout = LocalScoutColors.current

        when {
            showGlobalSearch -> {
                GlobalSearchScreen(
                    query = state.globalSearchQuery,
                    results = state.globalSearchResults,
                    onQueryChange = viewModel::updateGlobalSearch,
                    onSelectService = { id ->
                        showGlobalSearch = false
                        activeServiceDetailId = id
                    },
                    onSelectAssessment = { id ->
                        showGlobalSearch = false
                        activeAssessmentReader = state.inventory.savedAssessments.firstOrNull { it.id == id }
                    },
                    onClose = { showGlobalSearch = false }
                )
            }

            activeAssessmentReader != null -> {
                OfflineAssessmentReaderScreen(
                    assessment = activeAssessmentReader!!,
                    onBack = { activeAssessmentReader = null }
                )
            }

            activeServiceDetailId != null -> {
                val bundle = state.inventory.bundles.firstOrNull { it.service.id == activeServiceDetailId }
                if (bundle != null) {
                    ServiceDetailScreen(
                        bundle = bundle,
                        refreshingSourceId = state.refreshingSourceId,
                        isOnline = state.isOnline,
                        onBack = { activeServiceDetailId = null },
                        onDeleteService = viewModel::deleteService,
                        onMarkCapabilityVerifiedNow = viewModel::markCapabilityVerifiedNow,
                        onAddCapability = viewModel::upsertCapability,
                        onAddSource = viewModel::upsertSource,
                        onRefreshPublicSource = viewModel::refreshPublicSource,
                        onCaptureSnapshot = viewModel::captureSnapshot,
                        onOpenDeltaForService = { svcId ->
                            activeServiceDetailId = null
                            deltaFocusServiceId = svcId
                            currentTab = ScoutDestination.DELTA
                        },
                        onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                } else {
                    activeServiceDetailId = null
                }
            }

            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavigationRail = maxWidth >= 600.dp

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "DEX//SCOUT",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TelemetryBadge(
                                            text = if (state.isOnline) "ONLINE" else "OFFLINE",
                                            containerColor = if (state.isOnline) scout.freeCostContainer else scout.verifiedContainer,
                                            contentColor = if (state.isOnline) scout.freeCost else scout.verified
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { showGlobalSearch = true },
                                        modifier = Modifier.testTag("top_search_button")
                                    ) {
                                        Icon(Icons.Filled.Search, contentDescription = "Global full-text search")
                                    }
                                    IconButton(
                                        onClick = viewModel::toggleTheme,
                                        modifier = Modifier.testTag("top_theme_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (state.darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = "Toggle color theme"
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!useNavigationRail) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                    ScoutDestination.entries.forEach { dest ->
                                        NavigationBarItem(
                                            selected = currentTab == dest,
                                            onClick = { currentTab = dest },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useNavigationRail) {
                                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                    ScoutDestination.entries.forEach { dest ->
                                        NavigationRailItem(
                                            selected = currentTab == dest,
                                            onClick = { currentTab = dest },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val contentMod = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 840.dp)

                                when (currentTab) {
                                    ScoutDestination.BRIEF -> DashboardScreen(
                                        state = state,
                                        onOpenDelta = { svcId ->
                                            deltaFocusServiceId = svcId
                                            currentTab = ScoutDestination.DELTA
                                        },
                                        onOpenMatrixStaleOnly = {
                                            viewModel.updateMatrixFilter {
                                                it.copy(staleOnly = true, hideStale = false)
                                            }
                                            currentTab = ScoutDestination.MATRIX
                                        },
                                        onOpenMatrixAll = {
                                            viewModel.resetMatrixFilter()
                                            currentTab = ScoutDestination.MATRIX
                                        },
                                        onOpenServiceDetail = { id -> activeServiceDetailId = id },
                                        onOpenAssessment = { activeAssessmentReader = it },
                                        onOpenScout = { currentTab = ScoutDestination.SCOUT },
                                        onAddComboClick = { showAddComboDialog = true },
                                        modifier = contentMod
                                    )

                                    ScoutDestination.MATRIX -> MatrixScreen(
                                        state = state,
                                        onUpdateFilter = viewModel::updateMatrixFilter,
                                        onResetFilter = viewModel::resetMatrixFilter,
                                        onOpenServiceDetail = { id -> activeServiceDetailId = id },
                                        onAddServiceClick = { showAddServiceDialog = true },
                                        modifier = contentMod
                                    )

                                    ScoutDestination.DELTA -> DeltaScreen(
                                        state = state,
                                        initialServiceId = deltaFocusServiceId,
                                        onCaptureSnapshot = viewModel::captureSnapshot,
                                        modifier = contentMod
                                    )

                                    ScoutDestination.SCOUT -> ScoutRankerScreen(
                                        state = state,
                                        onUpdateTaskSpec = viewModel::updateTaskSpec,
                                        onSaveAssessmentOffline = { viewModel.saveCurrentAssessmentOffline() },
                                        onRunOptionalGemini = viewModel::runOptionalGeminiBrief,
                                        onClearGemini = viewModel::clearGeminiAnalysis,
                                        onOpenServiceDetail = { id -> activeServiceDetailId = id },
                                        modifier = contentMod
                                    )

                                    ScoutDestination.LAB -> LabScreen(
                                        state = state,
                                        initialSubTab = labInitialTab,
                                        onAddExperiment = viewModel::addExperiment,
                                        onDeleteExperiment = viewModel::deleteExperiment,
                                        onAddDiscovery = viewModel::addDiscovery,
                                        onUpdateDiscoveryStatus = { disc, status ->
                                            viewModel.updateDiscovery(disc.copy(status = status))
                                        },
                                        onOpenAssessment = { activeAssessmentReader = it },
                                        onDeleteAssessment = viewModel::deleteAssessment,
                                        onBuildExport = viewModel::buildExportPayload,
                                        onResetSyntheticSample = viewModel::resetToSyntheticSample,
                                        onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                        modifier = contentMod
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddServiceDialog) {
            AddServiceDialog(
                onDismiss = { showAddServiceDialog = false },
                onSave = viewModel::addServiceBundle
            )
        }

        if (showAddComboDialog) {
            AddComboDialog(
                bundles = state.inventory.bundles,
                onDismiss = { showAddComboDialog = false },
                onSave = viewModel::addFavoriteCombo
            )
        }
    }
}
