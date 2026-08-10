package com.kutluoglu.prayer_settings.data.repository

import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore,
    private val locationDataStore: LocationDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<Settings> = settingsDataStore.observeSettings()
    
    override suspend fun getSettings(): Settings {
        return settingsDataStore.observeSettings().first()
    }
    
    override suspend fun updateLocation(location: LocationSettings) {
        settingsDataStore.updateLocation(location)
        
        val locationDataModel = LocationDataModel(
            latitude = location.latitude,
            longitude = location.longitude,
            country = location.country,
            countryCode = getCountryCode(location.timeZone),
            city = location.cityName,
            county = location.district
        )
        locationDataStore.saveLocation(locationDataModel)
    }
    
    private fun getCountryCode(timeZone: String): String? {
        return when {
            timeZone.contains("Istanbul", ignoreCase = true) ||
            timeZone.contains("Europe/Istanbul", ignoreCase = true) -> "TR"
            timeZone.contains("Europe/Berlin", ignoreCase = true) -> "DE"
            timeZone.contains("Europe/London", ignoreCase = true) -> "GB"
            timeZone.contains("Europe/Paris", ignoreCase = true) -> "FR"
            timeZone.contains("Asia/Jakarta", ignoreCase = true) -> "ID"
            timeZone.contains("Asia/Riyadh", ignoreCase = true) -> "SA"
            else -> null
        }
    }
    
    override suspend fun updateCalculationMethod(method: String) {
        settingsDataStore.updateCalculationMethod(method)
    }
    
    override suspend fun updateLanguage(language: String) {
        settingsDataStore.updateLanguage(language)
    }
    
    override suspend fun updateHijriAdjustment(days: Int) {
        settingsDataStore.updateHijriAdjustment(days)
    }
}
