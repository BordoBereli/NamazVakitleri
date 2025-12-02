package com.kutluoglu.prayer.data.source.location

import com.kutluoglu.prayer.common.Result
import com.kutluoglu.prayer.data.mapper.location.LocationMapper
import com.kutluoglu.prayer.data.repository.location.LocationCache
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetLocationError
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class LocationDataStoreImp(
    private val locationCache: LocationCache,
    private val locationMapper: LocationMapper
): LocationDataStore {
    override suspend fun saveLocation(locationData: LocationData) {
        locationCache.saveLocation(
            locationMapper.mapFromDomain(locationData)
        )
    }

    override suspend fun getSavedLocation() =  try {
        val locationDataModel = locationCache.getSavedLocation()
        if (locationDataModel != null) {
            Result.Success(locationMapper.mapToDomain(locationDataModel))
        } else {
            Result.Error(GetLocationError.NOT_FOUND)
        }
    } catch (e: Exception) {
        Result.Error(GetLocationError.UNKNOWN(e.message))
    }
}