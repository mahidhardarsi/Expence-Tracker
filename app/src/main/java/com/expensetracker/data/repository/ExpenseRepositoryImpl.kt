package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.*
import com.expensetracker.data.local.database.DatabaseInitializer
import com.expensetracker.data.local.entity.*
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AiFinanceRepository
import com.expensetracker.domain.repository.ExpenseRepository
import com.expensetracker.util.IdGenerator
import com.expensetracker.util.nowIsoTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTING_THEME = "themeMode"
private const val SETTING_CURRENT_REPORT = "currentReportId"
private const val SETTING_MIGRATION_HINT = "migrationHintDismissed"
private const val SETTING_REPORT_SORT = "reportSortMode"
private const val SETTING_TRANSACTION_SORT = "transactionSortMode"

private val defaultCategorySet = DEFAULT_CATEGORIES.map { it.lowercase() }.toSet()

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val settingDao: SettingDao,
    private val initializer: DatabaseInitializer
) : ExpenseRepository, AiFinanceRepository {

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun ReportEntity.toDomain() = Report(id, name, updatedAt, customOrder)
    private fun TransactionEntity.toDomain() = Transaction(
        id, reportId, name, amount, date, category,
        if (type == "credit") TransactionType.CREDIT else TransactionType.DEBIT
    )
    private fun CategoryEntity.toDomain() = Category(
        name, if (kind == "default") CategoryKind.DEFAULT else CategoryKind.CUSTOM
    )
    private fun TransactionType.toDb() = if (this == TransactionType.CREDIT) "credit" else "debit"

    // ── Reports ──────────────────────────────────────────────────────────────

    override fun observeReports() = reportDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getReports(): List<Report> {
        initializer.ensureInitialized()
        return reportDao.getAll().map { it.toDomain() }
    }

    override suspend fun createReport(name: String): Long {
        initializer.ensureInitialized()
        val id = IdGenerator.newId()
        val order = reportDao.maxCustomOrder() + 1
        reportDao.insert(ReportEntity(id, name.trim().ifBlank { "My Report" }, nowIsoTimestamp(), order))
        settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, id.toString()))
        return id
    }

    override suspend fun renameReport(reportId: Long, name: String) {
        reportDao.rename(reportId, name.trim().ifBlank { "My Report" }, nowIsoTimestamp())
    }

    override suspend fun deleteReport(reportId: Long): Long {
        val reports = reportDao.getAll()
        val currentReportId = settingDao.getValue(SETTING_CURRENT_REPORT)?.toLongOrNull()

        if (reports.size <= 1) {
            reportDao.delete(reportId)
            val fallbackId = IdGenerator.newId()
            reportDao.insert(ReportEntity(fallbackId, "My Report", nowIsoTimestamp(), 0))
            settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, fallbackId.toString()))
            return fallbackId
        }

        reportDao.delete(reportId)
        initializer.normalizeReportOrder()

        if (currentReportId == reportId) {
            settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, "0"))
        }
        return currentReportId ?: 0L
    }

    override suspend fun moveReportCustomOrder(reportId: Long, direction: String): Boolean {
        initializer.normalizeReportOrder()
        val reports = reportDao.getAll().sortedBy { it.customOrder }
        val currentIndex = reports.indexOfFirst { it.id == reportId }
        if (currentIndex < 0) return false

        val swapIndex = if (direction == "up") currentIndex - 1 else currentIndex + 1
        if (swapIndex < 0 || swapIndex >= reports.size) return false

        val current = reports[currentIndex]
        val target = reports[swapIndex]
        reportDao.setCustomOrder(current.id, target.customOrder)
        reportDao.setCustomOrder(target.id, current.customOrder)
        return true
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    override fun observeTransactions() = transactionDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getTransactions(): List<Transaction> {
        initializer.ensureInitialized()
        return transactionDao.getAll().map { it.toDomain() }
    }

    override suspend fun getTransactions(reportId: Long?): List<Transaction> {
        initializer.ensureInitialized()
        val entities = if (reportId == null) transactionDao.getAll() else transactionDao.getByReport(reportId)
        return entities.map { it.toDomain() }
    }

    override suspend fun addTransaction(input: TransactionInput): Long {
        ensureCategory(input.category)
        val id = IdGenerator.newId()
        transactionDao.insert(
            TransactionEntity(
                id = id,
                reportId = input.reportId,
                name = input.name.trim().ifBlank { "Transaction" },
                amount = input.amount,
                date = input.date,
                category = input.category,
                type = input.type.toDb()
            )
        )
        reportDao.touch(input.reportId, nowIsoTimestamp())
        return id
    }

    override suspend fun updateTransaction(transactionId: Long, input: TransactionInput) {
        val existing = transactionDao.getById(transactionId)
        ensureCategory(input.category)
        transactionDao.update(
            TransactionEntity(
                id = transactionId,
                reportId = input.reportId,
                name = input.name.trim().ifBlank { "Transaction" },
                amount = input.amount,
                date = input.date,
                category = input.category,
                type = input.type.toDb()
            )
        )
        existing?.reportId?.let { reportDao.touch(it, nowIsoTimestamp()) }
        if (existing?.reportId != input.reportId) {
            reportDao.touch(input.reportId, nowIsoTimestamp())
        }
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        val existing = transactionDao.getById(transactionId)
        transactionDao.delete(transactionId)
        existing?.reportId?.let { reportDao.touch(it, nowIsoTimestamp()) }
    }

    // ── Categories ───────────────────────────────────────────────────────────

    override fun observeCategories() = categoryDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getCategories(): List<Category> = categoryDao.getAll().map { it.toDomain() }

    override suspend fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("Category name cannot be empty.")
        if (categoryDao.findByName(trimmed) != null) throw IllegalArgumentException("This category already exists.")
        categoryDao.insertIgnore(CategoryEntity(trimmed, "custom"))
    }

    override suspend fun removeCustomCategory(name: String) {
        categoryDao.deleteCustom(name)
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    override suspend fun getSettings(): AppSettings {
        initializer.ensureInitialized()
        val map = settingDao.getAll().associate { it.key to it.value }
        val reports = reportDao.getAll()
        val storedReportId = map[SETTING_CURRENT_REPORT]?.toLongOrNull()
        var currentReportId = storedReportId ?: reports.firstOrNull()?.id

        if (currentReportId != null && reports.none { it.id == currentReportId }) {
            currentReportId = reports.firstOrNull()?.id
            currentReportId?.let { settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, it.toString())) }
        }

        val reportSortMode = when (map[SETTING_REPORT_SORT]) {
            "recent" -> ReportSortMode.RECENT
            "amount" -> ReportSortMode.AMOUNT
            "custom" -> ReportSortMode.CUSTOM
            else -> ReportSortMode.NAME
        }
        val transactionSortMode = when (map[SETTING_TRANSACTION_SORT]) {
            "oldest" -> TransactionSortMode.OLDEST
            "amount" -> TransactionSortMode.AMOUNT
            "name" -> TransactionSortMode.NAME
            else -> TransactionSortMode.RECENT
        }

        return AppSettings(
            themeMode = if (map[SETTING_THEME] == "dark") ThemeMode.DARK else ThemeMode.LIGHT,
            currentReportId = currentReportId,
            migrationHintDismissed = map[SETTING_MIGRATION_HINT] == "true",
            reportSortMode = reportSortMode,
            transactionSortMode = transactionSortMode
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        settingDao.upsert(SettingEntity(SETTING_THEME, if (mode == ThemeMode.DARK) "dark" else "light"))
    }

    override suspend fun setCurrentReportId(reportId: Long) {
        settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, reportId.toString()))
    }

    override suspend fun setMigrationHintDismissed(value: Boolean) {
        settingDao.upsert(SettingEntity(SETTING_MIGRATION_HINT, value.toString()))
    }

    override suspend fun setReportSortMode(mode: ReportSortMode) {
        val v = when (mode) {
            ReportSortMode.RECENT -> "recent"
            ReportSortMode.AMOUNT -> "amount"
            ReportSortMode.CUSTOM -> "custom"
            else -> "name"
        }
        settingDao.upsert(SettingEntity(SETTING_REPORT_SORT, v))
    }

    override suspend fun setTransactionSortMode(mode: TransactionSortMode) {
        val v = when (mode) {
            TransactionSortMode.OLDEST -> "oldest"
            TransactionSortMode.AMOUNT -> "amount"
            TransactionSortMode.NAME -> "name"
            else -> "recent"
        }
        settingDao.upsert(SettingEntity(SETTING_TRANSACTION_SORT, v))
    }

    // ── Import ───────────────────────────────────────────────────────────────

    override suspend fun importLegacyJson(reports: List<LegacyReport>): ImportResult {
        initializer.ensureInitialized()
        val existingIds = reportDao.getAll().map { it.id }.toMutableSet()
        var importedReports = 0
        var importedTransactions = 0
        var skippedReports = 0
        var nextOrder = reportDao.maxCustomOrder() + 1

        for (report in reports) {
            if (existingIds.contains(report.id)) { skippedReports++; continue }
            reportDao.insert(ReportEntity(report.id, report.name.trim().ifBlank { "Imported Report" }, nowIsoTimestamp(), nextOrder))
            existingIds.add(report.id)
            importedReports++
            nextOrder++

            for (expense in report.expenses) {
                ensureCategory(expense.category)
                val safeId = getUniqueTransactionId(expense.id)
                transactionDao.insert(TransactionEntity(safeId, report.id, expense.name.trim().ifBlank { "Transaction" }, expense.amount, expense.date, expense.category, expense.type.toDb()))
                importedTransactions++
            }
            reportDao.touch(report.id, nowIsoTimestamp())
        }
        return ImportResult(importedReports, importedTransactions, skippedReports)
    }

    override suspend fun importLegacyCsv(reports: List<LegacyReport>): ImportResult {
        initializer.ensureInitialized()
        var importedReports = 0
        var importedTransactions = 0
        var nextOrder = reportDao.maxCustomOrder() + 1

        for (report in reports) {
            val existing = reportDao.getAll().firstOrNull { it.name == report.name }
            val reportId = existing?.id ?: run {
                val id = IdGenerator.newId()
                reportDao.insert(ReportEntity(id, report.name.trim().ifBlank { "Imported Report" }, nowIsoTimestamp(), nextOrder))
                importedReports++
                nextOrder++
                id
            }
            for (expense in report.expenses) {
                ensureCategory(expense.category)
                val safeId = getUniqueTransactionId(expense.id)
                transactionDao.insert(TransactionEntity(safeId, reportId, expense.name.trim().ifBlank { "Transaction" }, expense.amount, expense.date, expense.category, expense.type.toDb()))
                importedTransactions++
            }
            reportDao.touch(reportId, nowIsoTimestamp())
        }
        return ImportResult(importedReports, importedTransactions)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun ensureCategory(name: String) {
        if (categoryDao.findByName(name) != null) return
        val kind = if (defaultCategorySet.contains(name.lowercase())) "default" else "custom"
        categoryDao.insertIgnore(CategoryEntity(name, kind))
    }

    private suspend fun getUniqueTransactionId(candidateId: Long): Long {
        return if (transactionDao.exists(candidateId) != null) IdGenerator.newId() else candidateId
    }
}
