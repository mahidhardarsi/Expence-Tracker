package com.expensetracker.feature.ai.domain.usecase

import com.expensetracker.domain.model.AnalysisFilter
import com.expensetracker.domain.model.Report
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.feature.ai.util.FinanceAnalysisUtils
import javax.inject.Inject

class BuildFinancePromptUseCase @Inject constructor() {
    operator fun invoke(
        reports: List<Report>,
        transactions: List<Transaction>,
        currentReportId: Long?,
        analysisFilter: AnalysisFilter? = null
    ): String {
        val filtered = if (analysisFilter != null) {
            FinanceAnalysisUtils.filterTransactions(transactions, analysisFilter)
        } else {
            currentReportId?.let { reportId -> transactions.filter { it.reportId == reportId } } ?: transactions
        }
        val currentReport = reports.firstOrNull { it.id == currentReportId }
        val summary = FinanceAnalysisUtils.computeReportSummary(filtered)
        val categoryBreakdown = FinanceAnalysisUtils
            .computeCategoryBreakdown(filtered, TransactionType.DEBIT)
            .take(5)
            .joinToString { "${it.label}: ₹${it.value.toLong()}" }

        val recentLines = filtered.take(10).joinToString("\n") {
            "- ${it.date} | ${it.name} | ${it.category} | ${if (it.type == TransactionType.CREDIT) "+" else "-"}₹${it.amount.toLong()}"
        }

        return """
            You are a personal finance assistant embedded in an expense tracker app.
            Answer only from the user's local financial data below.
            Be concise, use Indian Rupee (₹), and say clearly when data is missing.

            === Current Report: ${currentReport?.name ?: "All Reports"} ===
            Total Income: ₹${summary.credit.toLong()}
            Total Expense: ₹${summary.debit.toLong()}
            Net Balance: ₹${summary.net.toLong()}
            Transactions: ${summary.count}

            Top Expense Categories: $categoryBreakdown

            Recent Transactions:
            $recentLines
        """.trimIndent()
    }
}
