package com.kutluoglu.prayer.data.prayer

import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import kotlinx.datetime.LocalDateTime
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
}