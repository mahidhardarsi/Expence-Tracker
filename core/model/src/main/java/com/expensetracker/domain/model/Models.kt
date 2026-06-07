package com.expensetracker.domain.model

enum class TransactionType { CREDIT, DEBIT }
enum class CategoryKind { DEFAULT, CUSTOM }
enum class ThemeMode { LIGHT, DARK }
enum class ReportSortMode { NAME, RECENT, AMOUNT, CUSTOM }
enum class TransactionSortMode { RECENT, OLDEST, AMOUNT, NAME }
enum class AnalysisPeriod { FULL, MONTHLY, ANNUALLY, CUSTOM }
enum class AnalysisScope { REPORT, PERIOD, REPORT_PERIOD }

data class Report(
    val id: Long,
    val name: String,
    val updatedAt: String,
    val customOrder: Int
)

data class Transaction(
    val id: Long,
    val reportId: Long,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType
)

data class Category(
    val name: String,
    val kind: CategoryKind
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val currentReportId: Long? = null,
    val migrationHintDismissed: Boolean = false,
    val reportSortMode: ReportSortMode = ReportSortMode.NAME,
    val transactionSortMode: TransactionSortMode = TransactionSortMode.RECENT
)

data class TransactionInput(
    val reportId: Long,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType
)

data class AnalysisFilter(
    val reportId: Long?,
    val reportIdIsAll: Boolean = false,
    val scope: AnalysisScope = AnalysisScope.REPORT,
    val period: AnalysisPeriod = AnalysisPeriod.FULL,
    val month: Int = 1,
    val year: Int = 2024,
    val fromDate: String = "",
    val toDate: String = ""
)

data class AnalysisSummary(
    val income: Double,
    val expense: Double,
    val net: Double,
    val count: Int
)

data class ReportSummary(
    val credit: Double,
    val debit: Double,
    val net: Double,
    val count: Int
)

data class MonthlyBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategoryBreakdownItem(
    val label: String,
    val value: Double,
    val color: Long
)

data class TopCategoryItem(
    val rank: Int,
    val label: String,
    val amount: Double,
    val color: Long,
    val percentage: Double
)

data class LegacyTransaction(
    val id: Long,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType
)

data class LegacyReport(
    val id: Long,
    val name: String,
    val expenses: List<LegacyTransaction>
)

data class ImportResult(
    val importedReports: Int,
    val importedTransactions: Int,
    val skippedReports: Int = 0
)

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val supportsVision: Boolean,
    val minRamMb: Int,
    val recommended: Boolean
)

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data object Queued : ModelState
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : ModelState
    data object Downloaded : ModelState
    data object Loading : ModelState
    data class Ready(val modelId: String, val supportsVision: Boolean) : ModelState
    data class Error(val message: String) : ModelState
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

enum class ChatRole { SYSTEM, USER, MODEL }

data class InvoiceFields(
    val merchant: String = "",
    val amount: Double? = null,
    val date: String = "",
    val category: String = "Other",
    val type: TransactionType = TransactionType.DEBIT,
    val notes: String = ""
)

data class AiDownloadProgress(
    val modelId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progressPercent: Int
)

enum class AiCapability { TEXT, VISION }

val DEFAULT_CATEGORIES = listOf(
    "Bills", "Food", "Health", "Investment", "Other", "Salary", "Shopping", "Transport"
)

val CHART_COLORS = listOf(
    0xFF2563EBL, 0xFF2DA44EL, 0xFF7C3AEDL, 0xFFD97706L,
    0xFFDC2626L, 0xFF4C8DA6L, 0xFF73849AL, 0xFF5E8B5AL
)
