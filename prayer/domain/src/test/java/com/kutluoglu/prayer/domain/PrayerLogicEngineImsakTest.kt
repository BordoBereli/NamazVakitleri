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

    @Test
    fun `current prayer between imsak and fajr is not imsak`() {
        val engine = PrayerLogicEngine(Clock.fixed(
            Instant.parse("2026-09-02T01:55:00Z"), ZoneOffset.UTC
        ))
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50), isImsak = true),
            prayer("Fajr", LocalTime(5, 0)),
            prayer("Sunrise", LocalTime(6, 30)),
            prayer("Dhuhr", LocalTime(12, 30))
        )
        val (current, next) = engine.findCurrentAndNextPrayer(prayers, zoneId)
        assertNotEquals("Imsak", current?.name)
        assertNotEquals("Imsak", next?.name)
        assertEquals("Fajr", next?.name)
    }

    @Test
    fun `imsak is never returned as next prayer`() {
        val engine = PrayerLogicEngine(Clock.fixed(
            Instant.parse("2026-09-01T20:00:00Z"), ZoneOffset.UTC
        ))
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50), isImsak = true),
            prayer("Fajr", LocalTime(5, 0)),
            prayer("Dhuhr", LocalTime(12, 30)),
            prayer("Isha", LocalTime(21, 0))
        )
        val (_, next) = engine.findCurrentAndNextPrayer(prayers, zoneId)
        assertNotEquals("Imsak", next?.name)
    }
}
