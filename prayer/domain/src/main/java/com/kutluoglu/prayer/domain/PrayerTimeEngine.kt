package com.kutluoglu.prayer.domain

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Prayer.*
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class PrayerTimeEngine : PrayerCalculationService {
    override fun calculateDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): List<Prayer> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents.from(
            Date.from(date.atStartOfDay(
                ZoneId.systemDefault()).toInstant()).toInstant().toKotlinInstant()
        )
        val params = getCalculationParameters(calculationMethod, juristicMethod)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
        return listOf(
            Prayer(
                name = "Fajr",
                arabicName = "الفجر",
                time = prayerTimes.fajr.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.fajr) == FAJR,
                calculationMethod = calculationMethod
            ),
            Prayer(
                name = "Sunrise",
                arabicName = "الشروق",
                time = prayerTimes.sunrise.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.sunrise) == SUNRISE,
                calculationMethod = calculationMethod
            ),
            Prayer(
                name = "Dhuhr",
                arabicName = "الظهر",
                time = prayerTimes.dhuhr.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.dhuhr) == DHUHR,
                calculationMethod = calculationMethod
            ),
            Prayer(
                name = "Asr",
                arabicName = "العصر",
                time = prayerTimes.asr.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.asr) == ASR,
                calculationMethod = calculationMethod
            ),
            Prayer(
                name = "Maghrib",
                arabicName = "المغرب",
                time = prayerTimes.maghrib.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.maghrib) == MAGHRIB,
                calculationMethod = calculationMethod
            ),
            Prayer(
                name = "Isha",
                arabicName = "العشاء",
                time = prayerTimes.isha.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalTime(),
                date = date,
                isCurrent = prayerTimes.currentPrayer(prayerTimes.isha) == ISHA,
                calculationMethod = calculationMethod
            )
        )
    }

    private fun getCalculationParameters(
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): CalculationParameters {
        val method = when (calculationMethod) {
            CalculationMethod.TURKEY_DIYANET -> com.batoulapps.adhan2.CalculationMethod.TURKEY
            CalculationMethod.ISNA -> com.batoulapps.adhan2.CalculationMethod.NORTH_AMERICA
            CalculationMethod.MUSLIM_WORLD_LEAGUE -> com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
        var params = method.parameters
        params = when(juristicMethod) {
            JuristicMethod.STANDARD -> params.copy(madhab =  com.batoulapps.adhan2.Madhab.SHAFI)
            JuristicMethod.HANAFI -> params.copy(madhab = com.batoulapps.adhan2.Madhab.HANAFI)
        }
        return params
    }
}