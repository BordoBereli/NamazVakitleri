package com.kutluoglu.prayer_feature.prayertimes.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_feature.prayertimes.PayerTimesScreen
import com.kutluoglu.prayer_navigation.core.Screen

/**
 * Created by F.K. on 24.10.2025.
 *
 */

fun NavGraphBuilder.prayerTimesGraph(navController: NavController) {
    composable(Screen.PayerTimesScreen.route) {
        PayerTimesScreen()
    }
}