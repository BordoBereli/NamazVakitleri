package com.kutluoglu.prayer.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Created by F.K. on 9.01.2026.
 *
 */
interface SettingsDataStore {
    fun observeSettings(): Flow<String>
    suspend fun getSettings(): String
    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        country: String,
        timeZone: String
    )
    suspend fun saveCalculationMethod(method: String)
    suspend fun saveLanguage(language: String)
    suspend fun saveHijriAdjustment(days: Int)
}