package com.kutluoglu.prayer.data.repository.location

import com.kutluoglu.prayer.data.model.LocationDataModel

/**
 * Created by F.K. on 30.11.2025.
 *
 */
interface LocationCache {
    suspend fun saveLocation(locationDataModel: LocationDataModel)
    suspend fun getSavedLocation(): LocationDataModel?
}