package com.rustore.server

import com.rustore.server.routes.appsRoutes
import com.rustore.server.routes.downloadRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true })
    }
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
    }
    install(SSE)
    routing {
        appsRoutes()
        downloadRoutes()
        staticResources("/apk", "apk")
        staticResources("/icons", "icons")
        staticResources("/screenshots", "screenshots")
        staticResources("/categories", "categories")
    }
}
