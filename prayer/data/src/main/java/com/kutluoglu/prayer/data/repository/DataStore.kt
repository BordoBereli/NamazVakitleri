package com.kutluoglu.prayer.data.repository

import com.kutluoglu.prayer.data.model.LocationDataModel

/**
 * Created by F.K. on 28.11.2025.
 *
 */

/**
 * Interface defining methods for the data operations related to PrayerTimes.
 * This is to be implemented by external data source layers, setting the requirements for the
 * operations that need to be implemented
 */

interface DataStore {
    suspend fun saveLocation(locationDataModel: LocationDataModel)
    suspend fun getSavedLocation(): LocationDataModel?
}