package com.kutluoglu.prayer.usecases.prayer

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.repository.IPrayerRepository
import kotlinx.datetime.YearMonth
import org.koin.core.annotation.Factory
import java.time.ZoneId

/**
 * Persists a whole month of prayer times for the given location so it can be
 * restored without recalculating.
 */
@Factory
class SaveMonthlyPrayerTimesUseCase(
    private val prayerRepository: IPrayerRepository
) {
    suspend operator fun invoke(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
        prayers: List<DailyPrayer>,
    ) {
        prayerRepository.saveMonthlyPrayerTimes(
            month = month,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            juristicMethod = juristicMethod,
            prayers = prayers
        )
    }
}
