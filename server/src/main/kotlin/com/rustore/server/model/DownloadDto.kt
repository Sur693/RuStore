package com.rustore.server.model

import kotlinx.serialization.Serializable

@Serializable
data class StartDownloadRequest(
    val appId: String,
    val appName: String
)

@Serializable
data class StartDownloadResponse(
    val taskId: String
)

// Статусы задачи — те же что и в Android DownloadStatus
@Serializable
data class DownloadTaskDto(
    val taskId: String,
    val appId: String,
    val appName: String,
    val status: String,   // QUEUED, DOWNLOADING, COMPLETED, CANCELLED, ERROR
    val progress: Int,
    val apkUrl: String? = null,
    val error: String? = null
)
