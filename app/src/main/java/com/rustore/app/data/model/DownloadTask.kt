package com.rustore.app.data.model

// Задача загрузки: создаётся при старте, отслеживается по taskId
data class DownloadTask(
    val taskId: String,
    val appId: String,
    val appName: String,
    val status: DownloadStatus,
    val progress: Int = 0,      // прогресс 0–100
    val apkPath: String? = null, // путь к файлу после загрузки
    val packageName: String = "", // packageName установленного приложения
    val error: String? = null
)
