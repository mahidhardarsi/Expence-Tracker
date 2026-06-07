package com.expensetracker.feature.ai.domain.usecase

import com.expensetracker.domain.model.ChatMessage
import com.expensetracker.domain.model.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUseCaseTest {
    @Test
    fun `pruneHistory keeps newest messages within token budget`() {
        val messages = listOf(
            ChatMessage("1", ChatRole.USER, "short", 1L),
            ChatMessage("2", ChatRole.MODEL, "a".repeat(400), 2L),
            ChatMessage("3", ChatRole.USER, "recent question", 3L),
            ChatMessage("4", ChatRole.MODEL, "recent answer", 4L)
        )

        val pruned = ChatUseCase.pruneHistory(messages, maxTokens = 120)

        assertEquals(listOf("3", "4"), pruned.map { it.id })
        assertTrue(pruned.last().content.contains("recent answer"))
    }
}
