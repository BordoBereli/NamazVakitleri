package com.kutluoglu.prayer.data.repository

import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import java.time.ZoneId

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
    /* Location */
    suspend fun saveLocation(locationDataModel: LocationDataModel)
    suspend fun getSavedLocation(): LocationDataModel?

    /* Prayer */
    suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
    ): List<Prayer>
}