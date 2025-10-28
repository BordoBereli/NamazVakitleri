package com.kutluoglu.prayer_feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_navigation.core.Screen

/**
 * Created by F.K. on 22.10.2025.
 *
 */

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable(Screen.HomeScreen.route) {
        HomeRoute(navController = navController)
    }
}
