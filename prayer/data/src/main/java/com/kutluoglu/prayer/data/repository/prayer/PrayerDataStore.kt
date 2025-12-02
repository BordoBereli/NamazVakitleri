package com.kutluoglu.prayer.data.repository.prayer

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

interface PrayerDataStore {
    suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
    ): List<Prayer>
}