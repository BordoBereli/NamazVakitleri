package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.quran.AyahData

/**
 * Created by F.K. on 11.11.2025.
 *
 */
interface IQuranRepository {
    suspend fun getRandomVerse(): Result<AyahData>
}