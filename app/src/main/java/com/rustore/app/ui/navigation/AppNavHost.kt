package com.rustore.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rustore.app.AppContainer
import com.rustore.app.ui.screens.appdetail.AppDetailScreen
import com.rustore.app.ui.screens.categories.CategoriesScreen
import com.rustore.app.ui.screens.onboarding.OnboardingScreen
import com.rustore.app.ui.screens.screenshots.ScreenshotViewerScreen
import com.rustore.app.ui.screens.search.SearchScreen
import com.rustore.app.ui.screens.showcase.ShowcaseScreen

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                appContainer = appContainer,
                onOnboardingComplete = {
                    navController.navigate(Screen.Showcase.createRoute()) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Showcase.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.takeIf { it.isNotEmpty() }
            ShowcaseScreen(
                appContainer = appContainer,
                initialCategory = category,
                onAppClick = { appId -> navController.navigate(Screen.AppDetail.createRoute(appId)) },
                onCategoriesClick = { navController.navigate(Screen.Categories.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                appContainer = appContainer,
                onCategoryClick = { categoryName ->
                    navController.navigate(Screen.Showcase.createRoute(categoryName)) {
                        popUpTo(Screen.Showcase.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                appContainer = appContainer,
                onAppClick = { appId -> navController.navigate(Screen.AppDetail.createRoute(appId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AppDetail.route,
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId") ?: ""
            AppDetailScreen(
                appContainer = appContainer,
                appId = appId,
                onBack = { navController.popBackStack() },
                onScreenshotClick = { index -> navController.navigate(Screen.Screenshots.createRoute(appId, index)) }
            )
        }

        composable(
            route = Screen.Screenshots.route,
            arguments = listOf(
                navArgument("appId") { type = NavType.StringType },
                navArgument("screenshotIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId") ?: ""
            val screenshotIndex = backStackEntry.arguments?.getInt("screenshotIndex") ?: 0
            ScreenshotViewerScreen(
                appContainer = appContainer,
                appId = appId,
                initialIndex = screenshotIndex,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
