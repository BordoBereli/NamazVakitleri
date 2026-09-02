package com.kutluoglu.prayer.data.repository.prayer

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
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
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
        persistDailyCache: Boolean = true,
    ): List<Prayer>

    suspend fun getMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    ): List<DailyPrayer>?

    suspend fun saveMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
        prayers: List<DailyPrayer>,
    )

    suspend fun clearCache()
}