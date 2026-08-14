package com.kutluoglu.prayer_feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kutluoglu.prayer_feature.settings.calculation.CalculationMethodRoute
import com.kutluoglu.prayer_feature.settings.hijri.HijriAdjustmentRoute
import com.kutluoglu.prayer_feature.settings.language.LanguageSelectionRoute
import com.kutluoglu.prayer_feature.settings.location.LocationSelectionRoute
import com.kutluoglu.prayer_feature.settings.location.MyLocationsRoute
import com.kutluoglu.prayer_navigation.core.Screen
import com.kutluoglu.prayer.model.location.City

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable(Screen.SettingsScreen.route) {
        SettingsRoute(
            onNavigateToMyLocations = {
                navController.navigate(Screen.MyLocationsScreen.route)
            },
            onNavigateToCalculationMethod = {
                navController.navigate(Screen.CalculationMethodScreen.route)
            },
            onNavigateToHijriAdjustment = {
                navController.navigate(Screen.HijriAdjustmentScreen.route)
            },
            onNavigateToLanguage = {
                navController.navigate(Screen.LanguageSelectionScreen.route)
            }
        )
    }

    composable(Screen.MyLocationsScreen.route) {
        MyLocationsRoute(
            onNavigateBack = { navController.popBackStack() },
            onAddLocation = { navController.navigate(Screen.LocationSelectionScreen.route) }
        )
    }

    composable(Screen.LocationSelectionScreen.route) {
        LocationSelectionRoute(
            onNavigateBack = { navController.popBackStack() },
            onCitySelected = { city ->
                navController.popBackStack()
            }
        )
    }

    composable(Screen.CalculationMethodScreen.route) {
        CalculationMethodRoute(
            onNavigateBack = { navController.popBackStack() },
            onMethodSelected = { method ->
                navController.popBackStack()
            }
        )
    }

    composable(Screen.HijriAdjustmentScreen.route) {
        HijriAdjustmentRoute(
            onNavigateBack = { navController.popBackStack() },
            onAdjustmentSelected = { adjustment ->
                navController.popBackStack()
            }
        )
    }

    composable(Screen.LanguageSelectionScreen.route) {
        LanguageSelectionRoute(
            onNavigateBack = { navController.popBackStack() },
            onLanguageSelected = { language ->
                navController.popBackStack()
            }
        )
    }
}
