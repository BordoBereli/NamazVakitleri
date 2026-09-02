package com.kutluoglu.prayer_settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        fun create(context: Context): SettingsDataStore {
            return SettingsDataStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    produceFile = { context.preferencesDataStoreFile("prayer_settings_store") }
                )
            )
        }
    }
    
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
        val IMSAK_OFFSET_MINUTES = intPreferencesKey("imsak_offset_minutes")
        val JURISTIC_METHOD = stringPreferencesKey("juristic_method")
        val CRASHLYTICS_ENABLED = booleanPreferencesKey("crashlytics_enabled")
    }
    
    fun observeSettings(): Flow<Settings> = dataStore.data.map { preferences ->
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
            language = preferences[PreferencesKeys.LANGUAGE] ?: "system",
            hijriAdjustment = preferences[PreferencesKeys.HIJRI_ADJUSTMENT] ?: 0,
            imsakOffsetMinutes = preferences[PreferencesKeys.IMSAK_OFFSET_MINUTES] ?: 10,
            juristicMethod = preferences[PreferencesKeys.JURISTIC_METHOD] ?: "STANDARD",
            crashlyticsEnabled = preferences[PreferencesKeys.CRASHLYTICS_ENABLED] ?: true
        )
    }
    
    suspend fun getSettings(): Settings {
        return dataStore.data.first().let { preferences ->
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
                language = preferences[PreferencesKeys.LANGUAGE] ?: "system",
                hijriAdjustment = preferences[PreferencesKeys.HIJRI_ADJUSTMENT] ?: 0,
                imsakOffsetMinutes = preferences[PreferencesKeys.IMSAK_OFFSET_MINUTES] ?: 10,
                juristicMethod = preferences[PreferencesKeys.JURISTIC_METHOD] ?: "STANDARD",
                crashlyticsEnabled = preferences[PreferencesKeys.CRASHLYTICS_ENABLED] ?: true
            )
        }
    }
    
    suspend fun updateLocation(location: LocationSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = location.latitude
            preferences[PreferencesKeys.LONGITUDE] = location.longitude
            preferences[PreferencesKeys.CITY_NAME] = location.cityName
            if (location.district != null) {
                preferences[PreferencesKeys.DISTRICT] = location.district
            } else {
                preferences.remove(PreferencesKeys.DISTRICT)
            }
            preferences[PreferencesKeys.COUNTRY] = location.country
            preferences[PreferencesKeys.TIME_ZONE] = location.timeZone
        }
    }
    
    suspend fun updateCalculationMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALCULATION_METHOD] = method
        }
    }
    
    suspend fun updateLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
    
    suspend fun updateHijriAdjustment(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIJRI_ADJUSTMENT] = days
        }
    }

    suspend fun updateImsakOffsetMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMSAK_OFFSET_MINUTES] = minutes
        }
    }

    suspend fun updateJuristicMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.JURISTIC_METHOD] = method
        }
    }

    suspend fun updateCrashlyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CRASHLYTICS_ENABLED] = enabled
        }
    }
}
