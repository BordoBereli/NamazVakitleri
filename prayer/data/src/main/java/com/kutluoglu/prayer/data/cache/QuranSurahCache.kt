package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.quran.AyahData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Persists fetched surahs (as lists of ayahs) keyed by surah number so random
 * verses can be served without a network call. Backed by Preferences DataStore.
 */
@Single
class QuranSurahCache(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getSurah(surahNumber: Int): List<AyahData>? {
        val key = stringPreferencesKey("quran_surah_$surahNumber")
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return null
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrNull()
        }
    }

    suspend fun putSurah(surahNumber: Int, ayahs: List<AyahData>) {
        val key = stringPreferencesKey("quran_surah_$surahNumber")
        val raw = withContext(Dispatchers.Default) { json.encodeToString(ayahs) }
        dataStore.edit { it[key] = raw }
    }
}
