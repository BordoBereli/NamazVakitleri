package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PrayerLogicEngineTest {

    private val engine = PrayerLogicEngine()

    @Test
    fun `calculateTimeRemaining uses the provided zone instead of system default`() {
        // GIVEN two zones with a known 3-hour offset (UTC vs Europe/Istanbul)
        val utc = ZoneId.of("UTC")
        val istanbul = ZoneId.of("Europe/Istanbul")
        val nextPrayerTime = LocalTime.parse("12:00")

        // WHEN computing remaining time in each zone
        val durationUtc = engine.calculateTimeRemaining(nextPrayerTime, utc)
        val durationIstanbul = engine.calculateTimeRemaining(nextPrayerTime, istanbul)

        // THEN the difference must equal the 3-hour zone offset (mod 24h)
        val diffHours = (durationUtc - durationIstanbul).toHours().mod(24)
        assertThat(diffHours).isEqualTo(3)
    }

    @Test
    fun `findCurrentAndNextPrayer uses the provided zone instead of system default`() {
        // GIVEN a fixed clock at 12:00 UTC and prayers spanning the day
        val engine = PrayerLogicEngine(
            Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
        )
        val prayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2026, 8, 11)),
            Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 8, 11)),
            Prayer("Isha", "العشاء", LocalTime.parse("20:00"), LocalDate(2026, 8, 11))
        )

        // WHEN computing the current prayer in UTC (now = 12:00) vs Istanbul (now = 15:00)
        val (currentUtc, _) = engine.findCurrentAndNextPrayer(prayers, ZoneId.of("UTC"))
        val (currentIstanbul, _) = engine.findCurrentAndNextPrayer(prayers, ZoneId.of("Europe/Istanbul"))

        // THEN the zone determines which prayer is current
        assertThat(currentUtc?.name).isEqualTo("Fajr")
        assertThat(currentIstanbul?.name).isEqualTo("Dhuhr")
    }
}
