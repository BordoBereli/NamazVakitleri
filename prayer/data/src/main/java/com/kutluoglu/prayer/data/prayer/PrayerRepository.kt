package com.kutluoglu.prayer.data.prayer

import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import org.koin.core.annotation.Single
import java.time.ZoneId

@Single
class PrayerRepository(
    private val prayerDataStore: PrayerDataStore
) : IPrayerRepository {
    override suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        imsakOffsetMinutes: Int,
        persistDailyCache: Boolean,
    ): List<Prayer> = prayerDataStore.getPrayerTimes(
        date = date,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod,
        imsakOffsetMinutes = imsakOffsetMinutes,
        persistDailyCache = persistDailyCache
    )

    override suspend fun getMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
    ): List<DailyPrayer>? = prayerDataStore.getMonthlyPrayerTimes(
        month = month,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod
    )

    override suspend fun saveMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        prayers: List<DailyPrayer>,
    ) {
        prayerDataStore.saveMonthlyPrayerTimes(
            month = month,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            prayers = prayers
        )
    }

    override suspend fun clearCache() {
        prayerDataStore.clearCache()
    }
}