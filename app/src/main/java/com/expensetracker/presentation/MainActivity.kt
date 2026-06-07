package com.expensetracker.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.*
import com.expensetracker.feature.ai.data.AiPreferencesStore
import com.expensetracker.feature.ai.data.InferenceEngine
import com.expensetracker.feature.ai.presentation.AiChatScreen
import com.expensetracker.feature.ai.presentation.InvoiceScannerSheet
import com.expensetracker.feature.ai.presentation.ModelHubScreen
import com.expensetracker.presentation.ui.*
import com.expensetracker.presentation.ui.analysis.AnalysisScreen
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.ui.options.OptionsScreen
import com.expensetracker.presentation.ui.reports.*
import com.expensetracker.presentation.ui.theme.*
import com.expensetracker.presentation.viewmodel.*
import com.expensetracker.util.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var aiPreferencesStore: AiPreferencesStore
    @Inject lateinit var inferenceEngine: InferenceEngine

    private val unloadHandler = Handler(Looper.getMainLooper())
    private val unloadRunnable = Runnable { inferenceEngine.unloadModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash until data is loaded
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.status == AppStatus.LOADING ||
            viewModel.uiState.value.status == AppStatus.IDLE
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.settings.themeMode == ThemeMode.DARK

            ExpenseTrackerTheme(darkTheme = isDark) {
                AppRoot(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        unloadHandler.removeCallbacks(unloadRunnable)
    }

    override fun onStop() {
        super.onStop()
        unloadHandler.postDelayed(unloadRunnable, 30_000L)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            inferenceEngine.unloadModel()
        }
    }
}

