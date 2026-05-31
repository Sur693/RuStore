package com.rustore.app.data.download

import com.rustore.app.data.model.DownloadStatus
import com.rustore.app.data.model.DownloadTask
import kotlinx.coroutines.flow.StateFlow

// Интерфейс управления задачами загрузки (постановка, статус, отмена, результат)
interface DownloadRepository {

    // Все активные задачи — Map<taskId, DownloadTask>
    val activeTasks: StateFlow<Map<String, DownloadTask>>

    // Метод постановки задачи: возвращает ID созданной задачи
    suspend fun startDownload(appId: String, appName: String): String

    // Метод получения статуса задачи по её ID
    fun getTaskStatus(taskId: String): DownloadTask?

    // Метод получения результата: возвращает путь к APK (если загружен)
    fun getResult(taskId: String): String?

    // Метод прерывания задачи
    suspend fun cancelDownload(taskId: String)

    // Наблюдение за задачей конкретного приложения
    fun observeAppDownload(appId: String): StateFlow<DownloadTask?>

    // Внутренние методы для DownloadService
    fun updateProgress(taskId: String, progress: Int)
    fun updateStatus(taskId: String, status: DownloadStatus, apkPath: String? = null, packageName: String? = null, error: String? = null)
    fun removeTask(taskId: String)
}
