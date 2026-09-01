package com.kutluoglu.prayer.data.quran

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.cache.QuranSurahCache
import com.kutluoglu.prayer.data.cache.SavedVersesStore
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
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
        coEvery { quranSurahCache.getSurah(any(), any()) } returns cached

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(cached)
        coVerify(exactly = 0) { quranDataSource.getSurah(any(), any()) }
    }

    @Test
    fun `fetches and caches the surah on a cache miss`() = runTest {
        val fetched = listOf(verse(2, 1), verse(2, 2))
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranSurahCache.putSurah(any(), any(), any()) } returns Unit
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(fetched)

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(fetched)
        coVerify(exactly = 1) { quranDataSource.getSurah(any(), "tr") }
        coVerify(exactly = 1) { quranSurahCache.putSurah(any(), "tr", fetched) }
    }

    @Test
    fun `propagates failure when remote fetch fails`() = runTest {
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.failure(RuntimeException("network"))

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { quranSurahCache.putSurah(any(), any(), any()) }
    }

    @Test
    fun `returns failure when remote returns an empty surah`() = runTest {
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(emptyList())

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { quranSurahCache.putSurah(any(), any(), any()) }
    }

    @Test
    fun `getVerse serves from cache when the surah is cached`() = runTest {
        val cached = listOf(verse(1, 1), verse(1, 2))
        coEvery { quranSurahCache.getSurah(any(), any()) } returns cached

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getVerse(1, 2, "tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(verse(1, 2))
        coVerify(exactly = 0) { quranDataSource.getSurah(any(), any()) }
    }

    @Test
    fun `getVerse fetches and caches the surah on a cache miss`() = runTest {
        val fetched = listOf(verse(1, 1), verse(1, 2))
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranSurahCache.putSurah(any(), any(), any()) } returns Unit
        coEvery { quranDataSource.getSurah(any(), "en") } returns Result.success(fetched)

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getVerse(1, 2, "en")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(verse(1, 2))
        coVerify(exactly = 1) { quranDataSource.getSurah(1, "en") }
        coVerify(exactly = 1) { quranSurahCache.putSurah(1, "en", fetched) }
    }

    @Test
    fun `getVerse returns failure when the verse is not in the surah`() = runTest {
        val fetched = listOf(verse(1, 1), verse(1, 2))
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(fetched)

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getVerse(1, 99, "tr")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getSavedVerses returns the store groups`() = runTest {
        val groups = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 1), verse(1, 2))))
        coEvery { savedVersesStore.getSavedVerseGroups() } returns groups
        coEvery { quranSurahCache.getSurah(any(), any()) } returns listOf(verse(1, 1), verse(1, 2))

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getSavedVerses("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(groups)
    }

    @Test
    fun `getSavedVerses re-localizes verses in the requested language`() = runTest {
        val stored = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 1).copy(text = "Türkçe metin"))))
        val localized = listOf(verse(1, 1).copy(text = "English text"))
        coEvery { savedVersesStore.getSavedVerseGroups() } returns stored
        coEvery { quranSurahCache.getSurah(any(), any()) } returns null
        coEvery { quranSurahCache.putSurah(any(), any(), any()) } returns Unit
        coEvery { quranDataSource.getSurah(any(), "en") } returns Result.success(localized)

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.getSavedVerses("en")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()[0].verses).isEqualTo(localized)
    }

    @Test
    fun `reorderSavedVerses persists the nested groups`() = runTest {
        val groups = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 2), verse(1, 1))))
        coEvery { savedVersesStore.saveGroups(groups) } returns Unit

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        val result = repository.reorderSavedVerses(groups)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { savedVersesStore.saveGroups(groups) }
    }

    @Test
    fun `collapse state delegates to the store`() = runTest {
        coEvery { savedVersesStore.getCollapsedSurahs() } returns setOf(1)
        coEvery { savedVersesStore.setCollapsedSurahs(any()) } returns Unit

        val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
        assertThat(repository.getCollapsedSurahs()).containsExactly(1)
        repository.setCollapsedSurahs(setOf(36))
        coVerify(exactly = 1) { savedVersesStore.setCollapsedSurahs(setOf(36)) }
    }
}
