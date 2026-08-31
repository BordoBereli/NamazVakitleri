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
 * Persists user-bookmarked verses as a JSON list of [AyahData]. Backed by the
 * same quranStore Preferences DataStore as [QuranSurahCache].
 */
@Single
class SavedVersesStore(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_verses")

    suspend fun getSavedVerses(): List<AyahData> {
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
        }
    }

    suspend fun isSaved(verse: AyahData): Boolean = getSavedVerses().any { it == verse }

    suspend fun toggle(verse: AyahData) {
        val current = getSavedVerses()
        val updated = if (current.any { it == verse }) {
            current.filterNot { it == verse }
        } else {
            current + verse
        }
        val raw = withContext(Dispatchers.Default) { json.encodeToString(updated) }
        dataStore.edit { it[key] = raw }
    }
}
