package com.rustore.server.routes

import com.rustore.server.model.appDatabase
import com.rustore.server.model.getCategories
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.appsRoutes() {

    // Список приложений с опциональной фильтрацией по категории
    get("/apps") {
        val category = call.request.queryParameters["category"]
        val apps = if (category != null) appDatabase.filter { it.category == category } else appDatabase
        call.respond(apps)
    }

    // Популярные приложения
    get("/apps/popular") {
        call.respond(appDatabase.filter { it.isPopular })
    }

    // Поиск по названию, описанию, разработчику
    get("/apps/search") {
        val query = call.request.queryParameters["q"]?.lowercase() ?: ""
        if (query.isBlank()) {
            call.respond(emptyList<Any>())
            return@get
        }
        val results = appDatabase.filter {
            it.name.lowercase().contains(query)
        }
        call.respond(results)
    }

    // Список категорий с количеством приложений
    get("/categories") {
        call.respond(getCategories())
    }

    // Одно приложение по ID
    get("/apps/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val app = appDatabase.find { it.id == id }
            ?: return@get call.respond(HttpStatusCode.NotFound, "Приложение не найдено")
        call.respond(app)
    }
}
