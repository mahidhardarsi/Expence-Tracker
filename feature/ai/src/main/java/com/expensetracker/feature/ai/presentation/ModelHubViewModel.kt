package com.expensetracker.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AiModel
import com.expensetracker.domain.model.ModelState
import com.expensetracker.feature.ai.data.AiPreferencesStore
import com.expensetracker.feature.ai.data.InferenceEngine
import com.expensetracker.feature.ai.data.ModelDownloadManager
import com.expensetracker.feature.ai.data.ModelRegistry
import com.expensetracker.feature.ai.data.ModelStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ModelHubUiState(
    val activeModelId: String? = null,
    val models: List<AiModelCardState> = emptyList()
)

data class AiModelCardState(
    val model: AiModel,
    val state: ModelState,
    val isActive: Boolean
)

@HiltViewModel
class ModelHubViewModel @Inject constructor(
    private val preferencesStore: AiPreferencesStore,
    private val downloadManager: ModelDownloadManager,
    private val storageManager: ModelStorageManager,
    private val inferenceEngine: InferenceEngine
) : ViewModel() {

    val uiState: StateFlow<ModelHubUiState> = combine(
        preferencesStore.snapshotFlow(),
        inferenceEngine.state
    ) { snapshot, engineState ->
        val cards = ModelRegistry.models.map { model ->
            val currentState = when {
                snapshot.activeModelId == model.id && engineState is ModelState.Ready && engineState.modelId == model.id -> engineState
                storageManager.isDownloaded(model.id) && snapshot.modelStates[model.id] == ModelState.NotDownloaded -> ModelState.Downloaded
                else -> snapshot.modelStates[model.id] ?: ModelState.NotDownloaded
            }
            AiModelCardState(
                model = model,
                state = currentState,
                isActive = snapshot.activeModelId == model.id
            )
        }
        ModelHubUiState(
            activeModelId = snapshot.activeModelId,
            models = cards
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelHubUiState())

    fun startDownload(modelId: String) = viewModelScope.launch {
        runCatching {
            preferencesStore.setModelState(modelId, ModelState.Queued)
            downloadManager.enqueue(modelId)
        }.onFailure { e ->
            preferencesStore.setModelState(modelId, ModelState.Error(e.message ?: "Failed to start download"))
        }
    }

    fun cancelDownload(modelId: String) = viewModelScope.launch {
        runCatching {
            downloadManager.cancel(modelId)
            preferencesStore.setModelState(modelId, ModelState.NotDownloaded)
        }
    }

    fun useModel(modelId: String) = viewModelScope.launch {
        runCatching {
            preferencesStore.setActiveModelId(modelId)
            inferenceEngine.loadModel(modelId)
        }
    }

    fun deleteModel(modelId: String) = viewModelScope.launch {
        runCatching {
            if (uiState.value.activeModelId == modelId) {
                inferenceEngine.unloadModel()
                preferencesStore.setActiveModelId(null)
            }
            withContext(Dispatchers.IO) {
                storageManager.deleteModel(modelId)
            }
            preferencesStore.clearModelState(modelId)
        }
    }
}
