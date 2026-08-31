package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class SavedVersesStoreTest {

    private lateinit var store: SavedVersesStore
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

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
    fun `saved verses persist across store instances`() = runBlocking {
        val v = verse(2)
        store.toggle(v)

        val reloaded = SavedVersesStore(dataStore)
        assertThat(reloaded.isSaved(v)).isTrue()
    }
}
