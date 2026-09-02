package com.kutluoglu.prayer_feature.common.prayerUtils

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.every
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
    fun `withLocalizedNames localizes six prayers including imsak`() {
        val resourcesProvider = mockk<ResourcesProvider>(relaxed = true)
        every { resourcesProvider.getStringArray(any()) } returns
            arrayOf("Imsak", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        val formatter = PrayerFormatter(resourcesProvider)
        val prayers = listOf(
            Prayer("Imsak", "الإمساك", kotlinx.datetime.LocalTime(4, 50), kotlinx.datetime.LocalDate(2026, 9, 2), isImsak = true),
            Prayer("Sunrise", "الشروق", kotlinx.datetime.LocalTime(6, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Dhuhr", "الظهر", kotlinx.datetime.LocalTime(12, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Asr", "العصر", kotlinx.datetime.LocalTime(15, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Maghrib", "المغرب", kotlinx.datetime.LocalTime(18, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Isha", "العشاء", kotlinx.datetime.LocalTime(20, 30), kotlinx.datetime.LocalDate(2026, 9, 2))
        )

        val localized = formatter.withLocalizedNames(prayers)

        assertThat(localized.map { it.name })
            .containsExactly("Imsak", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
            .inOrder()
    }

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
