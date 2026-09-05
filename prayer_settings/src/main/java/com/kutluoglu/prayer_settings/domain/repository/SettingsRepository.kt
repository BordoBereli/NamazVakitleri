package com.kutluoglu.prayer_settings.domain.repository

import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<Settings>
    suspend fun getSettings(): Settings
    suspend fun updateLocation(location: LocationSettings)
    suspend fun updateCalculationMethod(method: String)
    suspend fun updateLanguage(language: String)
    suspend fun updateHijriAdjustment(days: Int)
    suspend fun updateCrashlyticsEnabled(enabled: Boolean)
    suspend fun updateLockPortrait(lockPortrait: Boolean)
    suspend fun updateCompassAutoRotate(compassAutoRotate: Boolean)
}
