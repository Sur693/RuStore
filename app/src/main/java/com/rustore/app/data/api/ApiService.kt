package com.rustore.app.data.api

import com.rustore.app.data.api.dto.*
import retrofit2.http.*

interface ApiService {

    @GET("apps")
    suspend fun getApps(@Query("category") category: String? = null): List<AppResponse>

    @GET("apps/popular")
    suspend fun getPopularApps(): List<AppResponse>

    @GET("apps/search")
    suspend fun searchApps(@Query("q") query: String): List<AppResponse>

    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>

    @GET("apps/{id}")
    suspend fun getApp(@Path("id") id: String): AppResponse

    @POST("downloads")
    suspend fun startDownload(@Body request: StartDownloadRequest): StartDownloadResponse

    @GET("downloads/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): DownloadTaskResponse

    @DELETE("downloads/{taskId}")
    suspend fun cancelDownload(@Path("taskId") taskId: String)
}
