package com.expensetracker.util

import com.expensetracker.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AnalysisUtil {

    fun filterTransactions(
        transactions: List<Transaction>,
        filter: AnalysisFilter
    ): List<Transaction> {
        var result = transactions

        // Report filter
        if (!filter.reportIdIsAll && filter.reportId != null) {
            result = result.filter { it.reportId == filter.reportId }
        }

        // Date/period filter
        if (filter.period != AnalysisPeriod.FULL || filter.scope != AnalysisScope.REPORT) {
            result = when (filter.period) {
                AnalysisPeriod.MONTHLY -> result.filter {
                    val d = parseDate(it.date) ?: return@filter false
                    d.year == filter.year && d.monthValue == filter.month
                }
                AnalysisPeriod.ANNUALLY -> result.filter {
                    val d = parseDate(it.date) ?: return@filter false
                    d.year == filter.year
                }
                AnalysisPeriod.CUSTOM -> {
                    val from = parseDate(filter.fromDate)
                    val to = parseDate(filter.toDate)
                    result.filter {
                        val d = parseDate(it.date) ?: return@filter false
                        (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to))
                    }
                }
                AnalysisPeriod.FULL -> result
            }
        }

        return result
    }

    fun computeSummary(transactions: List<Transaction>): AnalysisSummary {
        var income = 0.0
        var expense = 0.0
        for (t in transactions) {
            if (t.type == TransactionType.CREDIT) income += t.amount
            else expense += t.amount
        }
        return AnalysisSummary(income, expense, income - expense, transactions.size)
    }

    fun computeReportSummary(transactions: List<Transaction>): ReportSummary {
        var credit = 0.0
        var debit = 0.0
        for (t in transactions) {
            if (t.type == TransactionType.CREDIT) credit += t.amount
            else debit += t.amount
        }
        return ReportSummary(credit, debit, credit - debit, transactions.size)
    }

    fun computeMonthlyBuckets(transactions: List<Transaction>): List<MonthlyBucket> {
        val grouped = mutableMapOf<String, Pair<Double, Double>>() // key -> (income, expense)
        for (t in transactions) {
            val d = parseDate(t.date) ?: continue
            val key = "%04d-%02d".format(d.year, d.monthValue)
            val label = monthLabel(d.year, d.monthValue)
            val cur = grouped.getOrPut(key) { Pair(0.0, 0.0) }
            if (t.type == TransactionType.CREDIT) {
                grouped[key] = Pair(cur.first + t.amount, cur.second)
            } else {
                grouped[key] = Pair(cur.first, cur.second + t.amount)
            }
        }
        return grouped.entries.sortedBy { it.key }.map { (key, pair) ->
            val parts = key.split("-")
            MonthlyBucket(monthLabel(parts[0].toInt(), parts[1].toInt()), pair.first, pair.second)
        }
    }

    fun computeCategoryBreakdown(
        transactions: List<Transaction>,
        type: TransactionType
    ): List<CategoryBreakdownItem> {
        val grouped = mutableMapOf<String, Double>()
        for (t in transactions.filter { it.type == type }) {
            grouped[t.category] = (grouped[t.category] ?: 0.0) + t.amount
        }
        val total = grouped.values.sum()
        if (total == 0.0) return emptyList()

        return grouped.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (cat, amount) ->
                CategoryBreakdownItem(cat, amount, CHART_COLORS[index % CHART_COLORS.size])
            }
    }

    fun computeTopCategories(
        transactions: List<Transaction>,
        type: TransactionType
    ): List<TopCategoryItem> {
        val items = computeCategoryBreakdown(transactions, type)
        val total = items.sumOf { it.value }
        return items.take(5).mapIndexed { index, item ->
            TopCategoryItem(
                rank = index + 1,
                label = item.label,
                amount = item.value,
                color = item.color,
                percentage = if (total > 0) (item.value / total * 100) else 0.0
            )
        }
    }

    fun sortReportsByMode(
        reports: List<Report>,
        transactions: List<Transaction>,
        mode: ReportSortMode
    ): List<Report> {
        return when (mode) {
            ReportSortMode.NAME -> reports.sortedBy { it.name.lowercase() }
            ReportSortMode.RECENT -> reports.sortedByDescending { it.updatedAt }
            ReportSortMode.AMOUNT -> {
                val totals = reports.associateWith { report ->
                    transactions.filter { it.reportId == report.id }.sumOf { it.amount }
                }
                reports.sortedByDescending { totals[it] ?: 0.0 }
            }
            ReportSortMode.CUSTOM -> reports.sortedBy { it.customOrder }
        }
    }

    fun sortTransactions(
        transactions: List<Transaction>,
        mode: TransactionSortMode
    ): List<Transaction> {
        return when (mode) {
            TransactionSortMode.RECENT -> transactions.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
            TransactionSortMode.OLDEST -> transactions.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id })
            TransactionSortMode.AMOUNT -> transactions.sortedByDescending { it.amount }
            TransactionSortMode.NAME -> transactions.sortedBy { it.name.lowercase() }
        }
    }

    private fun parseDate(iso: String): LocalDate? = try {
        LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) { null }
}
