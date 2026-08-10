package com.kutluoglu.prayer_settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "prayer_settings_store")

@Single
class SettingsDataStore(private val context: Context) {
    
    private object PreferencesKeys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val DISTRICT = stringPreferencesKey("district")
        val COUNTRY = stringPreferencesKey("country")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val LANGUAGE = stringPreferencesKey("language")
        val HIJRI_ADJUSTMENT = intPreferencesKey("hijri_adjustment")
    }
    
    fun observeSettings(): Flow<Settings> = context.settingsDataStore.data.map { preferences ->
        Settings(
            location = LocationSettings(
                latitude = preferences[PreferencesKeys.LATITUDE] ?: 41.0082,
                longitude = preferences[PreferencesKeys.LONGITUDE] ?: 28.9784,
                cityName = preferences[PreferencesKeys.CITY_NAME] ?: "Istanbul",
                district = preferences[PreferencesKeys.DISTRICT],
                country = preferences[PreferencesKeys.COUNTRY] ?: "Turkey",
                timeZone = preferences[PreferencesKeys.TIME_ZONE] ?: "Europe/Istanbul"
            ),
            calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD] ?: "TURKEY_DIYANET",
            language = preferences[PreferencesKeys.LANGUAGE] ?: "tr",
            hijriAdjustment = preferences[PreferencesKeys.HIJRI_ADJUSTMENT] ?: 0
        )
    }
    
    suspend fun getSettings(): Settings {
        var settings = Settings()
        context.settingsDataStore.data.collect { preferences ->
            settings = Settings(
                location = LocationSettings(
                    latitude = preferences[PreferencesKeys.LATITUDE] ?: 41.0082,
                    longitude = preferences[PreferencesKeys.LONGITUDE] ?: 28.9784,
                    cityName = preferences[PreferencesKeys.CITY_NAME] ?: "Istanbul",
                    district = preferences[PreferencesKeys.DISTRICT],
                    country = preferences[PreferencesKeys.COUNTRY] ?: "Turkey",
                    timeZone = preferences[PreferencesKeys.TIME_ZONE] ?: "Europe/Istanbul"
                ),
                calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD] ?: "TURKEY_DIYANET",
                language = preferences[PreferencesKeys.LANGUAGE] ?: "tr",
                hijriAdjustment = preferences[PreferencesKeys.HIJRI_ADJUSTMENT] ?: 0
            )
        }
        return settings
    }
    
    suspend fun updateLocation(location: LocationSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = location.latitude
            preferences[PreferencesKeys.LONGITUDE] = location.longitude
            preferences[PreferencesKeys.CITY_NAME] = location.cityName
            location.district?.let { preferences[PreferencesKeys.DISTRICT] = it }
            preferences[PreferencesKeys.COUNTRY] = location.country
            preferences[PreferencesKeys.TIME_ZONE] = location.timeZone
        }
    }
    
    suspend fun updateCalculationMethod(method: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CALCULATION_METHOD] = method
        }
    }
    
    suspend fun updateLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
    
    suspend fun updateHijriAdjustment(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HIJRI_ADJUSTMENT] = days
        }
    }
}
