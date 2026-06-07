package com.expensetracker.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.InvoiceFields
import com.expensetracker.feature.ai.domain.usecase.ExtractInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InvoiceScannerStep { IDLE, PREVIEW, LOADING, SUCCESS, ERROR }

data class InvoiceScannerUiState(
    val step: InvoiceScannerStep = InvoiceScannerStep.IDLE,
    val imagePath: String? = null,
    val extractedFields: InvoiceFields? = null,
    val tokenPreview: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class InvoiceScannerViewModel @Inject constructor(
    private val extractInvoiceUseCase: ExtractInvoiceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceScannerUiState())
    val uiState: StateFlow<InvoiceScannerUiState> = _uiState.asStateFlow()

    fun setPreviewImage(path: String) {
        _uiState.value = InvoiceScannerUiState(step = InvoiceScannerStep.PREVIEW, imagePath = path)
    }

    fun extract(categories: List<Category>) = viewModelScope.launch {
        val imagePath = _uiState.value.imagePath ?: return@launch
        _uiState.update { it.copy(step = InvoiceScannerStep.LOADING, tokenPreview = "", errorMessage = null) }
        runCatching {
            val prompt = extractInvoiceUseCase.buildPrompt()
            val chunks = extractInvoiceUseCase.streamExtraction(imagePath, prompt)
            chunks.collect { token ->
                _uiState.update { state -> state.copy(tokenPreview = state.tokenPreview + token) }
            }
            extractInvoiceUseCase.parseInvoiceJson(_uiState.value.tokenPreview, categories)
        }.onSuccess { result ->
            if (result == null) {
                _uiState.update { it.copy(step = InvoiceScannerStep.ERROR, errorMessage = "Could not extract fields.") }
            } else {
                _uiState.update { it.copy(step = InvoiceScannerStep.SUCCESS, extractedFields = result) }
            }
        }.onFailure { error ->
            _uiState.update { it.copy(step = InvoiceScannerStep.ERROR, errorMessage = error.message ?: "Extraction failed") }
        }
    }

    fun reset() {
        _uiState.value = InvoiceScannerUiState()
    }
}