@Composable
private fun AppRoot(uiState: AppUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf<AppTab>(AppTab.Reports) }
    var overlayRoute by remember { mutableStateOf<String?>(null) }

    // Reports sub-navigation
    var reportsRoute by remember { mutableStateOf<String>("list") }
    var selectedReportId by remember { mutableStateOf<Long?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionMode by remember { mutableStateOf("create") }
    var pendingInvoiceFields by remember { mutableStateOf<InvoiceFields?>(null) }
    var showInvoiceScanner by remember { mutableStateOf(false) }

    val activity = context as MainActivity
    val aiPrefs by activity.aiPreferencesStore.snapshotFlow().collectAsStateWithLifecycle(
        initialValue = com.expensetracker.feature.ai.data.AiPreferencesSnapshot()
    )
    val aiEngineState by activity.inferenceEngine.state.collectAsStateWithLifecycle()
    val isAiAvailable = remember(aiPrefs.activeModelId, aiEngineState) {
        aiPrefs.activeModelId != null && aiEngineState is com.expensetracker.domain.model.ModelState.Ready
    }
    val supportsVision = (aiEngineState as? com.expensetracker.domain.model.ModelState.Ready)?.supportsVision == true

    LaunchedEffect(aiPrefs.activeModelId) {
        val activeModelId = aiPrefs.activeModelId ?: return@LaunchedEffect
        if (aiEngineState !is com.expensetracker.domain.model.ModelState.Ready) {
            runCatching { activity.inferenceEngine.loadModel(activeModelId) }
        }
    }

    // Toast / snackbar
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    // Sync selectedReportId with settings
    LaunchedEffect(uiState.settings.currentReportId) {
        if (selectedReportId == null && uiState.settings.currentReportId != null) {
            selectedReportId = uiState.settings.currentReportId
        }
    }

    if (uiState.status == AppStatus.ERROR) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load data.\n${uiState.error}", color = MaterialTheme.appColors.accentRed)
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        bottomBar = {
            if (overlayRoute == null && (reportsRoute == "list" || currentTab != AppTab.Reports)) {
                AppBottomBar(currentTab) { tab ->
                    currentTab = tab
                    if (tab == AppTab.Reports) reportsRoute = "list"
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            BackHandler(enabled = overlayRoute != null || reportsRoute != "list" || showInvoiceScanner) {
                when {
                    showInvoiceScanner -> showInvoiceScanner = false
                    overlayRoute != null -> overlayRoute = null
                    reportsRoute == "transactionForm" -> {
                        reportsRoute = if (selectedReportId != null) "detail" else "list"
                        editingTransaction = null
                    }
                    reportsRoute == "detail" -> reportsRoute = "list"
                }
            }

            when {
                overlayRoute == "modelHub" -> {
                    ModelHubScreen(
                        onBack = { overlayRoute = null }
                    )
                }

                overlayRoute == "reportAiChat" -> {
                    AiChatScreen(
                        title = "AI Assistant",
                        reportId = selectedReportId ?: uiState.settings.currentReportId,
                        analysisFilter = null,
                        onBack = { overlayRoute = null }
                    )
                }

                overlayRoute == "analysisAiChat" -> {
                    AiChatScreen(
                        title = "AI Analysis",
                        reportId = uiState.analysisFilter.reportId,
                        analysisFilter = uiState.analysisFilter,
                        onBack = { overlayRoute = null }
                    )
                }

                // Transaction form
                currentTab == AppTab.Reports && reportsRoute == "transactionForm" -> {
                    val report = uiState.reports.find { it.id == (selectedReportId ?: uiState.settings.currentReportId) }
                    TransactionFormScreen(
                        editingTransaction = if (transactionMode == "edit") editingTransaction else null,
                        reports = uiState.reports,
                        categories = uiState.categories,
                        currentReportId = selectedReportId ?: uiState.settings.currentReportId,
                        prefillFields = pendingInvoiceFields,
                        onPrefillConsumed = { pendingInvoiceFields = null },
                        onScanInvoice = { showInvoiceScanner = true },
                        onSave = { input ->
                            if (transactionMode == "edit" && editingTransaction != null) {
                                viewModel.updateTransaction(editingTransaction!!.id, input)
                            } else {
                                viewModel.addTransaction(input)
                            }
                            reportsRoute = "detail"
                            editingTransaction = null
                        },
                        onCancel = {
                            reportsRoute = if (transactionMode == "edit") "detail" else "detail"
                            editingTransaction = null
                        }
                    )
                }

                // Report detail
                currentTab == AppTab.Reports && reportsRoute == "detail" -> {
                    val activeId = selectedReportId ?: uiState.settings.currentReportId
                    val report = uiState.reports.find { it.id == activeId }
                    val reportTxs = uiState.transactions.filter { it.reportId == activeId }
                    ReportDetailScreen(
                        report = report,
                        transactions = reportTxs,
                        sortMode = uiState.settings.transactionSortMode,
                        onBack = { reportsRoute = "list" },
                        onAddTransaction = {
                            transactionMode = "create"
                            editingTransaction = null
                            reportsRoute = "transactionForm"
                        },
                        onEditTransaction = { tx ->
                            transactionMode = "edit"
                            editingTransaction = tx
                            reportsRoute = "transactionForm"
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onRenameReport = { name -> activeId?.let { viewModel.renameReport(it, name) } },
                        onDeleteReport = {
                            activeId?.let { viewModel.deleteReport(it) }
                            reportsRoute = "list"
                        },
                        onExportJson = {
                            report?.let {
                                val content = ImportExportUtil.serializeLegacyJsonForReport(it, reportTxs)
                                val filename = buildExportFilename(it.name, "json")
                                val savedUri = FileUtil.saveToDownloads(context, filename, content, "application/json")
                                viewModel.showToast(if (savedUri != null) "Saved to Downloads/ExpenseTracker/$filename" else "Export failed")
                            }
                        },
                        onExportCsv = {
                            report?.let {
                                val content = ImportExportUtil.serializeLegacyCsvForReport(it, reportTxs)
                                val filename = buildExportFilename(it.name, "csv")
                                val savedUri = FileUtil.saveToDownloads(context, filename, content, "text/csv")
                                viewModel.showToast(if (savedUri != null) "Saved to Downloads/ExpenseTracker/$filename" else "Export failed")
                            }
                        },
                        onSetSortMode = { viewModel.setTransactionSortMode(it) },
                        isAiAvailable = isAiAvailable,
                        onOpenAiChat = { overlayRoute = "reportAiChat" }
                    )
                }

                // Reports list
                currentTab == AppTab.Reports -> {
                    ReportsListScreen(
                        reports = uiState.reports,
                        transactions = uiState.transactions,
                        currentReportId = uiState.settings.currentReportId,
                        sortMode = uiState.settings.reportSortMode,
                        onSelectReport = {
                            selectedReportId = it
                            viewModel.setCurrentReport(it)
                        },
                        onOpenReport = {
                            selectedReportId = it
                            reportsRoute = "detail"
                        },
                        onAddTransaction = {
                            transactionMode = "create"
                            editingTransaction = null
                            reportsRoute = "transactionForm"
                        },
                        onSetSortMode = { viewModel.setReportSortMode(it) },
                        onMoveUp = { viewModel.moveReportCustomOrder(it, "up") },
                        onMoveDown = { viewModel.moveReportCustomOrder(it, "down") }
                    )
                    // Floating create button with inline dialog
                    CreateReportFab(onCreate = { name -> viewModel.createReport(name) })
                }

                // Analysis
                currentTab == AppTab.Analysis -> {
                    AnalysisScreen(
                        reports = uiState.reports,
                        transactions = uiState.transactions,
                        filter = uiState.analysisFilter,
                        onFilterChange = { viewModel.setAnalysisFilter(it) },
                        isAiAvailable = isAiAvailable,
                        onOpenAiChat = { overlayRoute = "analysisAiChat" }
                    )
                }

                // Options
                currentTab == AppTab.Options -> {
                    OptionsScreen(
                        settings = uiState.settings,
                        categories = uiState.categories,
                        reports = uiState.reports,
                        transactions = uiState.transactions,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onAddCategory = { viewModel.addCategory(it) },
                        onRemoveCategory = { viewModel.removeCategory(it) },
                        onExportAllJson = { content ->
                            val filename = buildExportFilename("all_reports", "json")
                            val savedUri = FileUtil.saveToDownloads(context, filename, content, "application/json")
                            viewModel.showToast(if (savedUri != null) "Saved to Downloads/ExpenseTracker/$filename" else "Export failed")
                        },
                        onExportAllCsv = { content ->
                            val filename = buildExportFilename("all_reports", "csv")
                            val savedUri = FileUtil.saveToDownloads(context, filename, content, "text/csv")
                            viewModel.showToast(if (savedUri != null) "Saved to Downloads/ExpenseTracker/$filename" else "Export failed")
                        },
                        onImportJson = { content ->
                            viewModel.importJson(content) { result ->
                                viewModel.showToast("Imported ${result.importedReports} reports, ${result.importedTransactions} transactions. Skipped: ${result.skippedReports}")
                            }
                        },
                        onImportCsv = { content ->
                            viewModel.importCsv(content) { result ->
                                viewModel.showToast("Imported ${result.importedReports} reports, ${result.importedTransactions} transactions.")
                            }
                        },
                        onShowMessage = { viewModel.showToast(it) },
                        onOpenAiModels = { overlayRoute = "modelHub" }
                    )
                }
            }

            // Global loading overlay
            if (uiState.busy) {
                LoadingOverlay()
            }

            if (showInvoiceScanner) {
                InvoiceScannerSheet(
                    categories = uiState.categories,
                    visionAvailable = supportsVision,
                    onDismiss = { showInvoiceScanner = false },
                    onOpenModelHub = {
                        showInvoiceScanner = false
                        overlayRoute = "modelHub"
                    },
                    onApply = {
                        pendingInvoiceFields = it
                        showInvoiceScanner = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateReportFab(onCreate: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    // The FAB is placed via Box alignment since Scaffold's FAB slot is used by detail screen
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.appColors.accentBlue,
            contentColor = androidx.compose.ui.graphics.Color.White
        ) {
            Icon(Icons.Default.Add, "Create Report")
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Report") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Report name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { if (name.isNotBlank()) { onCreate(name); showDialog = false } }) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
