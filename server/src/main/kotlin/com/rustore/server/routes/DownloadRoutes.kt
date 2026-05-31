package com.rustore.server.routes

import com.rustore.server.DownloadManager
import com.rustore.server.model.StartDownloadRequest
import com.rustore.server.model.StartDownloadResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.downloadRoutes() {

    // Метод постановки задачи — возвращает taskId
    post("/downloads") {
        val request = call.receive<StartDownloadRequest>()
        val taskId = DownloadManager.startDownload(request.appId, request.appName)
        call.respond(StartDownloadResponse(taskId))
    }

    // Метод получения статуса задачи
    get("/downloads/{taskId}") {
        val taskId = call.parameters["taskId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val task = DownloadManager.getTask(taskId)
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(task)
    }

    // Метод получения результата (ссылка на APK)
    get("/downloads/{taskId}/result") {
        val taskId = call.parameters["taskId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val apkUrl = DownloadManager.getResult(taskId)
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(mapOf("apkUrl" to apkUrl))
    }

    // Метод прерывания задачи
    delete("/downloads/{taskId}") {
        val taskId = call.parameters["taskId"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest)
        DownloadManager.cancelTask(taskId)
        call.respond(HttpStatusCode.OK)
    }

    // Событие изменения статуса через SSE — клиент подписывается и получает обновления в реальном времени
    sse("/downloads/{taskId}/events") {
        val taskId = call.parameters["taskId"] ?: return@sse
        val json = Json { ignoreUnknownKeys = true }

        while (true) {
            val task = DownloadManager.getTask(taskId) ?: break
            // Отправляем текущий статус клиенту
            send(ServerSentEvent(data = json.encodeToString(task), event = "status"))

            // Завершаем поток при конечных статусах
            if (task.status in listOf("COMPLETED", "CANCELLED", "ERROR")) break
            delay(300) // обновление каждые 300 мс
        }
    }
}
