package com.kutluoglu.prayer.data.location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.ILocationRepository
import com.kutluoglu.prayer_cache.LocationCache
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 11.11.2025.
 *
 */

@Single
class LocationRepository(
    private val locationCache: LocationCache
): ILocationRepository {
    override suspend fun saveLocation(locationData: LocationData) {
        locationCache.saveLocation(locationData)
    }

    override suspend fun getSavedLocation(): LocationData? =
        locationCache.getSavedLocation()
}