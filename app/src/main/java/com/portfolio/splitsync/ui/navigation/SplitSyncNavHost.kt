package com.portfolio.splitsync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.portfolio.splitsync.ui.screens.auth.WelcomeScreen
import com.portfolio.splitsync.ui.screens.group.GroupDetailRoute
import com.portfolio.splitsync.ui.screens.home.HomeScreen

@Composable
fun SplitSyncNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome",
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        }
    ) {
        composable("welcome") {
            WelcomeScreen(
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onGroupClick = { groupId ->
                    navController.navigate("group_detail/$groupId")
                }, onLogout = {
                    navController.navigate("welcome") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "group_detail/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            GroupDetailRoute(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}