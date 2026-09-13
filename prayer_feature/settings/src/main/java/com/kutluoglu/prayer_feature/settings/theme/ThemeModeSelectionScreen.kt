package com.kutluoglu.prayer_feature.settings.theme

import com.kutluoglu.prayer_feature.settings.R as SettingsR

data class ThemeMode(
    val id: String,
    val nameRes: Int
)

val themeModes = listOf(
    ThemeMode("dark", SettingsR.string.theme_dark),
    ThemeMode("light", SettingsR.string.theme_light),
    ThemeMode("system", SettingsR.string.theme_system)
)

sealed class ThemeModeUiState {
    data object Loading : ThemeModeUiState()
    data class ThemeModesLoaded(
        val themeModes: List<ThemeMode>,
        val selectedThemeMode: String
    ) : ThemeModeUiState()
    data class Error(val message: String) : ThemeModeUiState()
}

sealed class ThemeModeEvent {
    data class SelectThemeMode(val themeMode: ThemeMode) : ThemeModeEvent()
}
