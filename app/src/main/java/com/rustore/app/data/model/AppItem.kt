package com.rustore.app.data.model

data class AppItem(
    val id: String,
    val name: String,
    val fullDescription: String,
    val developer: String,
    val category: AppCategory,
    val ageRating: AgeRating,
    val iconUrl: String? = null,
    val screenshotUrls: List<String> = emptyList(),
    val isPopular: Boolean = false,
    val rating: Float = 4.0f,
    val downloadCount: String = "1M+",
    val packageName: String = ""
)
