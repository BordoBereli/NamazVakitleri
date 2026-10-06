package com.kutluoglu.prayer_widget.data

import com.kutluoglu.core.designsystem.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer.domain.SurfacePrayerData
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class WidgetDataProviderTest {

    private fun surfaceData(
        city: String = "Istanbul",
        nextPrayerName: String = "Dhuhr",
        nextPrayerTime: String = "12:30",
        nextEpoch: Long = 1_700_000_000_000L,
        currentEpoch: Long = 1_699_999_000_000L,
        isJumuah: Boolean = false
    ) = SurfacePrayerData(
        city = city,
        district = "Fatih",
        nextPrayerName = nextPrayerName,
        nextPrayerTime = nextPrayerTime,
        nextPrayerLocalTime = LocalTime(12, 30),
        currentPrayerLocalTime = LocalTime(5, 47),
        nextPrayerEpochMillis = nextEpoch,
        currentPrayerEpochMillis = currentEpoch,
        isJumuah = isJumuah,
        hijriAdjustment = 0,
        location = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih", "Europe/Istanbul"),
        prayers = listOf(
            Prayer("Imsak", "Imsak", LocalTime(5, 47), LocalDate(2026, 10, 5)),
            Prayer(nextPrayerName, nextPrayerName, LocalTime(12, 30), LocalDate(2026, 10, 5))
        ),
        nextPrayerDate = LocalDate(2026, 10, 5)
    )

    @Test
    fun `load maps surface data to widget data`() = runTest {
        val surfaceProvider = mockk<PrayerSurfaceDataProvider>(relaxed = true)
        val formatter = mockk<PrayerFormatter>(relaxed = true)
        val countdown = mockk<WidgetCountdownFormatter>(relaxed = true)
        val calculator = mockk<PrayerLogicEngine>(relaxed = true)
        coEvery { surfaceProvider.load() } returns surfaceData()
        every { countdown.format(any(), any()) } returns "2s 15d"
        every { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ofHours(2)
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns mockk(relaxed = true)

        val provider = WidgetDataProvider(surfaceProvider, formatter, countdown, calculator)
        val result = provider.load()

        assertTrue(result is WidgetResult.Success)
        val data = (result as WidgetResult.Success).data
        assertEquals("Dhuhr", data.nextPrayerName)
        assertEquals("Istanbul", data.locationName)
        assertEquals("2s 15d", data.countdownText)
    }

    @Test
    fun `load returns error when surface data is null`() = runTest {
        val surfaceProvider = mockk<PrayerSurfaceDataProvider>(relaxed = true)
        val formatter = mockk<PrayerFormatter>(relaxed = true)
        val countdown = mockk<WidgetCountdownFormatter>(relaxed = true)
        val calculator = mockk<PrayerLogicEngine>(relaxed = true)
        coEvery { surfaceProvider.load() } returns null

        val provider = WidgetDataProvider(surfaceProvider, formatter, countdown, calculator)
        val result = provider.load()

        assertTrue(result is WidgetResult.Error)
    }
}
