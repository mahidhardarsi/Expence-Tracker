package com.expensetracker.domain.repository

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.ImportResult
import com.expensetracker.domain.model.LegacyReport
import com.expensetracker.domain.model.Report
import com.expensetracker.domain.model.ReportSortMode
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionInput
import com.expensetracker.domain.model.TransactionSortMode
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeReports(): Flow<List<Report>>
    suspend fun getReports(): List<Report>
    suspend fun createReport(name: String): Long
    suspend fun renameReport(reportId: Long, name: String)
    suspend fun deleteReport(reportId: Long): Long
    suspend fun moveReportCustomOrder(reportId: Long, direction: String): Boolean

    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun getTransactions(): List<Transaction>
    suspend fun addTransaction(input: TransactionInput): Long
    suspend fun updateTransaction(transactionId: Long, input: TransactionInput)
    suspend fun deleteTransaction(transactionId: Long)

    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategories(): List<Category>
    suspend fun addCustomCategory(name: String)
    suspend fun removeCustomCategory(name: String)

    suspend fun getSettings(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setCurrentReportId(reportId: Long)
    suspend fun setMigrationHintDismissed(value: Boolean)
    suspend fun setReportSortMode(mode: ReportSortMode)
    suspend fun setTransactionSortMode(mode: TransactionSortMode)

    suspend fun importLegacyJson(reports: List<LegacyReport>): ImportResult
    suspend fun importLegacyCsv(reports: List<LegacyReport>): ImportResult
}

interface AiFinanceRepository {
    suspend fun getReports(): List<Report>
    suspend fun getTransactions(reportId: Long? = null): List<Transaction>
    suspend fun getCategories(): List<Category>
    suspend fun getSettings(): AppSettings
}
