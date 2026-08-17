package com.kutluoglu.prayer.data.source.prayer

import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.YearMonth
import org.koin.core.annotation.Single
import java.time.ZoneId

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class PrayerDataStoreImp(
        private val prayerCalculationService: PrayerCalculationService,
        private val prayerTimesCache: PrayerTimesCache
): PrayerDataStore {
    override suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): List<Prayer> {
        val cacheKey = buildCacheKey(date, latitude, longitude, zoneId)
        prayerTimesCache.get(cacheKey)?.let { return it }

        val calculated = withContext(Dispatchers.Default) {
            prayerCalculationService.calculateDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                date = date,
                calculationMethod = CalculationMethod.TURKEY_DIYANET,
                juristicMethod = JuristicMethod.STANDARD
            )
        }
        prayerTimesCache.put(cacheKey, calculated)
        preCacheTomorrow(date, latitude, longitude, zoneId)
        return calculated
    }

    private suspend fun preCacheTomorrow(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ) {
        val tomorrow = date.date.plus(1, DateTimeUnit.DAY)
            .atTime(date.hour, date.minute, date.second, date.nanosecond)
        val tomorrowKey = buildCacheKey(tomorrow, latitude, longitude, zoneId)
        if (prayerTimesCache.get(tomorrowKey) != null) return
        val tomorrowPrayers = withContext(Dispatchers.Default) {
            prayerCalculationService.calculateDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                date = tomorrow,
                calculationMethod = CalculationMethod.TURKEY_DIYANET,
                juristicMethod = JuristicMethod.STANDARD
            )
        }
        prayerTimesCache.put(tomorrowKey, tomorrowPrayers)
    }

    override suspend fun getMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): List<DailyPrayer>? {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId)
        return prayerTimesCache.getMonth(cacheKey)
    }

    override suspend fun saveMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            prayers: List<DailyPrayer>
    ) {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId)
        prayerTimesCache.putMonth(cacheKey, prayers)
    }

    override suspend fun clearCache() {
        prayerTimesCache.clear()
    }

    private fun buildCacheKey(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): String = "${date.date}|$latitude|$longitude|${zoneId.id}"

    private fun buildMonthCacheKey(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): String = "$month|$latitude|$longitude|${zoneId.id}"
}