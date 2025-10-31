package com.kutluoglu.prayer.services

import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDateTime
import java.text.SimpleDateFormat
import java.time.ZoneId

/**
 * Created by F.K. on 17.10.2025.
 *
 */
interface PrayerCalculationService {
    fun calculateDailyPrayerTimes(
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            date: LocalDateTime,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod
    ): List<Prayer>
}