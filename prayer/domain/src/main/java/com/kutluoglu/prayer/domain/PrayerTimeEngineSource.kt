package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import java.time.ZoneId

class PrayerTimeEngineSource(
    private val engine: PrayerTimeEngine
) : DailyPrayerTimesSource {
    override suspend fun getDailyPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        persistDailyCache: Boolean
    ): Result<List<Prayer>> =
        runCatching {
            engine.calculateDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                date = date,
                calculationMethod = calculationMethod,
                juristicMethod = juristicMethod
            )
        }
}
