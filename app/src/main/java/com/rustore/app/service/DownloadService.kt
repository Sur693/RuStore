package com.rustore.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.rustore.app.MainActivity
import com.rustore.app.RuStoreApplication
import com.rustore.app.data.api.dto.DownloadTaskResponse
import com.rustore.app.data.api.dto.StartDownloadRequest
import com.rustore.app.data.model.DownloadStatus
import com.rustore.app.network.NetworkConfig
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

// Foreground Service для фоновой загрузки APK через API-сервер
// Подписывается на SSE-поток событий — загрузка не прерывается при уходе с экрана
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "action_start_download"
        const val ACTION_CANCEL = "action_cancel_download"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_APP_ID = "extra_app_id"
        const val EXTRA_APP_NAME = "extra_app_name"

        fun startDownload(context: Context, taskId: String, appId: String, appName: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_APP_NAME, appName)
            }
            context.startForegroundService(intent)
        }

        fun cancelDownload(context: Context, taskId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadJobs = mutableMapOf<String, Job>()
    private val serverTaskIds = mutableMapOf<String, String>()
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
                val appId = intent.getStringExtra(EXTRA_APP_ID) ?: return START_NOT_STICKY
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: return START_NOT_STICKY
                startDownloadTask(taskId, appId, appName)
            }
            ACTION_CANCEL -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
                cancelTask(taskId)
            }
        }
        return START_NOT_STICKY
    }

    private fun downloadApk(taskId: String, url: String, client: OkHttpClient): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val dir = cacheDir.resolve("apks").also { it.mkdirs() }
            val file = dir.resolve("$taskId.apk")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (_: Exception) { null }
    }

    private fun startDownloadTask(taskId: String, appId: String, appName: String) {
        val app = application as RuStoreApplication
        val repository = app.appContainer.downloadRepository
        val apiService = app.appContainer.apiService
        val okHttpClient = app.appContainer.okHttpClient

        startForeground(NOTIFICATION_ID, buildNotification(appName, 0))

        val job = serviceScope.launch {
            try {
                // 1. Ставим задачу на сервере — получаем serverTaskId
                val response = apiService.startDownload(StartDownloadRequest(appId, appName))
                val serverTaskId = response.taskId
                serverTaskIds[taskId] = serverTaskId

                repository.updateStatus(taskId, DownloadStatus.DOWNLOADING)

                // 2. Подписываемся на SSE-поток событий от сервера
                val request = Request.Builder()
                    .url("${NetworkConfig.BASE_URL}downloads/$serverTaskId/events")
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .build()

                val httpResponse = okHttpClient.newCall(request).execute()
                val source = httpResponse.body?.source() ?: return@launch

                // 3. Читаем SSE-события пока задача активна
                while (isActive && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue

                    val data = line.removePrefix("data:").trim()
                    val taskDto = gson.fromJson(data, DownloadTaskResponse::class.java) ?: continue

                    when (taskDto.status) {
                        "DOWNLOADING" -> {
                            repository.updateProgress(taskId, taskDto.progress)
                            updateNotification(appName, taskDto.progress)
                        }
                        "COMPLETED" -> {
                            val apkUrl = taskDto.apkUrl
                            if (apkUrl == null) {
                                repository.updateStatus(taskId, DownloadStatus.ERROR, error = "Нет ссылки на APK")
                                stopTaskIfAllDone(taskId)
                                break
                            }
                            // Скачиваем APK с сервера и сохраняем локально
                            val localPath = downloadApk(taskId, apkUrl, okHttpClient)
                            if (localPath == null) {
                                repository.updateStatus(taskId, DownloadStatus.ERROR, error = "Не удалось скачать APK")
                                stopTaskIfAllDone(taskId)
                                break
                            }
                            repository.updateStatus(taskId, DownloadStatus.COMPLETED, apkPath = localPath)
                            showCompletionNotification(appName)
                            stopTaskIfAllDone(taskId)
                            break
                        }
                        "CANCELLED" -> {
                            repository.updateStatus(taskId, DownloadStatus.CANCELLED)
                            stopTaskIfAllDone(taskId)
                            break
                        }
                        "ERROR" -> {
                            repository.updateStatus(taskId, DownloadStatus.ERROR,
                                error = taskDto.error ?: "Ошибка загрузки")
                            stopTaskIfAllDone(taskId)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    repository.updateStatus(taskId, DownloadStatus.ERROR, error = e.message)
                    stopTaskIfAllDone(taskId)
                }
            }
        }

        downloadJobs[taskId] = job
    }

    private fun cancelTask(taskId: String) {
        val app = application as RuStoreApplication
        val repository = app.appContainer.downloadRepository
        val apiService = app.appContainer.apiService

        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)

        // Отменяем задачу и на сервере
        serviceScope.launch {
            try {
                serverTaskIds[taskId]?.let { apiService.cancelDownload(it) }
            } catch (_: Exception) { }
            serverTaskIds.remove(taskId)
        }

        repository.updateStatus(taskId, DownloadStatus.CANCELLED)
        stopTaskIfAllDone(taskId)
    }

    private fun stopTaskIfAllDone(taskId: String) {
        downloadJobs.remove(taskId)
        serverTaskIds.remove(taskId)
        if (downloadJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Загрузка приложений", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Прогресс загрузки APK-файлов" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(appName: String, progress: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Загрузка: $appName")
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            ).build()

    private fun updateNotification(appName: String, progress: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(appName, progress))
    }

    private fun showCompletionNotification(appName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("$appName загружен")
            .setContentText("Нажмите для установки")
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            ).build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
