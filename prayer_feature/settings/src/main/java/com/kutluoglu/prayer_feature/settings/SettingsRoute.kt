package com.kutluoglu.prayer_feature.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToJuristicMethod: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToThemeMode: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    SettingsScreen(
        onNavigateToMyLocations = onNavigateToMyLocations,
        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
        onNavigateToJuristicMethod = onNavigateToJuristicMethod,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToThemeMode = onNavigateToThemeMode,
        onNavigateToNotifications = onNavigateToNotifications
    )
}
