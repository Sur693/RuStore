package com.rustore.server

import com.rustore.server.model.DownloadTaskDto
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


// Менеджер задач загрузки — хранит состояние в памяти, симулирует прогресс
object DownloadManager {

    private val tasks = ConcurrentHashMap<String, DownloadTaskDto>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val BASE_URL = System.getenv("BASE_URL") ?: "http://10.0.2.2:8080"

    // Метод постановки задачи — возвращает taskId
    fun startDownload(appId: String, appName: String): String {
        val taskId = UUID.randomUUID().toString()
        tasks[taskId] = DownloadTaskDto(taskId, appId, appName, "QUEUED", 0)

        val job = scope.launch {
            delay(300)
            tasks[taskId] = tasks[taskId]!!.copy(status = "DOWNLOADING")

            // Симуляция загрузки
            var progress = 0
            while (isActive && progress <= 100) {
                tasks[taskId] = tasks[taskId]!!.copy(progress = progress, status = "DOWNLOADING")
                delay(250)
                progress += 5
            }

            if (isActive) {
                // Загрузка завершена — возвращаем ссылку на APK
                tasks[taskId] = tasks[taskId]!!.copy(
                    status = "COMPLETED",
                    progress = 100,
                    apkUrl = "$BASE_URL/apk/test.apk"
                )
            }
            jobs.remove(taskId)
        }

        jobs[taskId] = job
        return taskId
    }

    // Метод получения статуса задачи
    fun getTask(taskId: String): DownloadTaskDto? = tasks[taskId]

    // Метод получения результата — возвращает ссылку на APK
    fun getResult(taskId: String): String? = tasks[taskId]?.apkUrl

    // Метод прерывания задачи
    fun cancelTask(taskId: String) {
        jobs[taskId]?.cancel()
        jobs.remove(taskId)
        tasks[taskId] = tasks[taskId]?.copy(status = "CANCELLED") ?: return
    }

    fun getAllTasks(): List<DownloadTaskDto> = tasks.values.toList()
}
