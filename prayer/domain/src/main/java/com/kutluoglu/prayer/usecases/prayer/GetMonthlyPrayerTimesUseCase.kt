package com.kutluoglu.prayer.usecases.prayer

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.repository.IPrayerRepository
import kotlinx.datetime.YearMonth
import org.koin.core.annotation.Factory
import java.time.ZoneId

/**
 * Returns a previously cached month of prayer times for the given location,
 * or null when the month is not cached.
 */
@Factory
class GetMonthlyPrayerTimesUseCase(
    private val prayerRepository: IPrayerRepository
) {
    suspend operator fun invoke(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    ): List<DailyPrayer>? = prayerRepository.getMonthlyPrayerTimes(
        month = month,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod,
        juristicMethod = juristicMethod
    )
}
