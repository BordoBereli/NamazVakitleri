package com.kutluoglu.prayer.domain

import kotlinx.datetime.LocalTime
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
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
}
