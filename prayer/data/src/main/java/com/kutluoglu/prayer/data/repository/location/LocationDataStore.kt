package com.kutluoglu.prayer.data.repository.location

import com.kutluoglu.prayer.model.location.LocationData

/**
 * Created by F.K. on 2.12.2025.
 *
 */
interface LocationDataStore {
    suspend fun saveLocation(locationData: LocationData)
    suspend fun getSavedLocation(): Result<LocationData>
}