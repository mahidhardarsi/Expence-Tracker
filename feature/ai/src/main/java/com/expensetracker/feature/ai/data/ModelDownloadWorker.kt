package com.expensetracker.feature.ai.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.expensetracker.domain.model.ModelState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.Request
import java.io.FileOutputStream

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val storageManager: ModelStorageManager,
    private val preferencesStore: AiPreferencesStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val model = ModelRegistry.byId(modelId) ?: return Result.failure()
        val partialFile = storageManager.partialModelFile(modelId)
        val existingLength = partialFile.takeIf { it.exists() }?.length() ?: 0L

        preferencesStore.setModelState(modelId, ModelState.Queued)
        setForeground(createForegroundInfo(model.displayName, 0))

        val request = Request.Builder()
            .url(model.downloadUrl)
            .apply {
                if (existingLength > 0) {
                    header("Range", "bytes=$existingLength-")
                }
            }
            .build()

        return runCatching {
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed (${response.code})")
                }

                val body = response.body ?: throw IllegalStateException("Empty download body")
                val totalBytes = existingLength + body.contentLength().coerceAtLeast(0L)
                partialFile.parentFile?.mkdirs()
                FileOutputStream(partialFile, existingLength > 0).use { output ->
                    val source = body.byteStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = existingLength
                    while (true) {
                        val read = source.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                        preferencesStore.setModelState(modelId, ModelState.Downloading(progress, downloaded, totalBytes))
                        setForeground(createForegroundInfo(model.displayName, progress))
                    }
                }

                partialFile.copyTo(storageManager.modelFile(modelId), overwrite = true)
                partialFile.delete()
                preferencesStore.setModelState(modelId, ModelState.Downloaded)
                Result.success()
            }
        }.getOrElse {
            preferencesStore.setModelState(modelId, ModelState.Error(it.message ?: "Download failed"))
            Result.failure()
        }
    }

    private fun createForegroundInfo(modelName: String, progress: Int): ForegroundInfo {
        ensureChannel()
        return ForegroundInfo(
            NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Downloading $modelName")
                .setContentText("$progress% complete")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
                .build(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "AI Model Downloads", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        private const val CHANNEL_ID = "ai_model_downloads"
        private const val NOTIFICATION_ID = 1201
    }
}
