package com.expensetracker.feature.ai.data

import android.content.Context
import com.expensetracker.domain.model.AiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootDir: File
        get() = File(context.filesDir, "ai-models").apply { mkdirs() }

    fun modelDirectory(modelId: String): File = File(rootDir, modelId).apply { mkdirs() }

    fun modelFile(modelId: String): File {
        val model = ModelRegistry.byId(modelId)
        val ext = model?.downloadUrl?.substringAfterLast('.', "litertlm") ?: "litertlm"
        return File(modelDirectory(modelId), "model.$ext")
    }

    fun partialModelFile(modelId: String): File = File(modelDirectory(modelId), "model.partial")

    fun tempImageFile(): File = File(context.cacheDir, "invoice-${System.currentTimeMillis()}.jpg")

    fun isDownloaded(modelId: String): Boolean = modelFile(modelId).exists()

    fun availableBytes(): Long = context.filesDir.usableSpace

    fun hasEnoughSpace(model: AiModel): Boolean = availableBytes() > model.sizeBytes + 100L * 1024L * 1024L

    fun deleteModel(modelId: String): Boolean = modelDirectory(modelId).deleteRecursively()
}
