package com.expensetracker.feature.ai.domain.usecase

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.InvoiceFields
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.feature.ai.data.InferenceEngine
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import javax.inject.Inject

class ExtractInvoiceUseCase @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val gson: Gson
) {
    fun streamExtraction(imagePath: String, prompt: String): Flow<String> = inferenceEngine.extractInvoice(imagePath, prompt)

    suspend fun parseExtraction(chunks: Flow<String>, categories: List<Category>): InvoiceFields? {
        val raw = chunks.fold(StringBuilder()) { acc, token -> acc.append(token) }.toString()
        return parseInvoiceJson(raw, categories, gson)
    }

    fun buildPrompt(): String = """
        You are a financial data extraction assistant. Extract fields from the provided receipt or invoice image.
        Respond only with valid JSON.
        {
          "merchant": "name",
          "amount": 0.0,
          "date": "YYYY-MM-DD",
          "category": "Bills|Food|Health|Investment|Other|Salary|Shopping|Transport",
          "type": "debit",
          "notes": "brief description"
        }
        Rules:
        - amount is numeric only
        - date must be ISO 8601, use today's date if unclear
        - category must be one of the provided categories
        - type should be credit only for refunds or salary slips
    """.trimIndent()

    fun parseInvoiceJson(rawOutput: String, categories: List<Category>): InvoiceFields? {
        return parseInvoiceJson(rawOutput, categories, gson)
    }

    companion object {
        fun parseInvoiceJson(rawOutput: String, categories: List<Category>, gson: Gson): InvoiceFields? {
            val cleaned = rawOutput.replace(Regex("```json|```"), "").trim()
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) return null
            val normalized = cleaned.substring(jsonStart, jsonEnd + 1)
            return runCatching {
                val payload = gson.fromJson(normalized, ParsedInvoicePayload::class.java)
                val category = matchCategory(payload.category, categories)
                InvoiceFields(
                    merchant = payload.merchant.orEmpty(),
                    amount = payload.amount,
                    date = payload.date.orEmpty(),
                    category = category,
                    type = if (payload.type.equals("credit", ignoreCase = true)) TransactionType.CREDIT else TransactionType.DEBIT,
                    notes = payload.notes.orEmpty()
                )
            }.getOrNull()
        }

        private fun matchCategory(rawCategory: String?, categories: List<Category>): String {
            val requested = rawCategory?.trim().orEmpty()
            return categories.firstOrNull { it.name.equals(requested, ignoreCase = true) }?.name
                ?: categories.firstOrNull { it.name.equals("Other", ignoreCase = true) }?.name
                ?: "Other"
        }
    }

    private data class ParsedInvoicePayload(
        val merchant: String? = null,
        val amount: Double? = null,
        val date: String? = null,
        val category: String? = null,
        val type: String? = null,
        val notes: String? = null
    )
}
