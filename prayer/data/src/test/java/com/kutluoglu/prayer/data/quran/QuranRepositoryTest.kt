package com.kutluoglu.prayer.data.quran

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.cache.QuranSurahCache
import com.kutluoglu.prayer.data.cache.SavedVersesStore
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QuranRepositoryTest {

    private val quranDataSource: QuranDataSource = mockk()
    private val quranSurahCache: QuranSurahCache = mockk()
    private val savedVersesStore: SavedVersesStore = mockk()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Text",
        surah = SurahInfo(
            englishName = "Surah $surahNumber",
            name = "سورة",
            number = surahNumber,
            numberOfAyahs = 10
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `serves from cache when the random surah is cached`() = runTest {
        val cached = listOf(verse(1, 1), verse(1, 2))
        coEvery { quranSurahCache.getSurah(any()) } returns cached

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(cached)
        coVerify(exactly = 0) { quranDataSource.getSurah(any(), any()) }
    }

    @Test
    fun `fetches and caches the surah on a cache miss`() = runTest {
        val fetched = listOf(verse(2, 1), verse(2, 2))
        coEvery { quranSurahCache.getSurah(any()) } returns null
        coEvery { quranSurahCache.putSurah(any(), any()) } returns Unit
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(fetched)

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(fetched)
        coVerify(exactly = 1) { quranDataSource.getSurah(any(), "tr") }
        coVerify(exactly = 1) { quranSurahCache.putSurah(any(), fetched) }
    }

    @Test
    fun `propagates failure when remote fetch fails`() = runTest {
        coEvery { quranSurahCache.getSurah(any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.failure(RuntimeException("network"))

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { quranSurahCache.putSurah(any(), any()) }
    }

    @Test
    fun `returns failure when remote returns an empty surah`() = runTest {
        coEvery { quranSurahCache.getSurah(any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(emptyList())

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { quranSurahCache.putSurah(any(), any()) }
    }
}
