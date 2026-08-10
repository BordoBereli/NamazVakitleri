package com.kutluoglu.prayer_cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kutluoglu.prayer.data.settings.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_data_store")

@Single
class SettingsDataStoreImp(
    private val context: Context
) : SettingsDataStore {

    companion object {
        private val KEY_CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_HIJRI_ADJUSTMENT = stringPreferencesKey("hijri_adjustment")
        private val KEY_LATITUDE = doublePreferencesKey("location_latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("location_longitude")
        private val KEY_CITY_NAME = stringPreferencesKey("location_city_name")
        private val KEY_COUNTRY = stringPreferencesKey("location_country")
        private val KEY_TIMEZONE = stringPreferencesKey("location_timezone")

        private const val DEFAULT_CALCULATION_METHOD = "TURKEY_DIYANET"
        private const val DEFAULT_LANGUAGE = "tr"
        private const val DEFAULT_HIJRI_ADJUSTMENT = "0"
        private const val DEFAULT_LATITUDE = 41.0082
        private const val DEFAULT_LONGITUDE = 28.9784
        private const val DEFAULT_CITY_NAME = "Istanbul"
        private const val DEFAULT_COUNTRY = "Turkey"
        private const val DEFAULT_TIMEZONE = "Europe/Istanbul"
    }

    override fun observeSettings(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            buildSettingsJson(preferences)
        }
    }

    override suspend fun getSettings(): String {
        return context.settingsDataStore.data.map { preferences ->
            buildSettingsJson(preferences)
        }.firstOrNull() ?: buildDefaultSettingsJson()
    }

    private fun buildSettingsJson(preferences: Preferences): String {
        val calculationMethod = preferences[KEY_CALCULATION_METHOD] ?: DEFAULT_CALCULATION_METHOD
        val language = preferences[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
        val hijriAdjustment = preferences[KEY_HIJRI_ADJUSTMENT] ?: DEFAULT_HIJRI_ADJUSTMENT
        val latitude = preferences[KEY_LATITUDE] ?: DEFAULT_LATITUDE
        val longitude = preferences[KEY_LONGITUDE] ?: DEFAULT_LONGITUDE
        val cityName = preferences[KEY_CITY_NAME] ?: DEFAULT_CITY_NAME
        val country = preferences[KEY_COUNTRY] ?: DEFAULT_COUNTRY
        val timeZone = preferences[KEY_TIMEZONE] ?: DEFAULT_TIMEZONE

        return """
            {
                "calculationMethod": "$calculationMethod",
                "language": "$language",
                "hijriAdjustment": $hijriAdjustment,
                "location": {
                    "latitude": $latitude,
                    "longitude": $longitude,
                    "cityName": "$cityName",
                    "country": "$country",
                    "timeZone": "$timeZone"
                }
            }
        """.trimIndent()
    }

    private fun buildDefaultSettingsJson(): String {
        return """
            {
                "calculationMethod": "$DEFAULT_CALCULATION_METHOD",
                "language": "$DEFAULT_LANGUAGE",
                "hijriAdjustment": 0,
                "location": {
                    "latitude": $DEFAULT_LATITUDE,
                    "longitude": $DEFAULT_LONGITUDE,
                    "cityName": "$DEFAULT_CITY_NAME",
                    "country": "$DEFAULT_COUNTRY",
                    "timeZone": "$DEFAULT_TIMEZONE"
                }
            }
        """.trimIndent()
    }

    override suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        country: String,
        timeZone: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LATITUDE] = latitude
            preferences[KEY_LONGITUDE] = longitude
            preferences[KEY_CITY_NAME] = cityName
            preferences[KEY_COUNTRY] = country
            preferences[KEY_TIMEZONE] = timeZone
        }
    }

    override suspend fun saveCalculationMethod(method: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_CALCULATION_METHOD] = method
        }
    }

    override suspend fun saveLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    override suspend fun saveHijriAdjustment(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_HIJRI_ADJUSTMENT] = days.toString()
        }
    }
}
