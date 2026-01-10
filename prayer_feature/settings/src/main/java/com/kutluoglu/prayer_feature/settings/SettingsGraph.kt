package com.kutluoglu.prayer_feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_navigation.core.Screen

/**
 * Created by F.K. on 10.01.2026.
 *
 */

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable(Screen.SettingsScreen.route) {
        SettingsRoute()
    }
}
