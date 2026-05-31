package com.rustore.app.data.api

import com.rustore.app.data.api.dto.AppResponse
import com.rustore.app.data.model.AgeRating
import com.rustore.app.data.model.AppCategory
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.model.CategoryInfo
import com.rustore.app.data.repository.AppRepository

// Реализация репозитория через API-сервер
class ApiAppRepository(private val apiService: ApiService) : AppRepository {

    override suspend fun getApps(category: AppCategory?): Result<List<AppItem>> = runCatching {
        apiService.getApps(category?.name).map { it.toAppItem() }
    }

    override suspend fun getApp(id: String): Result<AppItem> = runCatching {
        apiService.getApp(id).toAppItem()
    }

    override suspend fun searchApps(query: String): Result<List<AppItem>> = runCatching {
        apiService.searchApps(query).map { it.toAppItem() }
    }

    override suspend fun getPopularApps(): Result<List<AppItem>> = runCatching {
        apiService.getPopularApps().map { it.toAppItem() }
    }

    override suspend fun getCategories(): Result<List<CategoryInfo>> = runCatching {
        apiService.getCategories().map { dto ->
            CategoryInfo(
                category = AppCategory.valueOf(dto.category),
                appCount = dto.appCount,
                iconUrl = dto.iconUrl
            )
        }
    }
}

// Конвертация DTO сервера в модель приложения
private fun AppResponse.toAppItem() = AppItem(
    id = id,
    name = name,
    fullDescription = fullDescription,
    developer = developer,
    category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.TOOLS),
    ageRating = runCatching { AgeRating.valueOf(ageRating) }.getOrDefault(AgeRating.AGE_0),
    iconUrl = iconUrl,
    screenshotUrls = screenshotUrls,
    isPopular = isPopular,
    rating = rating,
    downloadCount = downloadCount,
    packageName = packageName
)
