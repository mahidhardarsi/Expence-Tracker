package com.expensetracker.feature.ai.domain.usecase

import com.expensetracker.domain.model.ChatMessage
import com.expensetracker.feature.ai.data.InferenceEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatUseCase @Inject constructor(
    private val inferenceEngine: InferenceEngine
) {
    operator fun invoke(systemPrompt: String, history: List<ChatMessage>, userInput: String): Flow<String> {
        return inferenceEngine.streamChat(systemPrompt, pruneHistory(history), userInput)
    }

    companion object {
        fun pruneHistory(messages: List<ChatMessage>, maxTokens: Int = 800): List<ChatMessage> {
            var tokenCount = 0
            val kept = mutableListOf<ChatMessage>()
            for (message in messages.reversed()) {
                val estimate = message.content.length / 4
                if (tokenCount + estimate > maxTokens) break
                kept.add(0, message)
                tokenCount += estimate
            }
            return kept
        }
    }
}
