package com.kutluoglu.prayer.data

import com.kutluoglu.prayer.data.source.location.LocationDataStoreImp
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.ILocationRepository
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class LocationRepositoryImp(
    private val locationDataStore: LocationDataStoreImp
): ILocationRepository {
    override suspend fun saveLocation(locationData: LocationData) {
        locationDataStore.saveLocation(locationData)
    }

    override suspend fun getSavedLocation() = locationDataStore.getSavedLocation()
}