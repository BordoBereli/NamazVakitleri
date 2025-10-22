package com.kutluoglu.prayer.services

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Prayer.*
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@OptIn(ExperimentalTime::class)
class PrayerTimeEngine : PrayerCalculationService {
    override fun calculateDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDateTime,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): List<Prayer> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents.fromLocalDateTime(date)
        val params = getCalculationParameters(calculationMethod, juristicMethod)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        return listOf(
            Prayer(
                name = "Fajr",
                arabicName = "الفجر",
                time = prayerTimes.fajr.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == FAJR
            ),
            Prayer(
                name = "Sunrise",
                arabicName = "الشروق",
                time = prayerTimes.sunrise.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == SUNRISE
            ),
            Prayer(
                name = "Dhuhr",
                arabicName = "الظهر",
                time = prayerTimes.dhuhr.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == DHUHR
            ),
            Prayer(
                name = "Asr",
                arabicName = "العصر",
                time = prayerTimes.asr.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == ASR
            ),
            Prayer(
                name = "Maghrib",
                arabicName = "المغرب",
                time = prayerTimes.maghrib.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == MAGHRIB
            ),
            Prayer(
                name = "Isha",
                arabicName = "العشاء",
                time = prayerTimes.isha.toLocalDateTime(ZoneId.systemDefault().toKotlinTimeZone()).time,
                date = date.date,
                isCurrent = prayerTimes.currentPrayer(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                ) == ISHA
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