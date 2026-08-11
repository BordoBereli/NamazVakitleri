package com.kutluoglu.prayer.data.source.prayer

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
        private val prayerCalculationService: PrayerCalculationService
): PrayerDataStore {
    override suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId
    ): List<Prayer> = prayerCalculationService.calculateDailyPrayerTimes(
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        date = date,
        calculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod = JuristicMethod.STANDARD
    )
}