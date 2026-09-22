package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.Flow

/**
 * Clean persistence contract for the active/saved location, owned by
 * `prayer_location`. Consumers (e.g. `prayer_settings`) depend on this
 * interface instead of reaching into data-layer internals.
 */
interface SavedLocationStore {
    suspend fun saveLocation(location: LocationData)
    suspend fun getSavedLocation(): LocationData?
    fun observeLocation(): Flow<LocationData?>
}
