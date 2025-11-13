package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.location.LocationData

/**
 * Created by F.K. on 11.11.2025.
 *
 */
interface ILocationRepository {
    suspend fun saveLocation(locationData: LocationData)
    suspend fun getSavedLocation(): LocationData?
}