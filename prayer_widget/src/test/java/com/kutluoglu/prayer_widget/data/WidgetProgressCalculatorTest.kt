package com.kutluoglu.prayer_widget.data

import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetProgressCalculatorTest {

    @Test
    fun `zero at current prayer time`() {
        val progress = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(12, 30),
            next = LocalTime(16, 0),
            now = LocalTime(12, 30)
        )
        assertEquals(0f, progress)
    }

    @Test
    fun `one at next prayer time`() {
        val progress = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(12, 30),
            next = LocalTime(16, 0),
            now = LocalTime(16, 0)
        )
        assertEquals(1f, progress)
    }

    @Test
    fun `halfway through interval`() {
        val progress = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(12, 30),
            next = LocalTime(16, 0),
            now = LocalTime(14, 15)
        )
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun `wraps past midnight`() {
        val progress = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(21, 0),
            next = LocalTime(5, 0),
            now = LocalTime(1, 0)
        )
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun `clamps out of range to zero or one`() {
        val before = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(12, 30), next = LocalTime(16, 0), now = LocalTime(10, 0)
        )
        val after = WidgetProgressCalculator.computeRingProgress(
            current = LocalTime(12, 30), next = LocalTime(16, 0), now = LocalTime(18, 0)
        )
        assertEquals(0f, before)
        assertEquals(1f, after)
    }
}
