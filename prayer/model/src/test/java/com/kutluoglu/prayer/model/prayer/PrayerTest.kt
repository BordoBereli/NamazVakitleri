package com.kutluoglu.prayer.model.prayer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrayerTest {

    @Test
    fun `prayer defaults to isImsak false`() {
        val prayer = Prayer("Fajr", "الفجر", LocalTime(5, 0), LocalDate(2026, 9, 2))
        assertFalse(prayer.isImsak)
    }

    @Test
    fun `prayer can be marked as imsak`() {
        val prayer = Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 9, 2), isImsak = true)
        assertTrue(prayer.isImsak)
    }
}
