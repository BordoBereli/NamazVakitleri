package com.kutluoglu.prayer_feature.home.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RamadanDateResolverTest {

    private val resolver = RamadanDateResolver()

    @Test
    fun `returns 1 on first day of ramadan`() {
        assertEquals(1, resolver.ramadanDayFor(LocalDate.of(2026, 2, 18)))
    }

    @Test
    fun `returns null outside ramadan`() {
        assertNull(resolver.ramadanDayFor(LocalDate.of(2026, 3, 21)))
    }

    @Test
    fun `honors hijri adjustment`() {
        assertEquals(1, resolver.ramadanDayFor(LocalDate.of(2026, 2, 17), hijriAdjustment = 1))
    }
}
