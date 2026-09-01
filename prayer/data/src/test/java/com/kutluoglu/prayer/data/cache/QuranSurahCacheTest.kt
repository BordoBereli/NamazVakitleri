package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class QuranSurahCacheTest {

    private lateinit var cache: QuranSurahCache
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        cache = QuranSurahCache(dataStore)
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
    fun `getSurah returns null for a missing surah`() = runBlocking {
        assertThat(cache.getSurah(1, "tr")).isNull()
    }

    @Test
    fun `putSurah then getSurah returns the cached ayahs`() = runBlocking {
        val ayahs = listOf(verse(1), verse(2))

        cache.putSurah(1, "tr", ayahs)

        assertThat(cache.getSurah(1, "tr")).isEqualTo(ayahs)
    }

    @Test
    fun `getSurah returns null for corrupt json`() = runBlocking {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("quran_surah_1_tr")
        dataStore.edit { it[key] = "not-json" }

        assertThat(cache.getSurah(1, "tr")).isNull()
    }

    @Test
    fun `surahs cached under different languages are kept separate`() = runBlocking {
        val tr = listOf(verse(1))
        val en = listOf(verse(1).copy(text = "English text"))

        cache.putSurah(1, "tr", tr)
        cache.putSurah(1, "en", en)

        assertThat(cache.getSurah(1, "tr")).isEqualTo(tr)
        assertThat(cache.getSurah(1, "en")).isEqualTo(en)
    }
}
