package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDateTime
import java.time.ZoneId

fun interface DailyPrayerTimesSource {
    suspend fun getDailyPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        persistDailyCache: Boolean
    ): Result<List<Prayer>>
}
