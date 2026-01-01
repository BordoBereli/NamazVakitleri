package com.kutluoglu.prayer_feature.qibla.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_navigation.core.Screen

/**
 * Created by F.K. on 1.01.2026.
 *
 */

fun NavGraphBuilder.qiblaGraph(navController: NavController) {
    composable(Screen.QiblaScreen.route) {
        QiblaRoute()
    }
}