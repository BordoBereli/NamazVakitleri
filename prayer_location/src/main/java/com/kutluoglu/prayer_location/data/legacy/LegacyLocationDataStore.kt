package com.kutluoglu.prayer_location.data.legacy

import kotlinx.coroutines.flow.Flow

/**
 * Legacy single-location persistence contract. Retained for migration from the
 * old single-location store into the modern multi-location store.
 */
interface LegacyLocationDataStore {
    suspend fun saveLocation(locationDataModel: LegacyLocationDataModel)
    suspend fun getSavedLocation(): LegacyLocationDataModel?
    fun observeLocation(): Flow<LegacyLocationDataModel?>
}
