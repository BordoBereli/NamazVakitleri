package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.quran.AyahData

/**
 * Created by F.K. on 11.11.2025.
 *
 */
interface IQuranRepository {
    suspend fun getRandomVerse(language: String): Result<AyahData>
    suspend fun isVerseSaved(verse: AyahData): Boolean
    suspend fun toggleSavedVerse(verse: AyahData): Result<Unit>
    suspend fun getSavedVerses(): Result<List<AyahData>>
    suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit>
}
