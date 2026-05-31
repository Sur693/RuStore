package com.rustore.app.data.repository

import com.rustore.app.data.model.AppCategory
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.model.CategoryInfo

interface AppRepository {
    suspend fun getApps(category: AppCategory? = null): Result<List<AppItem>>
    suspend fun getApp(id: String): Result<AppItem>
    suspend fun searchApps(query: String): Result<List<AppItem>>
    suspend fun getPopularApps(): Result<List<AppItem>>
    suspend fun getCategories(): Result<List<CategoryInfo>>
}
