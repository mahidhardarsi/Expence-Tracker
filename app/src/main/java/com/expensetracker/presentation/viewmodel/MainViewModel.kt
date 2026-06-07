package com.expensetracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.ExpenseRepository
import com.expensetracker.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AppUiState(
    val status: AppStatus = AppStatus.IDLE,
    val busy: Boolean = false,
    val error: String? = null,
    val reports: List<Report> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val analysisFilter: AnalysisFilter = defaultAnalysisFilter(null),
    val toastMessage: String? = null
)

enum class AppStatus { IDLE, LOADING, READY, ERROR }

fun defaultAnalysisFilter(currentReportId: Long?) = AnalysisFilter(
    reportId = currentReportId,
    reportIdIsAll = false,
    scope = AnalysisScope.REPORT,
    period = AnalysisPeriod.FULL,
    month = LocalDate.now().monthValue,
    year = LocalDate.now().year,
    fromDate = "",
    toDate = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AppStatus.LOADING, busy = true, error = null) }
            runCatching { hydrate() }
                .onSuccess { _uiState.update { it.copy(status = AppStatus.READY, busy = false) } }
                .onFailure { e -> _uiState.update { it.copy(status = AppStatus.ERROR, busy = false, error = e.message) } }
        }
    }

    private suspend fun hydrate() {
        val reports = repository.getReports()
        val transactions = repository.getTransactions()
        val categories = repository.getCategories()
        val settings = repository.getSettings()
        val sortedReports = AnalysisUtil.sortReportsByMode(reports, transactions, settings.reportSortMode)
        val currentFilter = reconcileFilter(_uiState.value.analysisFilter, sortedReports, settings)

        _uiState.update { state ->
            state.copy(
                reports = sortedReports,
                transactions = transactions,
                categories = categories,
                settings = settings,
                analysisFilter = currentFilter,
                busy = false,
                error = null
            )
        }
    }

    private fun reconcileFilter(current: AnalysisFilter, reports: List<Report>, settings: AppSettings): AnalysisFilter {
        val reportExists = when {
            current.reportIdIsAll -> true
            current.reportId == null -> false
            else -> reports.any { it.id == current.reportId }
        }
        val base = defaultAnalysisFilter(settings.currentReportId)
        return current.copy(reportId = if (reportExists) current.reportId else settings.currentReportId)
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message, status = AppStatus.ERROR) } }
    }

    fun setCurrentReport(reportId: Long) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching {
            repository.setCurrentReportId(reportId)
            hydrate()
            _uiState.update { it.copy(analysisFilter = it.analysisFilter.copy(reportId = reportId, reportIdIsAll = false)) }
        }.onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun setAnalysisFilter(patch: AnalysisFilter) {
        _uiState.update { it.copy(analysisFilter = patch) }
    }

    fun setReportSortMode(mode: ReportSortMode) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, settings = it.settings.copy(reportSortMode = mode)) }
        runCatching { repository.setReportSortMode(mode); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun setTransactionSortMode(mode: TransactionSortMode) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, settings = it.settings.copy(transactionSortMode = mode)) }
        runCatching { repository.setTransactionSortMode(mode); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun moveReportCustomOrder(reportId: Long, direction: String) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true) }
        runCatching { repository.moveReportCustomOrder(reportId, direction); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun toggleTheme() = viewModelScope.launch {
        val currentMode = _uiState.value.settings.themeMode
        val newMode = if (currentMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        _uiState.update { it.copy(settings = it.settings.copy(themeMode = newMode)) }
        runCatching { repository.setThemeMode(newMode) }
            .onFailure { _uiState.update { it.copy(settings = it.settings.copy(themeMode = currentMode)) } }
    }

    fun dismissMigrationHint() = viewModelScope.launch {
        runCatching { repository.setMigrationHintDismissed(true); hydrate() }
    }

    fun createReport(name: String) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.createReport(name); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun renameReport(reportId: Long, name: String) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.renameReport(reportId, name); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun deleteReport(reportId: Long) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.deleteReport(reportId); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun addTransaction(input: TransactionInput) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.addTransaction(input); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun updateTransaction(transactionId: Long, input: TransactionInput) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.updateTransaction(transactionId, input); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun deleteTransaction(transactionId: Long) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.deleteTransaction(transactionId); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun addCategory(name: String) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.addCustomCategory(name); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun removeCategory(name: String) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching { repository.removeCustomCategory(name); hydrate() }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun importJson(content: String, onResult: (ImportResult) -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching {
            val parsed = ImportExportUtil.parseLegacyJson(content)
            val result = repository.importLegacyJson(parsed)
            hydrate()
            result
        }.onSuccess { onResult(it) }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun importCsv(content: String, onResult: (ImportResult) -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(busy = true, error = null) }
        runCatching {
            val parsed = ImportExportUtil.parseLegacyCsv(content)
            val result = repository.importLegacyCsv(parsed)
            hydrate()
            result
        }.onSuccess { onResult(it) }
            .onFailure { e -> _uiState.update { it.copy(busy = false, error = e.message) } }
    }

    fun clearToast() = _uiState.update { it.copy(toastMessage = null) }

    fun showToast(message: String) = _uiState.update { it.copy(toastMessage = message) }
}
