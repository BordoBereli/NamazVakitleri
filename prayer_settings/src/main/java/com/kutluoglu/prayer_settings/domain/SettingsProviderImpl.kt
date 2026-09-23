package com.kutluoglu.prayer_settings.domain

import com.kutluoglu.prayer.settings.AppLocation
import com.kutluoglu.prayer.settings.AppSettings
import com.kutluoglu.prayer.settings.SettingsProvider
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * [SettingsProvider] implementation that delegates to [SettingsRepository] and
 * maps the `prayer_settings` [Settings] model to the narrow [AppSettings] port
 * model.
 */
@Single
class SettingsProviderImpl(
    private val repository: SettingsRepository
) : SettingsProvider {
    override suspend fun getSettings(): AppSettings = repository.getSettings().toAppSettings()
    override fun observeSettings(): Flow<AppSettings> = repository.observeSettings().map { it.toAppSettings() }
    override suspend fun updateLockPortrait(enabled: Boolean) = repository.updateLockPortrait(enabled)
    override suspend fun updateCompassAutoRotate(enabled: Boolean) = repository.updateCompassAutoRotate(enabled)
}

private fun Settings.toAppSettings(): AppSettings = AppSettings(
    calculationMethod = calculationMethod,
    juristicMethod = juristicMethod,
    hijriAdjustment = hijriAdjustment,
    language = language,
    lockPortrait = lockPortrait,
    compassAutoRotate = compassAutoRotate,
    location = location.toAppLocation()
)

private fun LocationSettings.toAppLocation(): AppLocation = AppLocation(
    latitude = latitude,
    longitude = longitude,
    cityName = cityName,
    district = district,
    country = country,
    timeZone = timeZone
)
