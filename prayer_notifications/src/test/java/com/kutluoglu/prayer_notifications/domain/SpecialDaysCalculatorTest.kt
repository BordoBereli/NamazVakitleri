package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SpecialDaysCalculatorTest {

    private val calculator = SpecialDaysCalculator()

    @Test
    fun `detects Ramadan start`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 18)))
            .isEqualTo(SpecialDay.RAMADAN_START)
    }

    @Test
    fun `detects Laylat al-Qadr`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 3, 16)))
            .isEqualTo(SpecialDay.LAYLAT_AL_QADIR)
    }

    @Test
    fun `detects Eid al-Fitr`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 3, 20)))
            .isEqualTo(SpecialDay.EID_AL_FITR)
    }

    @Test
    fun `detects Eid al-Adha`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 5, 27)))
            .isEqualTo(SpecialDay.EID_AL_ADHA)
    }

    @Test
    fun `returns null for ordinary days`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 6, 15))).isNull()
    }

    @Test
    fun `applies hijri adjustment`() {
        // 2026-02-18 is 1 Ramadan 1447. +1 day -> 2 Ramadan (not special).
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 18), hijriAdjustment = 1)).isNull()
        // 2026-02-17 is 29 Sha'ban 1447. +1 day -> 1 Ramadan 1447.
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 17), hijriAdjustment = 1))
            .isEqualTo(SpecialDay.RAMADAN_START)
    }
}
