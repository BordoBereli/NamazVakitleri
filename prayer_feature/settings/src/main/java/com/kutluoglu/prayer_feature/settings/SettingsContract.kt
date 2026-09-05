package com.kutluoglu.prayer_feature.settings

import com.kutluoglu.core.common.AppVersion
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(
        val settings: Settings,
        val version: AppVersion
    ) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

sealed class SettingsEvent {
    data object LoadSettings : SettingsEvent()
    data class UpdateLocation(val location: LocationSettings) : SettingsEvent()
    data class UpdateCalculationMethod(val method: String) : SettingsEvent()
    data class UpdateLanguage(val language: String) : SettingsEvent()
    data class UpdateHijriAdjustment(val days: Int) : SettingsEvent()
    data class UpdateLockPortrait(val lockPortrait: Boolean) : SettingsEvent()
    data class UpdateCompassAutoRotate(val compassAutoRotate: Boolean) : SettingsEvent()
    data object ClearCache : SettingsEvent()
}
