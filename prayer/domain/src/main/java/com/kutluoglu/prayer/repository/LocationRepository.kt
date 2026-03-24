package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.Flow


/**
 * Created by F.K. on 11.11.2025.
 *
 */
interface LocationRepository {
    suspend fun saveLocation(locationData: LocationData)
    suspend fun getSavedLocation(): Result<LocationData>
    fun observeLocation(): Flow<LocationData>
}