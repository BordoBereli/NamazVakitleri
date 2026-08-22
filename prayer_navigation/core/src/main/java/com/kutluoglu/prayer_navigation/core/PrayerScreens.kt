package com.kutluoglu.prayer_navigation.core

/**
 * Created by F.K. on 22.10.2025.
 *
 */

sealed class Screen(val route: String) {
    data object HomeScreen: Screen("home")
    data object PayerTimesScreen: Screen("prayer_times")
    data object QiblaScreen: Screen("qibla")
    data object SettingsScreen: Screen("settings")
    data object MyLocationsScreen: Screen("my_locations")
    data object LocationSelectionScreen: Screen("location_selection")
    data object CalculationMethodScreen: Screen("calculation_method")
    data object LanguageSelectionScreen: Screen("language_selection")
    data object HijriAdjustmentScreen: Screen("hijri_adjustment")
    data object NotificationsScreen: Screen("notifications")
}
