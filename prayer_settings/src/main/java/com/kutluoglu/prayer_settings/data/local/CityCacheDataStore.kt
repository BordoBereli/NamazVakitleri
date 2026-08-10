package com.kutluoglu.prayer_settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kutluoglu.prayer_settings.domain.model.City
import com.kutluoglu.prayer_settings.domain.model.CityList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cityCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "city_cache")

class CityCacheDataStore(
    private val context: Context
) {
    companion object {
        private val CITIES_KEY = stringPreferencesKey("cached_cities")
        private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp")
        const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    suspend fun saveCities(cities: List<City>) {
        context.cityCacheDataStore.edit { preferences ->
            val cityList = CityList(cities)
            val json = kotlinx.serialization.json.Json.encodeToString(CityList.serializer(), cityList)
            preferences[CITIES_KEY] = json
            preferences[CACHE_TIMESTAMP_KEY] = System.currentTimeMillis().toString()
        }
    }

    suspend fun getCities(): List<City> {
        return context.cityCacheDataStore.data.first().let { preferences ->
            val json = preferences[CITIES_KEY]
            if (json != null) {
                try {
                    kotlinx.serialization.json.Json.decodeFromString<CityList>(json).cities
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun getCitiesFlow(): Flow<List<City>> {
        return context.cityCacheDataStore.data.map { preferences ->
            val json = preferences[CITIES_KEY]
            if (json != null) {
                try {
                    kotlinx.serialization.json.Json.decodeFromString<CityList>(json).cities
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun isCacheValid(): Boolean {
        return context.cityCacheDataStore.data.first().let { preferences ->
            val timestamp = preferences[CACHE_TIMESTAMP_KEY]?.toLongOrNull()
            if (timestamp != null) {
                System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
            } else {
                false
            }
        }
    }

    suspend fun clearCache() {
        context.cityCacheDataStore.edit { preferences ->
            preferences.remove(CITIES_KEY)
            preferences.remove(CACHE_TIMESTAMP_KEY)
        }
    }

    suspend fun clearOldCache() {
        context.cityCacheDataStore.edit { preferences ->
            val timestamp = preferences[CACHE_TIMESTAMP_KEY]?.toLongOrNull()
            if (timestamp != null && System.currentTimeMillis() - timestamp > CACHE_DURATION_MS) {
                preferences.remove(CITIES_KEY)
                preferences.remove(CACHE_TIMESTAMP_KEY)
            }
        }
    }
}
