package com.expensetracker.feature.ai.data

import com.expensetracker.domain.model.AiModel

object ModelRegistry {
    val models = listOf(
        AiModel(
            id = "gemma-4-e2b",
            displayName = "Gemma 4 E2B",
            description = "High-performance multimodal model for invoice scanning and complex queries.",
            sizeBytes = 2_500_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            supportsVision = true,
            minRamMb = 4_000,
            recommended = true
        ),
        AiModel(
            id = "gemma-3-1b",
            displayName = "Gemma 3 1B",
            description = "Efficient text-only model for fast chat responses and lower memory usage.",
            sizeBytes = 900_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
            supportsVision = false,
            minRamMb = 2_000,
            recommended = false
        )
    )

    fun byId(modelId: String?): AiModel? = models.firstOrNull { it.id == modelId }
}
