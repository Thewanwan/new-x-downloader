package com.twitter.downloader.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.twitter.downloader.TwitterDownloaderApp
import com.twitter.downloader.ui.screens.home.HomeScreen
import com.twitter.downloader.ui.screens.history.DownloadHistoryScreen
import com.twitter.downloader.ui.screens.logs.LogScreen
import com.twitter.downloader.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Logs : Screen("logs")
    data object DownloadHistory : Screen("download_history/{userId}/{userName}") {
        fun createRoute(userId: Long, userName: String): String {
            return "download_history/$userId/$userName"
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                },
                onNavigateToHistory = { userId, userName ->
                    navController.navigate(Screen.DownloadHistory.createRoute(userId, userName))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Logs.route) {
            LogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DownloadHistory.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as TwitterDownloaderApp
            val downloadDao = application.database.downloadDao()

            DownloadHistoryScreen(
                userId = userId,
                userName = userName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                downloadDao = downloadDao
            )
        }
    }
}
