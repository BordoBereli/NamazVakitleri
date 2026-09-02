package com.kutluoglu.prayer_notifications.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SpecialDaysCalculatorExtendedTest {

    private val calculator = SpecialDaysCalculator()

    @Test
    fun `detects islamic new year`() {
        assertEquals(SpecialDay.ISLAMIC_NEW_YEAR, calculator.specialDayFor(LocalDate.of(2025, 6, 26)))
    }

    @Test
    fun `detects ashura`() {
        assertEquals(SpecialDay.ASHURA, calculator.specialDayFor(LocalDate.of(2025, 7, 5)))
    }

    @Test
    fun `detects isra miraj`() {
        assertEquals(SpecialDay.ISRA_MIRAJ, calculator.specialDayFor(LocalDate.of(2026, 1, 16)))
    }

    @Test
    fun `detects mid shaban`() {
        assertEquals(SpecialDay.MID_SHABAN, calculator.specialDayFor(LocalDate.of(2026, 2, 3)))
    }

    @Test
    fun `detects mawlid`() {
        assertEquals(SpecialDay.MAWLID_AL_NABI, calculator.specialDayFor(LocalDate.of(2025, 9, 4)))
    }

    @Test
    fun `detects arafah`() {
        assertEquals(SpecialDay.ARAFAH, calculator.specialDayFor(LocalDate.of(2026, 5, 26)))
    }

    @Test
    fun `day before and after special day return null`() {
        assertNull(calculator.specialDayFor(LocalDate.of(2025, 7, 4)))
        assertNull(calculator.specialDayFor(LocalDate.of(2025, 7, 6)))
    }

    @Test
    fun `hijri adjustment shifts detected day`() {
        assertEquals(SpecialDay.ASHURA, calculator.specialDayFor(LocalDate.of(2025, 7, 4), hijriAdjustment = 1))
    }
}
