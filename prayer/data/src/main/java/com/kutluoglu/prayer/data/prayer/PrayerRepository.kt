package com.kutluoglu.prayer.data.prayer

import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
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
    ): List<Prayer> = prayerDataStore.getPrayerTimes(
        date = date,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId
    )

    override suspend fun getMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
    ): List<DailyPrayer>? = prayerDataStore.getMonthlyPrayerTimes(
        month = month,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId
    )

    override suspend fun saveMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        prayers: List<DailyPrayer>,
    ) {
        prayerDataStore.saveMonthlyPrayerTimes(
            month = month,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            prayers = prayers
        )
    }

    override suspend fun clearCache() {
        prayerDataStore.clearCache()
    }
}