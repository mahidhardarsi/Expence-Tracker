package com.expensetracker.feature.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.domain.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

data class AiPreferencesSnapshot(
    val activeModelId: String? = null,
    val modelStates: Map<String, ModelState> = emptyMap()
)

@Singleton
class AiPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeModelIdKey = stringPreferencesKey("active_model_id")

    fun snapshotFlow(): Flow<AiPreferencesSnapshot> = context.aiPreferencesDataStore.data.map { prefs ->
        val states = ModelRegistry.models.associate { model ->
            model.id to decodeState(
                prefs[stringPreferencesKey("model_state_${model.id}")],
                prefs[longPreferencesKey("downloaded_bytes_${model.id}")] ?: 0L,
                model.sizeBytes
            )
        }
        AiPreferencesSnapshot(
            activeModelId = prefs[activeModelIdKey],
            modelStates = states
        )
    }

    suspend fun setActiveModelId(modelId: String?) {
        context.aiPreferencesDataStore.edit { prefs ->
            if (modelId == null) prefs.remove(activeModelIdKey) else prefs[activeModelIdKey] = modelId
        }
    }

    suspend fun setModelState(modelId: String, state: ModelState) {
        context.aiPreferencesDataStore.edit { prefs ->
            prefs[stringPreferencesKey("model_state_$modelId")] = encodeState(state)
            if (state is ModelState.Downloading) {
                prefs[longPreferencesKey("downloaded_bytes_$modelId")] = state.downloadedBytes
            } else if (state is ModelState.Downloaded || state is ModelState.Ready) {
                prefs[longPreferencesKey("downloaded_bytes_$modelId")] = ModelRegistry.byId(modelId)?.sizeBytes ?: 0L
            }
        }
    }

    suspend fun clearModelState(modelId: String) {
        context.aiPreferencesDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("model_state_$modelId"))
            prefs.remove(longPreferencesKey("downloaded_bytes_$modelId"))
            if (prefs[activeModelIdKey] == modelId) {
                prefs.remove(activeModelIdKey)
            }
        }
    }

    private fun encodeState(state: ModelState): String = when (state) {
        ModelState.NotDownloaded -> "not_downloaded"
        ModelState.Queued -> "queued"
        is ModelState.Downloading -> "downloading:${state.progress}"
        ModelState.Downloaded -> "downloaded"
        ModelState.Loading -> "loading"
        is ModelState.Ready -> "ready:${state.modelId}:${state.supportsVision}:${state.accelerator}"
        is ModelState.Error -> "error:${state.message}"
    }

    private fun decodeState(raw: String?, downloadedBytes: Long, totalBytes: Long): ModelState {
        if (raw.isNullOrBlank()) return ModelState.NotDownloaded
        return when {
            raw == "not_downloaded" -> ModelState.NotDownloaded
            raw == "queued" -> ModelState.Queued
            raw == "downloaded" -> ModelState.Downloaded
            raw == "loading" -> ModelState.Loading
            raw.startsWith("downloading:") -> {
                val progress = raw.substringAfter(':').toIntOrNull() ?: 0
                ModelState.Downloading(progress, downloadedBytes, totalBytes)
            }
            raw.startsWith("ready:") -> {
                val pieces = raw.split(":")
                ModelState.Ready(
                    modelId = pieces.getOrNull(1).orEmpty(),
                    supportsVision = pieces.getOrNull(2)?.toBoolean() ?: false,
                    accelerator = pieces.getOrNull(3) ?: "CPU"
                )
            }
            raw.startsWith("error:") -> ModelState.Error(raw.substringAfter("error:"))
            else -> ModelState.NotDownloaded
        }
    }
}
