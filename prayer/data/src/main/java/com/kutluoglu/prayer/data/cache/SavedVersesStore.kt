package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
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
        println("SavedVersesStore getSavedVerses raw=$raw")
        if (raw.isNullOrBlank()) return emptyList()
        val decoded = withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
        }
        println("SavedVersesStore getSavedVerses decoded=${decoded.size}")
        return decoded
    }

    suspend fun isSaved(verse: AyahData): Boolean = getSavedVerses().any { it.sameVerse(verse) }

    suspend fun toggle(verse: AyahData): Unit {
        dataStore.edit { prefs ->
            val raw = prefs[key]
            val current = if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
            }
            val updated = if (current.any { it.sameVerse(verse) }) {
                current.filterNot { it.sameVerse(verse) }
            } else {
                listOf(verse) + current
            }
            prefs[key] = withContext(Dispatchers.Default) { json.encodeToString(updated) }
        }
    }

    suspend fun reorder(verses: List<AyahData>) {
        dataStore.edit { prefs ->
            prefs[key] = withContext(Dispatchers.Default) { json.encodeToString(verses) }
        }
    }

    /**
     * Verses are identified by their stable position (surah number + number in
     * surah), not by their translated text, so a verse saved in one language is
     * still recognized after the app language changes.
     */
    private fun AyahData.sameVerse(other: AyahData): Boolean =
        surah.number == other.surah.number && numberInSurah == other.numberInSurah
}

internal fun groupBySurah(verses: List<AyahData>): List<SavedVerseGroup> {
    val order = LinkedHashMap<Int, SavedVerseGroup>()
    verses.forEach { verse ->
        val existing = order[verse.surah.number]
        order[verse.surah.number] = if (existing == null) {
            SavedVerseGroup(surah = verse.surah, verses = listOf(verse))
        } else {
            existing.copy(verses = existing.verses + verse)
        }
    }
    return order.values.toList()
}
