package com.kutluoglu.prayer.data

import com.kutluoglu.prayer.data.mapper.location.LocationMapper
import com.kutluoglu.prayer.data.source.DataStoreFactory
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.ILocationRepository
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class PrayerRepositoryImp(
    private val factory: DataStoreFactory,
    private val locationMapper: LocationMapper
): ILocationRepository {
    override suspend fun saveLocation(locationData: LocationData) {
        factory.retrieveLocationDataStore().saveLocation(
            locationMapper.mapFromDomain(locationData)
        )
    }

    override suspend fun getSavedLocation() = factory.retrieveLocationDataStore().getSavedLocation()?.let {
        savedLocation -> locationMapper.mapToDomain(savedLocation)
    }
}