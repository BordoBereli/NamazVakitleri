package com.kutluoglu.prayer_feature.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    SettingsScreen(
        onNavigateToMyLocations = onNavigateToMyLocations,
        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
        onNavigateToLanguage = onNavigateToLanguage
    )
}
