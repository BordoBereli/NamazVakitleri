package com.kutluoglu.prayer.data.source.prayer

import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import kotlinx.datetime.LocalDateTime
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

        val calculated = prayerCalculationService.calculateDailyPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            date = date,
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD
        )
        prayerTimesCache.put(cacheKey, calculated)
        return calculated
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
}