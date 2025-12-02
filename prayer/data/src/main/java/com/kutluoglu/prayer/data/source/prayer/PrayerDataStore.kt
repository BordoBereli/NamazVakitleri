package com.kutluoglu.prayer.data.source.prayer

import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.DataStore
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single
import java.time.ZoneId

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class PrayerDataStore(

): DataStore {
    override suspend fun saveLocation(locationDataModel: LocationDataModel) {
        TODO("Not yet implemented")
    }

    override suspend fun getSavedLocation(): LocationDataModel? {
        TODO("Not yet implemented")
    }

    override suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): List<Prayer> {
        TODO("Not yet implemented")
    }
}