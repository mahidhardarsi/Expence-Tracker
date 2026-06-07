package com.expensetracker.feature.ai.data

import com.expensetracker.domain.model.ChatMessage
import com.expensetracker.domain.model.ChatRole
import com.expensetracker.domain.model.ModelState
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InferenceEngine @Inject constructor(
    private val storageManager: ModelStorageManager,
    private val preferencesStore: AiPreferencesStore
) : Closeable {

    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state = _state.asStateFlow()

    private var engine: Engine? = null
    private var loadedModelId: String? = null

    suspend fun loadModel(modelId: String) = withContext(Dispatchers.IO) {
        val model = ModelRegistry.byId(modelId) ?: error("Unknown model")
        if (!storageManager.isDownloaded(modelId)) {
            _state.value = ModelState.Error("Model not downloaded")
            return@withContext
        }
        if (loadedModelId == modelId && engine != null) {
            _state.value = ModelState.Ready(modelId, model.supportsVision)
            return@withContext
        }

        _state.value = ModelState.Loading
        unloadModel()

        try {
            val modelPath = storageManager.modelFile(modelId).absolutePath
            val visionBackend = if (model.supportsVision) Backend.GPU() else null
            val (createdEngine, usedBackend) = try {
                val gpuConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU(),
                    visionBackend = visionBackend
                )
                Engine(gpuConfig).also { it.initialize() } to "GPU"
            } catch (_: Exception) {
                val cpuConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    visionBackend = visionBackend
                )
                Engine(cpuConfig).also { it.initialize() } to "CPU"
            }
            engine = createdEngine
            loadedModelId = modelId
            val readyState = ModelState.Ready(modelId, model.supportsVision, accelerator = usedBackend)
            _state.value = readyState
            preferencesStore.setModelState(modelId, readyState)
        } catch (e: Exception) {
            val errorState = ModelState.Error(e.message ?: "Failed to load model")
            _state.value = errorState
            preferencesStore.setModelState(modelId, errorState)
        }
    }

    fun unloadModel() {
        runCatching { engine?.close() }
        engine = null
        loadedModelId = null
    }

    fun streamChat(systemPrompt: String, history: List<ChatMessage>, userInput: String): Flow<String> = flow {
        val activeEngine = engine ?: error("No model loaded")
        val conversation = activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                initialMessages = history.toMessages()
            )
        )
        try {
            conversation.sendMessageAsync(userInput)
                .map { it.toChunkText() }
                .collect { emit(it) }
        } finally {
            conversation.close()
        }
    }

    fun extractInvoice(imagePath: String, prompt: String): Flow<String> = flow {
        val activeEngine = engine ?: error("No model loaded")
        val conversation = activeEngine.createConversation()
        try {
            val contents = Contents.of(
                Content.ImageFile(imagePath),
                Content.Text(prompt)
            )
            conversation.sendMessageAsync(contents)
                .map { it.toChunkText() }
                .collect { emit(it) }
        } finally {
            conversation.close()
        }
    }

    override fun close() {
        unloadModel()
    }

    private fun List<ChatMessage>.toMessages(): List<Message> = mapNotNull { message ->
        when (message.role) {
            ChatRole.SYSTEM -> null
            ChatRole.USER -> Message.user(message.content)
            ChatRole.MODEL -> Message.model(message.content)
        }
    }

    private fun Message.toChunkText(): String = toString()
}
