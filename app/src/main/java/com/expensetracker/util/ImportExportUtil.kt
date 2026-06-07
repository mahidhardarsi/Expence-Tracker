package com.expensetracker.util

import com.expensetracker.domain.model.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken

object ImportExportUtil {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    val LEGACY_IMPORT_JSON_SAMPLE: String = gson.toJson(
        listOf(
            mapOf(
                "id" to 101,
                "name" to "March 2026",
                "expenses" to listOf(
                    mapOf("id" to 1001, "name" to "Salary", "amount" to 55000, "date" to "2026-03-01", "category" to "Salary", "type" to "credit"),
                    mapOf("id" to 1002, "name" to "Groceries", "amount" to 2400, "date" to "2026-03-05", "category" to "Food", "type" to "debit")
                )
            )
        )
    )

    val LEGACY_IMPORT_CSV_SAMPLE: String = listOf(
        "Report,Name,Date,Category,Type,Amount",
        "March 2026,Salary,2026-03-01,Salary,credit,55000",
        "March 2026,Groceries,2026-03-05,Food,debit,2400"
    ).joinToString("\n")

    fun serializeLegacyJson(reports: List<Report>, transactions: List<Transaction>): String {
        val legacy = buildLegacyReports(reports, transactions)
        return gson.toJson(legacy)
    }

    fun serializeLegacyCsv(reports: List<Report>, transactions: List<Transaction>): String {
        val lines = mutableListOf("Report,Name,Date,Category,Type,Amount")
        for (report in buildLegacyReports(reports, transactions)) {
            for (expense in report.expenses) {
                lines.add(buildCsvRow(report.name, expense))
            }
        }
        return lines.joinToString("\n")
    }

    fun serializeLegacyJsonForReport(report: Report, transactions: List<Transaction>): String {
        return gson.toJson(listOf(buildLegacyReport(report, transactions)))
    }

    fun serializeLegacyCsvForReport(report: Report, transactions: List<Transaction>): String {
        val lines = mutableListOf("Report,Name,Date,Category,Type,Amount")
        for (expense in buildLegacyReport(report, transactions).expenses) {
            lines.add(buildCsvRow(report.name, expense))
        }
        return lines.joinToString("\n")
    }

    fun parseLegacyJson(content: String): List<LegacyReport> {
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val raw: List<Map<String, Any>> = gson.fromJson(content, type)
            ?: throw IllegalArgumentException("The JSON import must be an array of reports.")
        return raw.map { sanitizeReport(it) }
    }

    fun parseLegacyCsv(content: String): List<LegacyReport> {
        val lines = content.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = parseCsvRow(lines[0]).map { it.lowercase() }
        val reportIdx = headers.indexOfFirst { it.contains("report") }
        val nameIdx = headers.indexOfFirst { it == "name" }
        val dateIdx = headers.indexOfFirst { it.contains("date") }
        val categoryIdx = headers.indexOfFirst { it.contains("category") }
        val typeIdx = headers.indexOfFirst { it.contains("type") }
        val amountIdx = headers.indexOfFirst { it.contains("amount") }

        if (listOf(reportIdx, nameIdx, dateIdx, categoryIdx, typeIdx, amountIdx).any { it < 0 }) {
            throw IllegalArgumentException("The CSV file is missing one or more required columns.")
        }

        val grouped = linkedMapOf<String, LegacyReport>()
        for (line in lines.drop(1)) {
            val cols = parseCsvRow(line)
            val reportName = cols.getOrNull(reportIdx)?.trim()?.ifBlank { "Imported Report" } ?: "Imported Report"
            val existing = grouped.getOrPut(reportName) {
                LegacyReport(IdGenerator.newId(), reportName, emptyList())
            }
            val tx = LegacyTransaction(
                id = IdGenerator.newId(),
                name = cols.getOrNull(nameIdx)?.trim()?.ifBlank { "Transaction" } ?: "Transaction",
                amount = cols.getOrNull(amountIdx)?.toDoubleOrNull() ?: 0.0,
                date = cols.getOrNull(dateIdx)?.trim()?.ifBlank { todayIsoDate() } ?: todayIsoDate(),
                category = cols.getOrNull(categoryIdx)?.trim()?.ifBlank { "Other" } ?: "Other",
                type = if (cols.getOrNull(typeIdx)?.trim() == "credit") TransactionType.CREDIT else TransactionType.DEBIT
            )
            grouped[reportName] = existing.copy(expenses = existing.expenses + tx)
        }
        return grouped.values.toList()
    }

    fun extractImportedCategoryNames(reports: List<LegacyReport>): List<String> {
        val defaultSet = DEFAULT_CATEGORIES.map { it.lowercase() }.toSet()
        return reports.flatMap { it.expenses }
            .map { it.category }
            .filter { !defaultSet.contains(it.lowercase()) }
            .distinct()
            .sorted()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildLegacyReports(reports: List<Report>, transactions: List<Transaction>): List<LegacyReport> {
        return reports.map { buildLegacyReport(it, transactions) }
    }

    private fun buildLegacyReport(report: Report, transactions: List<Transaction>): LegacyReport {
        val txs = transactions.filter { it.reportId == report.id }
        return LegacyReport(
            id = report.id,
            name = report.name,
            expenses = txs.map {
                LegacyTransaction(it.id, it.name, it.amount, it.date, it.category, it.type)
            }
        )
    }

    private fun buildCsvRow(reportName: String, expense: LegacyTransaction): String {
        fun escape(s: String) = "\"${s.replace("\"", "\"\"")}\""
        return listOf(
            escape(reportName),
            escape(expense.name),
            expense.date,
            expense.category,
            if (expense.type == TransactionType.CREDIT) "credit" else "debit",
            expense.amount.toString()
        ).joinToString(",")
    }

    private fun parseCsvRow(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { values.add(current.toString().trim()); current.clear() }
                else -> current.append(ch)
            }
        }
        values.add(current.toString().trim())
        return values
    }

    @Suppress("UNCHECKED_CAST")
    private fun sanitizeReport(map: Map<String, Any>): LegacyReport {
        val id = (map["id"] as? Number)?.toLong() ?: IdGenerator.newId()
        val name = (map["name"] as? String)?.trim()?.ifBlank { "Imported Report" } ?: "Imported Report"
        val expenses = (map["expenses"] as? List<*>)?.filterIsInstance<Map<String, Any>>()?.map { sanitizeTransaction(it) } ?: emptyList()
        return LegacyReport(id, name, expenses)
    }

    private fun sanitizeTransaction(map: Map<String, Any>): LegacyTransaction {
        return LegacyTransaction(
            id = (map["id"] as? Number)?.toLong() ?: IdGenerator.newId(),
            name = (map["name"] as? String)?.trim()?.ifBlank { "Transaction" } ?: "Transaction",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            date = (map["date"] as? String)?.trim()?.ifBlank { todayIsoDate() } ?: todayIsoDate(),
            category = (map["category"] as? String)?.trim()?.ifBlank { "Other" } ?: "Other",
            type = if (map["type"] == "credit") TransactionType.CREDIT else TransactionType.DEBIT
        )
    }
}
