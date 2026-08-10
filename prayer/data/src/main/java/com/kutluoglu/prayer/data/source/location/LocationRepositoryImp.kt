package com.kutluoglu.prayer.data.source.location

import com.kutluoglu.prayer.data.mapper.location.LocationMapper
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.LocationRepository
import com.kutluoglu.prayer.usecases.location.LocationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class LocationRepositoryImp(
        private val locationCache: LocationDataStore,
        private val locationMapper: LocationMapper
): LocationRepository {
    override suspend fun saveLocation(locationData: LocationData) {
        locationCache.saveLocation(
            locationMapper.mapFromDomain(locationData)
        )
    }

    override suspend fun getSavedLocation() =  try {
        val locationDataModel = locationCache.getSavedLocation()
        if (locationDataModel != null) {
            Result.success(locationMapper.mapToDomain(locationDataModel))
        } else {
            Result.failure(Exception(LocationError.NOT_FOUND().message))
        }
    } catch (e: Exception) {
        Result.failure(Exception(LocationError.UNKNOWN(e.message).message))
    }

    override fun observeLocation(): Flow<LocationData> {
        return locationCache.observeLocation().map { model ->
            if (model != null) {
                locationMapper.mapToDomain(model)
            } else {
                throw Exception(LocationError.NOT_FOUND().message)
            }
        }
    }
}