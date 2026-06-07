package com.expensetracker.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object IdGenerator {
    fun newId(): Long = System.currentTimeMillis() + (0..999).random()
}

fun nowIsoTimestamp(): String = java.time.Instant.now().toString()

fun todayIsoDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatCurrency(amount: Double): String {
    return "₹" + String.format(Locale("en", "IN"), "%,.2f", amount)
}

fun formatCurrencyCompact(amount: Double): String {
    return when {
        amount >= 10_00_000 -> "₹%.1fL".format(amount / 1_00_000)
        amount >= 1_000 -> "₹%.1fK".format(amount / 1_000)
        else -> "₹%.0f".format(amount)
    }
}

fun isoDateToDisplay(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) {
        isoDate
    }
}

fun currentYear(): Int = LocalDate.now().year
fun currentMonth(): Int = LocalDate.now().monthValue

fun monthLabel(year: Int, month: Int): String {
    return YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMM yyyy"))
}

fun buildExportFilename(base: String, extension: String): String {
    val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val safeName = base.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
    return "${safeName}_$timestamp.$extension"
}
