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
 * Persists user-bookmarked verses as a JSON list of [SavedVerseGroup] (surah + ordered
 * verses). Backed by the same quranStore Preferences DataStore as [QuranSurahCache].
 * Migrates the legacy flat `saved_verses` list to the nested format on first access.
 */
@Single
class SavedVersesStore(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val keyGroups = stringPreferencesKey("saved_verse_groups")
    private val keyLegacy = stringPreferencesKey("saved_verses")
    private val keyCollapsed = stringPreferencesKey("saved_verses_collapsed")

    suspend fun getSavedVerseGroups(): List<SavedVerseGroup> {
        migrateIfNeeded()
        val raw = dataStore.data.map { it[keyGroups] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<SavedVerseGroup>>(raw) }.getOrDefault(emptyList())
        }
    }

    suspend fun isSaved(verse: AyahData): Boolean =
        getSavedVerseGroups().any { group -> group.verses.any { it.sameVerse(verse) } }

    suspend fun toggle(verse: AyahData) {
        migrateIfNeeded()
        dataStore.edit { prefs ->
            val current = decodeGroups(prefs[keyGroups])
            val updated = if (current.any { group -> group.verses.any { it.sameVerse(verse) } }) {
                current.mapNotNull { group ->
                    val remaining = group.verses.filterNot { it.sameVerse(verse) }
                    if (remaining.isEmpty()) null else group.copy(verses = remaining)
                }
            } else {
                val existing = current.firstOrNull { it.surah.number == verse.surah.number }
                if (existing != null) {
                    current.map { group ->
                        if (group.surah.number == verse.surah.number) {
                            group.copy(verses = listOf(verse) + group.verses)
                        } else group
                    }
                } else {
                    current + listOf(SavedVerseGroup(surah = verse.surah, verses = listOf(verse)))
                }
            }
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(updated) }
        }
    }

    suspend fun saveGroups(groups: List<SavedVerseGroup>) {
        migrateIfNeeded()
        dataStore.edit { prefs ->
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(groups) }
        }
    }

    suspend fun getCollapsedSurahs(): Set<Int> {
        val raw = dataStore.data.map { it[keyCollapsed] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun setCollapsedSurahs(surahs: Set<Int>) {
        dataStore.edit { prefs ->
            prefs[keyCollapsed] = surahs.sorted().joinToString(",")
        }
    }

    private suspend fun migrateIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[keyGroups] != null) return@edit
            val raw = prefs[keyLegacy] ?: return@edit
            if (raw.isBlank()) return@edit
            val flat = runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
            val groups = groupBySurah(flat)
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(groups) }
            prefs.remove(keyLegacy)
        }
    }

    private fun decodeGroups(raw: String?): List<SavedVerseGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<SavedVerseGroup>>(raw) }.getOrDefault(emptyList())
    }

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
