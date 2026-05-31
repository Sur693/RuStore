package com.rustore.app.data.model

data class CategoryInfo(
    val category: AppCategory,
    val appCount: Int,
    val iconUrl: String? = null
)
