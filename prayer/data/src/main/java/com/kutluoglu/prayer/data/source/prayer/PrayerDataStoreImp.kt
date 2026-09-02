package com.kutluoglu.prayer.data.source.prayer

import android.util.Log
import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.time.ZoneId

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class PrayerDataStoreImp(
        private val prayerCalculationService: PrayerCalculationService,
        private val prayerTimesCache: PrayerTimesCache,
        @Named("preCacheScope") private val preCacheScope: CoroutineScope
): PrayerDataStore {
    override suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod,
            persistDailyCache: Boolean,
    ): List<Prayer> {
        val cacheKey = buildCacheKey(date, latitude, longitude, zoneId, calculationMethod, juristicMethod)
        val cached = prayerTimesCache.get(cacheKey)
        if (cached != null) {
            if (persistDailyCache) preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod, juristicMethod)
            return cached
        }

        val calculated = withContext(Dispatchers.Default) {
            prayerCalculationService.calculateDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                date = date,
                calculationMethod = calculationMethod,
                juristicMethod = juristicMethod
            )
        }
        if (persistDailyCache) {
            prayerTimesCache.put(cacheKey, calculated)
            preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod, juristicMethod)
        }
        return calculated
    }

    private fun preCacheTomorrow(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod
    ) {
        preCacheScope.launch {
            runCatching {
                val tomorrow = date.date.plus(1, DateTimeUnit.DAY)
                    .atTime(date.hour, date.minute, date.second, date.nanosecond)
                val tomorrowKey = buildCacheKey(tomorrow, latitude, longitude, zoneId, calculationMethod, juristicMethod)
                if (prayerTimesCache.get(tomorrowKey) != null) return@runCatching
                val tomorrowPrayers = prayerCalculationService.calculateDailyPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    zoneId = zoneId,
                    date = tomorrow,
                    calculationMethod = calculationMethod,
                    juristicMethod = juristicMethod
                )
                prayerTimesCache.put(tomorrowKey, tomorrowPrayers)
            }.onFailure { error ->
                Log.e("PrayerDataStoreImp", "Failed to pre-cache tomorrow: ${error.message}")
            }
        }
    }

    override suspend fun getMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod,
    ): List<DailyPrayer>? {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId, calculationMethod, juristicMethod)
        return prayerTimesCache.getMonth(cacheKey)
    }

    override suspend fun saveMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod,
            prayers: List<DailyPrayer>
    ) {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId, calculationMethod, juristicMethod)
        prayerTimesCache.putMonth(cacheKey, prayers)
    }

    override suspend fun clearCache() {
        prayerTimesCache.clear()
    }

    private fun buildCacheKey(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod
    ): String = "${date.date}|$latitude|$longitude|${zoneId.id}|$calculationMethod|$juristicMethod"

    private fun buildMonthCacheKey(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod
    ): String = "$month|$latitude|$longitude|${zoneId.id}|$calculationMethod|$juristicMethod"
}
