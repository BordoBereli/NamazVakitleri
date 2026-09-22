package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class PrayerTimesFormattingTest {

    @Test
    fun `toEpochMillis converts prayer date and time to epoch millis in zone`() {
        val prayer = Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 4))

        val millis = toEpochMillis(prayer, ZoneId.of("Europe/Istanbul"))

        // 2026-09-04T13:00:00+03:00 == 2026-09-04T10:00:00Z
        assertThat(millis).isEqualTo(Instant.parse("2026-09-04T10:00:00Z").toEpochMilli())
    }

    @Test
    fun `formatClockTime pads hours and minutes`() {
        assertThat(formatClockTime(LocalTime.parse("05:05"))).isEqualTo("05:05")
        assertThat(formatClockTime(LocalTime.parse("13:30"))).isEqualTo("13:30")
    }

    @Test
    fun `isJumuahPrayer is true for friday dhuhr`() {
        val fridayDhuhr = Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 4))

        assertThat(isJumuahPrayer(fridayDhuhr)).isTrue()
    }

    @Test
    fun `isJumuahPrayer is false for non-friday dhuhr`() {
        val thursdayDhuhr = Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 3))

        assertThat(isJumuahPrayer(thursdayDhuhr)).isFalse()
    }

    @Test
    fun `isJumuahPrayer is false for friday non-dhuhr`() {
        val fridayAsr = Prayer("Asr", "العصر", LocalTime.parse("16:30"), LocalDate(2026, 9, 4))

        assertThat(isJumuahPrayer(fridayAsr)).isFalse()
    }
}
