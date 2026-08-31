package com.kutluoglu.prayer.data.quran

import com.kutluoglu.prayer.data.cache.QuranSurahCache
import com.kutluoglu.prayer.data.cache.SavedVersesStore
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import kotlin.random.Random
import org.koin.core.annotation.Single

/**
 * Cache-first random verse provider. Picks a random surah (1-114); serves from
 * the local cache when present, otherwise fetches the whole surah in one call
 * and caches it for future use.
 */
@Single
class QuranRepository(
    private val quranDataSource: QuranDataSource,
    private val quranSurahCache: QuranSurahCache,
    private val savedVersesStore: SavedVersesStore
) : IQuranRepository {
    override suspend fun getRandomVerse(langCode: String): Result<AyahData> = runCatching {
        val surahNumber = Random.nextInt(1, 115)
        val cached = quranSurahCache.getSurah(surahNumber)
        if (!cached.isNullOrEmpty()) {
            cached.random()
        } else {
            val ayahs = quranDataSource.getSurah(surahNumber, langCode).getOrThrow()
            if (ayahs.isNotEmpty()) {
                quranSurahCache.putSurah(surahNumber, ayahs)
            }
            require(ayahs.isNotEmpty()) { "Surah $surahNumber returned no ayahs" }
            ayahs.random()
        }
    }

    override suspend fun isVerseSaved(verse: AyahData): Boolean = savedVersesStore.isSaved(verse)

    override suspend fun toggleSavedVerse(verse: AyahData): Result<Unit> =
        runCatching { savedVersesStore.toggle(verse) }

    override suspend fun getSavedVerses(): Result<List<AyahData>> =
        runCatching { savedVersesStore.getSavedVerses() }

    override suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit> =
        runCatching { savedVersesStore.reorder(verses) }
}
