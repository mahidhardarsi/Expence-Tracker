package com.expensetracker.feature.ai.util

import com.expensetracker.domain.model.AnalysisFilter
import com.expensetracker.domain.model.AnalysisPeriod
import com.expensetracker.domain.model.AnalysisScope
import com.expensetracker.domain.model.CategoryBreakdownItem
import com.expensetracker.domain.model.CHART_COLORS
import com.expensetracker.domain.model.ReportSummary
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FinanceAnalysisUtils {
    fun filterTransactions(transactions: List<Transaction>, filter: AnalysisFilter): List<Transaction> {
        var result = transactions
        if (!filter.reportIdIsAll && filter.reportId != null) {
            result = result.filter { it.reportId == filter.reportId }
        }
        if (filter.scope != AnalysisScope.REPORT) {
            result = when (filter.period) {
                AnalysisPeriod.MONTHLY -> result.filter {
                    parseDate(it.date)?.let { date -> date.year == filter.year && date.monthValue == filter.month } == true
                }
                AnalysisPeriod.ANNUALLY -> result.filter {
                    parseDate(it.date)?.year == filter.year
                }
                AnalysisPeriod.CUSTOM -> {
                    val from = parseDate(filter.fromDate)
                    val to = parseDate(filter.toDate)
                    result.filter {
                        val date = parseDate(it.date) ?: return@filter false
                        (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to))
                    }
                }
                AnalysisPeriod.FULL -> result
            }
        }
        return result
    }

    fun computeReportSummary(transactions: List<Transaction>): ReportSummary {
        var credit = 0.0
        var debit = 0.0
        transactions.forEach {
            if (it.type == TransactionType.CREDIT) credit += it.amount else debit += it.amount
        }
        return ReportSummary(credit = credit, debit = debit, net = credit - debit, count = transactions.size)
    }

    fun computeCategoryBreakdown(transactions: List<Transaction>, type: TransactionType): List<CategoryBreakdownItem> {
        val grouped = linkedMapOf<String, Double>()
        transactions.filter { it.type == type }.forEach { transaction ->
            grouped[transaction.category] = (grouped[transaction.category] ?: 0.0) + transaction.amount
        }
        return grouped.entries.sortedByDescending { it.value }.mapIndexed { index, entry ->
            CategoryBreakdownItem(entry.key, entry.value, CHART_COLORS[index % CHART_COLORS.size])
        }
    }

    private fun parseDate(raw: String): LocalDate? = runCatching {
        LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
}
