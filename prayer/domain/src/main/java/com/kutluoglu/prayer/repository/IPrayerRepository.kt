package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId


/**
 * Interface for the PrayerRepository, defining the contract for data operations.
 */
interface IPrayerRepository {
    /**
     * Fetches prayer times for a specific date and location.
     * It will handle the logic of whether to fetch from a local cache or calculate new times.
     */
    suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
    ): List<Prayer>
}