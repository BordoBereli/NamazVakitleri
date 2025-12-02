package com.kutluoglu.prayer.data.repository.location

import com.kutluoglu.prayer.common.Result
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetLocationError

/**
 * Created by F.K. on 2.12.2025.
 *
 */
interface LocationDataStore {
    suspend fun saveLocation(locationData: LocationData)
    suspend fun getSavedLocation(): Result<LocationData, GetLocationError>
}