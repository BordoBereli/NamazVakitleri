package com.kutluoglu.prayer_feature.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToImsakOffset: () -> Unit,
    onNavigateToJuristicMethod: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    SettingsScreen(
        onNavigateToMyLocations = onNavigateToMyLocations,
        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
        onNavigateToImsakOffset = onNavigateToImsakOffset,
        onNavigateToJuristicMethod = onNavigateToJuristicMethod,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToNotifications = onNavigateToNotifications
    )
}
