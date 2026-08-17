package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * Persists computed prayer times keyed by day/location so they can be reused
 * without recalculating. Backed by Preferences DataStore.
 */
@Single
class PrayerTimesCache(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(cacheKey: String): List<Prayer>? {
        val key = stringPreferencesKey(cacheKey)
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return null
        return withContext(Dispatchers.Default) {
            runCatching {
                json.decodeFromString<List<CachedPrayer>>(raw).map { it.toPrayer() }
            }.getOrNull()
        }
    }

    suspend fun put(cacheKey: String, prayers: List<Prayer>) {
        val key = stringPreferencesKey(cacheKey)
        val raw = withContext(Dispatchers.Default) {
            json.encodeToString(prayers.map { it.toCached() })
        }
        dataStore.edit { it[key] = raw }
    }

    suspend fun getMonth(cacheKey: String): List<DailyPrayer>? {
        val key = stringPreferencesKey(cacheKey)
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return null
        return withContext(Dispatchers.Default) {
            runCatching {
                json.decodeFromString<List<CachedDailyPrayer>>(raw).map { it.toDailyPrayer() }
            }.getOrNull()
        }
    }

    suspend fun putMonth(cacheKey: String, prayers: List<DailyPrayer>) {
        val key = stringPreferencesKey(cacheKey)
        val raw = withContext(Dispatchers.Default) {
            json.encodeToString(prayers.map { it.toCached() })
        }
        dataStore.edit { it[key] = raw }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
