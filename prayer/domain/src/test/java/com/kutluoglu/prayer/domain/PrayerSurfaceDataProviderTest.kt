package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.settings.AppSettings
import com.kutluoglu.prayer.settings.AppLocation
import com.kutluoglu.prayer.settings.SettingsProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PrayerSurfaceDataProviderTest {

    private fun prayer(name: String, time: LocalTime, date: LocalDate = LocalDate(2026, 10, 5)) =
        Prayer(name, name, time, date)

    private fun appSettings() = AppSettings(
        calculationMethod = "TURKEY_DIYANET",
        juristicMethod = "STANDARD",
        hijriAdjustment = 0,
        language = "system",
        lockPortrait = true,
        compassAutoRotate = true,
        location = AppLocation(
            latitude = 41.0082,
            longitude = 28.9784,
            cityName = "Istanbul",
            district = "Fatih",
            country = "Turkey",
            timeZone = "Europe/Istanbul"
        )
    )

    @Test
    fun `load returns surface data with location and prayers`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih")
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            DailyPrayerTimes(
                prayers = listOf(prayer("Imsak", LocalTime(5, 47)), prayer("Dhuhr", LocalTime(12, 58))),
                currentPrayer = prayer("Imsak", LocalTime(5, 47)),
                nextPrayer = prayer("Dhuhr", LocalTime(12, 58)),
                currentPrayerEpochMillis = 1L,
                nextPrayerEpochMillis = 2L,
                isJumuah = false
            )
        )

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings, clock)
        val result = provider.load()

        assertTrue(result != null)
        assertEquals("Istanbul", result!!.city)
        assertEquals("Fatih", result.district)
        assertEquals("Dhuhr", result.nextPrayerName)
        assertEquals(2L, result.nextPrayerEpochMillis)
        assertEquals(1L, result.currentPrayerEpochMillis)
        assertEquals(2, result.prayers.size)
        assertEquals(false, result.isJumuah)
    }

    @Test
    fun `load returns null when no location`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)

        coEvery { locationSource.resolveSelected() } returns null

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings)
        assertNull(provider.load())
    }

    @Test
    fun `load returns null when settings fail`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
        coEvery { settings.getSettings() } throws RuntimeException("boom")

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings)
        assertNull(provider.load())
    }

    @Test
    fun `load returns null when daily load fails`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(RuntimeException("network"))

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings)
        assertNull(provider.load())
    }
}
