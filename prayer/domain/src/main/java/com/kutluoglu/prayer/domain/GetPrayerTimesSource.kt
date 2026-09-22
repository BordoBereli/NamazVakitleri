package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import kotlinx.datetime.LocalDateTime
import java.time.ZoneId

class GetPrayerTimesSource(
    private val useCase: GetPrayerTimesUseCase
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
        useCase(date, latitude, longitude, zoneId, calculationMethod, juristicMethod, persistDailyCache)
}
