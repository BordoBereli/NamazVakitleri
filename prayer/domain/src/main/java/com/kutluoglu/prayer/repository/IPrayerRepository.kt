package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
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
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<Prayer>

    /**
     * Returns a previously cached month of prayer times for the given location,
     * or null when the month is not cached.
     */
    suspend fun getMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>?

    /**
     * Persists a whole month of prayer times for the given location so it can be
     * restored without recalculating.
     */
    suspend fun saveMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
            prayers: List<DailyPrayer>,
    )

    /**
     * Clears any locally cached prayer times.
     */
    suspend fun clearCache()
}