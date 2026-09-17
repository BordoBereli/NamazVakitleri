package com.kutluoglu.wear.shared.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WatchCountdownCalculatorTest {

    private val hourShort = "s"
    private val minuteShort = "d"

    @Test
    fun `countdownText formats hours and minutes`() {
        val next = 1_700_000_000_000L
        val now = next - (2 * 3_600_000L + 14 * 60_000L)

        assertThat(WatchCountdownCalculator.countdownText(next, now, hourShort, minuteShort))
            .isEqualTo("2s 14d")
    }

    @Test
    fun `countdownText formats only hours`() {
        val next = 1_700_000_000_000L
        val now = next - 3 * 3_600_000L

        assertThat(WatchCountdownCalculator.countdownText(next, now, hourShort, minuteShort))
            .isEqualTo("3s")
    }

    @Test
    fun `countdownText formats only minutes`() {
        val next = 1_700_000_000_000L
        val now = next - 45 * 60_000L

        assertThat(WatchCountdownCalculator.countdownText(next, now, hourShort, minuteShort))
            .isEqualTo("45d")
    }

    @Test
    fun `countdownText clamps to zero when past`() {
        val next = 1_700_000_000_000L
        val now = next + 5 * 60_000L

        assertThat(WatchCountdownCalculator.countdownText(next, now, hourShort, minuteShort))
            .isEqualTo("0d")
    }

    @Test
    fun `ringProgress is zero at start`() {
        val current = 1_699_999_000_000L
        val next = 1_700_000_000_000L

        assertThat(WatchCountdownCalculator.ringProgress(current, next, current)).isEqualTo(0f)
    }

    @Test
    fun `ringProgress is one at end`() {
        val current = 1_699_999_000_000L
        val next = 1_700_000_000_000L

        assertThat(WatchCountdownCalculator.ringProgress(current, next, next)).isEqualTo(1f)
    }

    @Test
    fun `ringProgress is half way`() {
        val current = 1_699_999_000_000L
        val next = 1_700_000_000_000L
        val now = current + (next - current) / 2

        assertThat(WatchCountdownCalculator.ringProgress(current, next, now)).isEqualTo(0.5f)
    }

    @Test
    fun `ringProgress clamps when now outside range`() {
        val current = 1_699_999_000_000L
        val next = 1_700_000_000_000L

        assertThat(WatchCountdownCalculator.ringProgress(current, next, current - 60_000L)).isEqualTo(0f)
        assertThat(WatchCountdownCalculator.ringProgress(current, next, next + 60_000L)).isEqualTo(1f)
    }
}
