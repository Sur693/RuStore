package com.rustore.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class AppResponse(
    val id: String,
    val name: String,
    val fullDescription: String,
    val developer: String,
    val category: String,
    val ageRating: String,
    val iconUrl: String? = null,
    val screenshotUrls: List<String> = emptyList(),
    val isPopular: Boolean,
    val rating: Float,
    val downloadCount: String,
    val packageName: String = ""
)

data class CategoryResponse(
    val category: String,
    val displayName: String,
    val appCount: Int,
    val iconUrl: String? = null
)

data class StartDownloadRequest(
    val appId: String,
    val appName: String
)

data class StartDownloadResponse(
    val taskId: String
)

data class DownloadTaskResponse(
    val taskId: String,
    val appId: String,
    val appName: String,
    val status: String,
    val progress: Int,
    val apkUrl: String? = null,
    val error: String? = null
)
