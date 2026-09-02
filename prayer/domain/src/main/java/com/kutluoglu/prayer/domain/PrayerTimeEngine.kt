package com.kutluoglu.prayer.domain

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Factory
class PrayerTimeEngine : PrayerCalculationService {
    override fun calculateDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        date: LocalDateTime,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): List<Prayer> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents.fromLocalDateTime(date)
        val params = getCalculationParameters(calculationMethod, juristicMethod)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        // Use the provided zoneId for conversion
        val kotlinTimeZone = zoneId.toKotlinTimeZone()
        val imsak = prayerTimes.fajr.toLocalDateTime(kotlinTimeZone).time

        return listOf(
            Prayer(
                name = "Imsak",
                arabicName = "الإمساك",
                time = imsak,
                date = date.date,
                isImsak = true
            ),
            Prayer(
                name = "Sunrise",
                arabicName = "الشروق",
                time = prayerTimes.sunrise.toLocalDateTime(kotlinTimeZone).time,
                date = date.date
            ),
            Prayer(
                name = "Dhuhr",
                arabicName = "الظهر",
                time = prayerTimes.dhuhr.toLocalDateTime(kotlinTimeZone).time,
                date = date.date
            ),
            Prayer(
                name = "Asr",
                arabicName = "العصر",
                time = prayerTimes.asr.toLocalDateTime(kotlinTimeZone).time,
                date = date.date
            ),
            Prayer(
                name = "Maghrib",
                arabicName = "المغرب",
                time = prayerTimes.maghrib.toLocalDateTime(kotlinTimeZone).time,
                date = date.date
            ),
            Prayer(
                name = "Isha",
                arabicName = "العشاء",
                time = prayerTimes.isha.toLocalDateTime(kotlinTimeZone).time,
                date = date.date
            )
        )
    }

    private fun getCalculationParameters(
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): CalculationParameters {
        val params = when (calculationMethod) {
            CalculationMethod.TURKEY_DIYANET -> com.batoulapps.adhan2.CalculationMethod.TURKEY.parameters
            CalculationMethod.MWL -> com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            CalculationMethod.ISNA -> com.batoulapps.adhan2.CalculationMethod.NORTH_AMERICA.parameters
            CalculationMethod.EGYPT -> com.batoulapps.adhan2.CalculationMethod.EGYPTIAN.parameters
            CalculationMethod.MAKKAH -> com.batoulapps.adhan2.CalculationMethod.UMM_AL_QURA.parameters
            CalculationMethod.KARACHI -> com.batoulapps.adhan2.CalculationMethod.KARACHI.parameters
        }
        return when (juristicMethod) {
            JuristicMethod.STANDARD -> params.copy(madhab = com.batoulapps.adhan2.Madhab.SHAFI)
            JuristicMethod.HANAFI -> params.copy(madhab = com.batoulapps.adhan2.Madhab.HANAFI)
        }
    }
}
