package com.kutluoglu.prayer.data.source.location

import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.DataStore
import com.kutluoglu.prayer.data.repository.location.LocationCache
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class LocationDataStore(
    private val locationCache: LocationCache
) : DataStore {
    override suspend fun saveLocation(locationDataModel: LocationDataModel) {
        locationCache.saveLocation(locationDataModel)
    }

    override suspend fun getSavedLocation() = locationCache.getSavedLocation()

}