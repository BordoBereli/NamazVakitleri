package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class SavedVersesStoreTest {

    private lateinit var store: SavedVersesStore
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        store = SavedVersesStore(dataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Faatiha",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `isSaved is false for a verse never saved`() = runBlocking {
        assertThat(store.isSaved(verse(1))).isFalse()
    }

    @Test
    fun `toggle adds then removes a verse`() = runBlocking {
        val v = verse(1)

        store.toggle(v)
        assertThat(store.isSaved(v)).isTrue()

        store.toggle(v)
        assertThat(store.isSaved(v)).isFalse()
    }

    @Test
    fun `verse is recognized across languages by its position`() = runBlocking {
        val savedInTurkish = verse(1).copy(text = "Türkçe metin")
        store.toggle(savedInTurkish)

        val reFetchedInEnglish = verse(1).copy(text = "English text")
        assertThat(store.isSaved(reFetchedInEnglish)).isTrue()

        store.toggle(reFetchedInEnglish)
        assertThat(store.isSaved(reFetchedInEnglish)).isFalse()
    }

    @Test
    fun `saved verses persist across store instances`() = runBlocking {
        val v = verse(2)
        store.toggle(v)

        val reloaded = SavedVersesStore(dataStore)
        assertThat(reloaded.isSaved(v)).isTrue()
    }

    @Test
    fun `migrates legacy flat list to nested groups`() = runBlocking {
        val legacy = listOf(
            AyahData("a", SurahInfo("Ya-Sin", "يس", 36, 83), 2),
            AyahData("b", SurahInfo("Al-Fatihah", "الفاتحة", 1, 7), 1),
            AyahData("c", SurahInfo("Ya-Sin", "يس", 36, 83), 1),
        )
        dataStore.edit { it[stringPreferencesKey("saved_verses")] = json.encodeToString(legacy) }

        val groups = store.getSavedVerseGroups()

        assertThat(groups.map { it.surah.number }).containsExactly(36, 1).inOrder()
        assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2, 1).inOrder()
        assertThat(groups[1].verses.map { it.numberInSurah }).containsExactly(1).inOrder()
    }

    @Test
    fun `toggle adds a verse to an existing group and removes it`() = runBlocking {
        store.toggle(verse(1))
        store.toggle(verse(2))

        var groups = store.getSavedVerseGroups()
        assertThat(groups).hasSize(1)
        assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2, 1).inOrder()

        store.toggle(verse(1))
        groups = store.getSavedVerseGroups()
        assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2).inOrder()
    }

    @Test
    fun `toggle creates a new group when the surah is new`() = runBlocking {
        store.toggle(verse(1))
        val otherSurah = AyahData(
            text = "T",
            surah = SurahInfo("Ya-Sin", "يس", 36, 83),
            numberInSurah = 1
        )
        store.toggle(otherSurah)

        val groups = store.getSavedVerseGroups()
        assertThat(groups.map { it.surah.number }).containsExactly(1, 36).inOrder()
    }

    @Test
    fun `removing the last verse drops the group`() = runBlocking {
        store.toggle(verse(1))
        store.toggle(verse(1))

        assertThat(store.getSavedVerseGroups()).isEmpty()
    }

    @Test
    fun `saveGroups persists the nested order`() = runBlocking {
        store.toggle(verse(1))
        store.toggle(verse(2))
        val groups = store.getSavedVerseGroups()
        val reversed = groups.map { it.copy(verses = it.verses.reversed()) }

        store.saveGroups(reversed)

        assertThat(store.getSavedVerseGroups()[0].verses.map { it.numberInSurah })
            .containsExactly(1, 2).inOrder()
    }

    @Test
    fun `collapse state persists across store instances`() = runBlocking {
        store.setCollapsedSurahs(setOf(1, 36))

        val reloaded = SavedVersesStore(dataStore)
        assertThat(reloaded.getCollapsedSurahs()).containsExactly(1, 36)
    }

    @Test
    fun `groupBySurah groups flat verses by surah preserving order`() = runBlocking {
        val surah1 = SurahInfo("Al-Fatihah", "الفاتحة", 1, 7)
        val surah36 = SurahInfo("Ya-Sin", "يس", 36, 83)
        val flat = listOf(
            AyahData("a", surah36, 1),
            AyahData("b", surah1, 1),
            AyahData("c", surah1, 2),
            AyahData("d", surah36, 2),
        )

        val groups = groupBySurah(flat)

        assertThat(groups.map { it.surah.number }).containsExactly(36, 1).inOrder()
        assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(1, 2).inOrder()
        assertThat(groups[1].verses.map { it.numberInSurah }).containsExactly(1, 2).inOrder()
    }
}
