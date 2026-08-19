package com.kutluoglu.prayer_feature.common.prayerUtils

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate

class PrayerFormatterTest {

    private val formatter = PrayerFormatter(mockk(relaxed = true))

    private val zoneId = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `getInitialTimeInfo formats unadjusted hijri date when adjustment is zero`() {
        val hijrahDate = HijrahDate.of(1448, 1, 1)

        val state = formatter.getInitialTimeInfo(zoneId, today, hijrahDate, hijriAdjustment = 0)

        assertThat(state.hijriDate).isEqualTo("01 Muharram 1448")
    }

    @Test
    fun `getInitialTimeInfo adds days to hijri date when adjustment is positive`() {
        val hijrahDate = HijrahDate.of(1448, 1, 1)

        val state = formatter.getInitialTimeInfo(zoneId, today, hijrahDate, hijriAdjustment = 5)

        assertThat(state.hijriDate).isEqualTo("06 Muharram 1448")
    }

    @Test
    fun `getInitialTimeInfo subtracts days from hijri date when adjustment is negative`() {
        val hijrahDate = HijrahDate.of(1448, 1, 5)

        val state = formatter.getInitialTimeInfo(zoneId, today, hijrahDate, hijriAdjustment = -3)

        assertThat(state.hijriDate).isEqualTo("02 Muharram 1448")
    }
}
