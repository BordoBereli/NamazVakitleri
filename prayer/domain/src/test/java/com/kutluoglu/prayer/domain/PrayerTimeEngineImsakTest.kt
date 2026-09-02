package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerTimeEngineImsakTest {

    private val engine = PrayerTimeEngine()
    private val zoneId = ZoneId.of("Europe/Istanbul")
    private val date = LocalDateTime(2026, 9, 2, 0, 0)

    @Test
    fun `imsak equals fajr minus default offset of 10 minutes`() {
        val prayers = engine.calculateDailyPrayerTimes(
            latitude = 41.0082, longitude = 28.9784, zoneId = zoneId, date = date,
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD
        )
        val fajr = prayers.first { it.name == "Fajr" }
        val imsak = prayers.first { it.isImsak }
        assertEquals(fajr.time.toJavaLocalTime().minusMinutes(10).toKotlinLocalTime(), imsak.time)
    }

    @Test
    fun `imsak offset zero makes imsak equal fajr`() {
        val prayers = engine.calculateDailyPrayerTimes(
            latitude = 41.0082, longitude = 28.9784, zoneId = zoneId, date = date,
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD,
            imsakOffsetMinutes = 0
        )
        val fajr = prayers.first { it.name == "Fajr" }
        val imsak = prayers.first { it.isImsak }
        assertEquals(fajr.time, imsak.time)
    }
}
