package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class PrayerLogicEngineImsakTest {

    private val zoneId = ZoneId.of("Europe/Istanbul")

    private fun prayer(name: String, time: LocalTime, isImsak: Boolean = false) =
        Prayer(name, name, time, LocalDate(2026, 9, 2), isImsak = isImsak)

    // Istanbul is UTC+3, so an instant at hour:minute UTC is hour+3:minute local.
    private fun engineAt(hour: Int, minute: Int): PrayerLogicEngine {
        val instant = Instant.parse(
            "2026-09-02T${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00Z"
        )
        return PrayerLogicEngine(Clock.fixed(instant, ZoneOffset.UTC))
    }

    @Test
    fun `current prayer between imsak and sunrise is not imsak`() {
        // 02:00 UTC = 05:00 Istanbul, between Imsak (04:50) and Sunrise (06:00)
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50), isImsak = true),
            prayer("Sunrise", LocalTime(6, 0)),
            prayer("Dhuhr", LocalTime(13, 0))
        )
        val (current, next) = engineAt(2, 0).findCurrentAndNextPrayer(prayers, zoneId)
        assertNotEquals("Imsak", current?.name)
        assertEquals("Sunrise", next?.name)
    }

    @Test
    fun `imsak is never returned as next prayer`() {
        // 03:00 UTC = 06:00 Istanbul, at Sunrise
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50), isImsak = true),
            prayer("Sunrise", LocalTime(6, 0))
        )
        val (_, next) = engineAt(3, 0).findCurrentAndNextPrayer(prayers, zoneId)
        assertNotEquals("Imsak", next?.name)
        assertEquals("Sunrise", next?.name)
    }
}
