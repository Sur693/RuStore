package com.rustore.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Showcase : Screen("showcase?category={category}") {
        fun createRoute(category: String? = null): String =
            if (category != null) "showcase?category=$category" else "showcase?category="
    }
    data object Categories : Screen("categories")
    data object Search : Screen("search")
    data object AppDetail : Screen("app_detail/{appId}") {
        fun createRoute(appId: String): String = "app_detail/$appId"
    }
    data object Screenshots : Screen("screenshots/{appId}/{screenshotIndex}") {
        fun createRoute(appId: String, screenshotIndex: Int): String =
            "screenshots/$appId/$screenshotIndex"
    }
}
