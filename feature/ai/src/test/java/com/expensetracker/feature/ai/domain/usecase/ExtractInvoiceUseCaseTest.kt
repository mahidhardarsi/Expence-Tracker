package com.expensetracker.feature.ai.domain.usecase

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategoryKind
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractInvoiceUseCaseTest {
    private val categories = listOf(
        Category("Food", CategoryKind.DEFAULT),
        Category("Bills", CategoryKind.DEFAULT),
        Category("Other", CategoryKind.DEFAULT)
    )

    @Test
    fun `parses fenced json output`() {
        val result = ExtractInvoiceUseCase.parseInvoiceJson(
            """
            ```json
            {
              "merchant": "Swiggy",
              "amount": 420.0,
              "date": "2026-05-18",
              "category": "Food",
              "type": "debit",
              "notes": "Dinner order"
            }
            ```
            """.trimIndent(),
            categories,
            Gson()
        )

        assertEquals("Swiggy", result?.merchant)
        assertEquals(420.0, result?.amount ?: 0.0, 0.0)
        assertEquals("Food", result?.category)
    }

    @Test
    fun `falls back to Other for unknown category`() {
        val result = ExtractInvoiceUseCase.parseInvoiceJson(
            """{"merchant":"Vendor","amount":99.0,"date":"2026-05-18","category":"Travel","type":"debit","notes":"Taxi"}""",
            categories,
            Gson()
        )

        assertEquals("Other", result?.category)
    }

    @Test
    fun `returns null for non json output`() {
        assertNull(ExtractInvoiceUseCase.parseInvoiceJson("sorry I cannot parse this", categories, Gson()))
    }
}
