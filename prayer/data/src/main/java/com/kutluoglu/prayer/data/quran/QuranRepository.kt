package com.kutluoglu.prayer.data.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 11.11.2025.
 *
 */

@Single
class QuranRepository(
    private val quranDataSource: QuranDataSource
): IQuranRepository {
    override suspend fun getRandomVerse(): Result<AyahData> =
        quranDataSource.getRandomVerse()
}