package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import com.kutluoglu.prayer.model.Prayer
import java.time.LocalDate

/**
 * Created by F.K. on 17.10.2025.
 *
 */
interface PrayerCalculationService {
    fun calculateDailyPrayerTimes(
            latitude: Double,
            longitude: Double,
            date: LocalDate,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod
    ): List<Prayer>
}