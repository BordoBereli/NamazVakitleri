package com.kutluoglu.prayer_feature.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    onNavigateToLocationSelection: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    SettingsScreen(
        onNavigateToLocationSelection = onNavigateToLocationSelection,
        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
        onNavigateToLanguage = onNavigateToLanguage
    )
}
