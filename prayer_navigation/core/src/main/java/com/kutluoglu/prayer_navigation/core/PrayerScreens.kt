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
}
