package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_feature.home.HomeRoute
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_navigation.core.Screen
import kotlinx.datetime.format.Padding

/**
 * Created by F.K. on 22.10.2025.
 *
 */

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable(Screen.HomeScreen.route) {
        HomeRoute(navController = navController)
    }
}
