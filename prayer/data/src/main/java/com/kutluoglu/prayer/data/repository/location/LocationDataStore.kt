package com.kutluoglu.prayer.data.repository.location

import com.kutluoglu.prayer.data.model.LocationDataModel
import kotlinx.coroutines.flow.Flow

/**
 * Created by F.K. on 30.11.2025.
 *
 */
interface LocationDataStore {
    suspend fun saveLocation(locationDataModel: LocationDataModel)
    suspend fun getSavedLocation(): LocationDataModel?
    fun observeLocation(): Flow<LocationDataModel?>
}