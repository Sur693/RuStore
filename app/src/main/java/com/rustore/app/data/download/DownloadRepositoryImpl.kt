package com.rustore.app.data.download

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rustore.app.data.model.DownloadStatus
import com.rustore.app.data.model.DownloadTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// Реализация репозитория загрузок — хранит задачи в памяти
class DownloadRepositoryImpl(private val prefs: SharedPreferences) : DownloadRepository {

    private val gson = Gson()

    // Все задачи: Map<taskId, DownloadTask>, StateFlow для наблюдения из ViewModel
    private val _activeTasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    override val activeTasks: StateFlow<Map<String, DownloadTask>> = _activeTasks.asStateFlow()

    // Map<appId, taskId> — быстрый поиск задачи по приложению
    private val appToTask = mutableMapOf<String, String>()

    init {
        // Восстанавливаем установленные приложения из SharedPreferences после перезапуска
        val json = prefs.getString("installed_tasks", null)
        if (json != null) {
            val type = object : TypeToken<List<DownloadTask>>() {}.type
            val tasks: List<DownloadTask> = gson.fromJson(json, type)
            _activeTasks.value = tasks.associateBy { it.taskId }
            tasks.forEach { appToTask[it.appId] = it.taskId }
        }
    }

    // Метод постановки задачи: создаёт DownloadTask и возвращает её ID
    override suspend fun startDownload(appId: String, appName: String): String {
        val existingTaskId = appToTask[appId]
        if (existingTaskId != null) {
            val existing = _activeTasks.value[existingTaskId]
            if (existing != null && existing.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)) {
                return existingTaskId
            }
            // Удаляем старую задачу (ERROR/CANCELLED) перед повтором — иначе два таска с одним appId
            _activeTasks.value = _activeTasks.value - existingTaskId
            appToTask.remove(appId)
        }
        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            taskId = taskId,
            appId = appId,
            appName = appName,
            status = DownloadStatus.QUEUED,
            progress = 0
        )
        _activeTasks.value = _activeTasks.value + (taskId to task)
        appToTask[appId] = taskId
        return taskId
    }

    // Метод получения статуса задачи по ID
    override fun getTaskStatus(taskId: String): DownloadTask? {
        return _activeTasks.value[taskId]
    }

    // Метод получения результата: путь к APK после завершения загрузки
    override fun getResult(taskId: String): String? {
        return _activeTasks.value[taskId]?.apkPath
    }

    // Метод прерывания задачи
    override suspend fun cancelDownload(taskId: String) {
        updateStatus(taskId, DownloadStatus.CANCELLED)
    }

    // StateFlow для наблюдения за задачей конкретного приложения
    override fun observeAppDownload(appId: String): StateFlow<DownloadTask?> {
        val flow = MutableStateFlow<DownloadTask?>(
            _activeTasks.value.values.firstOrNull { it.appId == appId }
        )
        return flow
    }

    // Обновление прогресса — вызывается из DownloadService каждые 250 мс
    override fun updateProgress(taskId: String, progress: Int) {
        val task = _activeTasks.value[taskId] ?: return
        _activeTasks.value = _activeTasks.value + (taskId to task.copy(
            progress = progress,
            status = DownloadStatus.DOWNLOADING
        ))
    }

    // Обновление статуса — вызывается из DownloadService и InstallReceiver
    override fun updateStatus(taskId: String, status: DownloadStatus, apkPath: String?, packageName: String?, error: String?) {
        val task = _activeTasks.value[taskId] ?: return
        _activeTasks.value = _activeTasks.value + (taskId to task.copy(
            status = status,
            apkPath = apkPath ?: task.apkPath,
            packageName = packageName ?: task.packageName,
            error = error
        ))
        if (status == DownloadStatus.INSTALLED) persistInstalledTasks()
    }

    // Удаляет задачу (например, после успешного удаления приложения)
    override fun removeTask(taskId: String) {
        val task = _activeTasks.value[taskId] ?: return
        appToTask.remove(task.appId)
        _activeTasks.value = _activeTasks.value - taskId
        persistInstalledTasks()
    }

    // Сохраняем только INSTALLED задачи — они нужны после перезапуска
    private fun persistInstalledTasks() {
        val installed = _activeTasks.value.values.filter { it.status == DownloadStatus.INSTALLED }
        prefs.edit().putString("installed_tasks", gson.toJson(installed)).apply()
    }
}
