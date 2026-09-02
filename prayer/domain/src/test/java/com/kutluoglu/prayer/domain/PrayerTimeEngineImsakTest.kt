package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerTimeEngineImsakTest {

    private val engine = PrayerTimeEngine()
    private val zoneId = ZoneId.of("Europe/Istanbul")
    private val date = LocalDateTime(2026, 9, 2, 0, 0)

    private fun calculate() = engine.calculateDailyPrayerTimes(
        latitude = 41.0082,
        longitude = 28.9784,
        zoneId = zoneId,
        date = date,
        calculationMethod = CalculationMethod.TURKEY_DIYANET,
        juristicMethod = JuristicMethod.STANDARD
    )

    @Test
    fun `returns imsak as first prayer`() {
        val prayers = calculate()
        assertEquals("Imsak", prayers.first().name)
        assertTrue(prayers.first().isImsak)
    }

    @Test
    fun `does not return a fajr prayer`() {
        val prayers = calculate()
        assertFalse(prayers.any { it.name == "Fajr" })
    }

    @Test
    fun `imsak is earlier than sunrise`() {
        val prayers = calculate()
        val imsak = prayers.first { it.isImsak }
        val sunrise = prayers.first { it.name == "Sunrise" }
        assertTrue(imsak.time < sunrise.time)
    }

    @Test
    fun `returns six prayers`() {
        assertEquals(6, calculate().size)
    }
}
