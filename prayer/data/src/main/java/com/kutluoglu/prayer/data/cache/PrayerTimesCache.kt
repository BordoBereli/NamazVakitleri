package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
        return runCatching {
            json.decodeFromString<List<CachedPrayer>>(raw).map { it.toPrayer() }
        }.getOrNull()
    }

    suspend fun put(cacheKey: String, prayers: List<Prayer>) {
        val key = stringPreferencesKey(cacheKey)
        val raw = json.encodeToString(prayers.map { it.toCached() })
        dataStore.edit { it[key] = raw }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
