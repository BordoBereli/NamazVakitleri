package com.kutluoglu.prayer.domain

import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class PrayerCalculationServiceTest {

    @Test
    fun `calculateDailyPrayerTimes successful`() {
        // This test documents our starting point.

        // GIVEN a specific, known set of parameters
        val service = PrayerTimeEngine()
        val date = LocalDateTime.createBy(2025, 9, 15)
        val latitude = 41.03648429460445 // Halkalı/Küçükçekmece/Istanbul
        val longitude = 28.79004556525033
        val method = CalculationMethod.TURKEY_DIYANET
        val juristic = JuristicMethod.STANDARD

        // WHEN we call the function we are about to build
        val prayers = service.calculateDailyPrayerTimes(
            latitude,
            longitude,
            date,
            method,
            juristic
        )

        // THEN we expect a specific, verifiable result
        val fajrPrayer = prayers.first { it.name == "Fajr" }
        val sunrisePrayer = prayers.first { it.name == "Sunrise" }
        val dhuhrPrayer = prayers.first { it.name == "Dhuhr" }
        val asrPrayer = prayers.first { it.name == "Asr" }
        val maghribPrayer = prayers.first { it.name == "Maghrib" }
        val ishaPrayer = prayers.first { it.name == "Isha" }
        // NOTE: Replace this with a time from a trusted source for that exact date/location.
        assertThat(fajrPrayer.time).isEqualTo(LocalTime.parse("05:12"))
        assertThat(sunrisePrayer.time).isEqualTo(LocalTime.parse("06:38"))
        assertThat(dhuhrPrayer.time).isEqualTo(LocalTime.parse("13:05"))
        assertThat(asrPrayer.time).isEqualTo(LocalTime.parse("16:34"))
        assertThat(maghribPrayer.time).isEqualTo(LocalTime.parse("19:21"))
        assertThat(ishaPrayer.time).isEqualTo(LocalTime.parse("20:42"))
    }
}

