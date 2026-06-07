package com.expensetracker.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AnalysisFilter
import com.expensetracker.domain.model.ChatMessage
import com.expensetracker.domain.model.ChatRole
import com.expensetracker.domain.repository.AiFinanceRepository
import com.expensetracker.feature.ai.domain.usecase.BuildFinancePromptUseCase
import com.expensetracker.feature.ai.domain.usecase.ChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val title: String = "AI Assistant",
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val canChat: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repository: AiFinanceRepository,
    private val buildFinancePromptUseCase: BuildFinancePromptUseCase,
    private val chatUseCase: ChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var currentReportId: Long? = null
    private var currentFilter: AnalysisFilter? = null

    fun configure(reportId: Long?, filter: AnalysisFilter?, title: String) {
        currentReportId = reportId
        currentFilter = filter
        _uiState.update { it.copy(title = title) }
    }

    fun sendMessage(userText: String) = viewModelScope.launch {
        if (userText.isBlank()) return@launch

        val reports = repository.getReports()
        val transactions = repository.getTransactions(currentReportId)
        val systemPrompt = buildFinancePromptUseCase(reports, transactions, currentReportId, currentFilter)

        val userMessage = ChatMessage(UUID.randomUUID().toString(), ChatRole.USER, userText.trim(), System.currentTimeMillis())
        val assistantMessage = ChatMessage(UUID.randomUUID().toString(), ChatRole.MODEL, "", System.currentTimeMillis(), isStreaming = true)

        _uiState.update { it.copy(messages = it.messages + userMessage + assistantMessage, isSending = true, errorMessage = null) }

        runCatching {
            chatUseCase(systemPrompt, _uiState.value.messages.filter { it.role != ChatRole.SYSTEM }, userText.trim())
                .collect { token ->
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        val lastIndex = updated.lastIndex
                        if (lastIndex >= 0) {
                            val current = updated[lastIndex]
                            updated[lastIndex] = current.copy(content = current.content + token, isStreaming = true)
                        }
                        state.copy(messages = updated)
                    }
                }
        }.onFailure { error ->
            _uiState.update {
                it.copy(isSending = false, errorMessage = error.message ?: "Chat failed")
            }
        }.onSuccess {
            _uiState.update { state ->
                val updated = state.messages.toMutableList()
                val lastIndex = updated.lastIndex
                if (lastIndex >= 0) {
                    updated[lastIndex] = updated[lastIndex].copy(isStreaming = false)
                }
                state.copy(messages = updated, isSending = false)
            }
        }
    }
}
