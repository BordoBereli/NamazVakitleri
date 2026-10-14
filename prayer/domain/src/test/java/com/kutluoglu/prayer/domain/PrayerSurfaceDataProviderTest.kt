package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.settings.AppSettings
import com.kutluoglu.prayer.settings.AppLocation
import com.kutluoglu.prayer.settings.SettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
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
        // Epoch is recomputed from the effective next prayer (Dhuhr 12:58 in
        // Europe/Istanbul resolved from the TR country code), not passed through.
        val expectedEpoch = java.time.ZonedDateTime.of(2026, 10, 5, 12, 58, 0, 0, java.time.ZoneId.of("Europe/Istanbul"))
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedEpoch, result.nextPrayerEpochMillis)
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

    @Test
    fun `load after Isha exposes tomorrow's Imsak time`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        val todayResult = DailyPrayerTimes(
            prayers = listOf(prayer("Isha", LocalTime(19, 30))),
            currentPrayer = prayer("Isha", LocalTime(19, 30)),
            nextPrayer = prayer("Fajr", LocalTime(5, 47), date = LocalDate(2026, 10, 6)),
            currentPrayerEpochMillis = 1L,
            nextPrayerEpochMillis = 2L,
            isJumuah = false
        )
        val tomorrowResult = DailyPrayerTimes(
            prayers = listOf(Prayer("Imsak", "Imsak", LocalTime(5, 48), LocalDate(2026, 10, 6), isImsak = true)),
            currentPrayer = null,
            nextPrayer = null,
            currentPrayerEpochMillis = 0L,
            nextPrayerEpochMillis = 0L,
            isJumuah = false
        )

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih", timeZoneId = "Europe/Istanbul")
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(todayResult)
        coEvery {
            dailyLoader.load(match { it.date == LocalDate(2026, 10, 6) }, any(), any(), any(), any(), any(), any())
        } returns Result.success(tomorrowResult)

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings, clock)
        val result = provider.load()

        assertTrue(result != null)
        assertEquals(LocalTime(5, 48), result!!.tomorrowImsakTime)
        coVerify(exactly = 1) {
            dailyLoader.load(match { it.date == LocalDate(2026, 10, 6) }, any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `load after Isha patches next prayer time and epoch with tomorrow's exact Imsak`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        val todayResult = DailyPrayerTimes(
            prayers = listOf(prayer("Isha", LocalTime(19, 30))),
            currentPrayer = prayer("Isha", LocalTime(19, 30)),
            nextPrayer = Prayer("Imsak", "Imsak", LocalTime(5, 47), LocalDate(2026, 10, 6), isImsak = true),
            currentPrayerEpochMillis = 1L,
            nextPrayerEpochMillis = 2L,
            isJumuah = false
        )
        val tomorrowResult = DailyPrayerTimes(
            prayers = listOf(Prayer("Imsak", "Imsak", LocalTime(5, 48), LocalDate(2026, 10, 6), isImsak = true)),
            currentPrayer = null,
            nextPrayer = null,
            currentPrayerEpochMillis = 0L,
            nextPrayerEpochMillis = 0L,
            isJumuah = false
        )

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih", timeZoneId = "Europe/Istanbul")
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(todayResult)
        coEvery {
            dailyLoader.load(match { it.date == LocalDate(2026, 10, 6) }, any(), any(), any(), any(), any(), any())
        } returns Result.success(tomorrowResult)

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings, clock)
        val result = provider.load()

        assertTrue(result != null)
        // Next prayer time must be tomorrow's exact Imsak (05:48), not today's (05:47).
        assertEquals(LocalTime(5, 48), result!!.nextPrayerLocalTime)
        assertEquals("05:48", result.nextPrayerTime)
        val expectedEpoch = java.time.ZonedDateTime.of(2026, 10, 6, 5, 48, 0, 0, java.time.ZoneId.of("Europe/Istanbul"))
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedEpoch, result.nextPrayerEpochMillis)
    }

    @Test
    fun `load during the day skips tomorrow's Imsak load`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih", timeZoneId = "Europe/Istanbul")
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
        assertNull(result!!.tomorrowImsakTime)
        coVerify(exactly = 0) {
            dailyLoader.load(match { it.date == LocalDate(2026, 10, 6) }, any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `load after Isha keeps tomorrowImsakTime null when tomorrow load fails`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        val todayResult = DailyPrayerTimes(
            prayers = listOf(prayer("Isha", LocalTime(19, 30))),
            currentPrayer = prayer("Isha", LocalTime(19, 30)),
            nextPrayer = prayer("Fajr", LocalTime(5, 47), date = LocalDate(2026, 10, 6)),
            currentPrayerEpochMillis = 1L,
            nextPrayerEpochMillis = 2L,
            isJumuah = false
        )

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih", timeZoneId = "Europe/Istanbul")
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(todayResult)
        coEvery {
            dailyLoader.load(match { it.date == LocalDate(2026, 10, 6) }, any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("network"))

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings, clock)
        val result = provider.load()

        assertTrue(result != null)
        assertNull(result!!.tomorrowImsakTime)
    }

    @Test
    fun `load exposes current prayer name`() = runTest {
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
        assertEquals("Imsak", result!!.currentPrayerName)
    }
}
