package com.kutluoglu.prayer_widget.data

import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class WidgetDataProviderTest {

    private fun prayer(name: String, time: LocalTime) =
        Prayer(name, name, time, LocalDate(2026, 9, 2))

    private fun countdownFormatter() = mockk<WidgetCountdownFormatter>(relaxed = true)

    private suspend fun withSystemDefaultZone(zoneId: String, block: suspend () -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        try {
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `load returns next prayer and countdown`() = runTest {
        withSystemDefaultZone("Europe/Istanbul") {
            val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
            val locations = mockk<LocationsCoordinator>(relaxed = true)
            val settings = mockk<GetSettingsUseCase>(relaxed = true)
            val calculator = PrayerLogicEngine(Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC))
            val formatter = mockk<com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter>(relaxed = true)
            val countdown = countdownFormatter()

            coEvery { locations.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
            coEvery { settings() } returns Settings()
            coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
                listOf(
                    prayer("Dhuhr", LocalTime(12, 30)),
                    prayer("Maghrib", LocalTime(19, 30)),
                    prayer("Asr", LocalTime(16, 0))
                )
            )
            coEvery { formatter.withLocalizedNames(any()) } returns listOf(
                prayer("Dhuhr", LocalTime(12, 30)),
                prayer("Maghrib", LocalTime(19, 30)),
                prayer("Asr", LocalTime(16, 0))
            )
            coEvery { countdown.format(any(), any()) } returns "2s 15d"

            val provider = WidgetDataProvider(useCase, locations, settings, calculator, formatter, countdown)
            val result = provider.load()
            assertTrue(result is WidgetResult.Success)
            val data = (result as WidgetResult.Success).data
            assertEquals("Dhuhr", data.nextPrayerName)
            assertEquals("Istanbul", data.locationName)
            assertEquals("2s 15d", data.countdownText)
        }
    }

    @Test
    fun `load computes ring progress between current and next prayer`() = runTest {
        withSystemDefaultZone("Europe/Istanbul") {
            val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
            val locations = mockk<LocationsCoordinator>(relaxed = true)
            val settings = mockk<GetSettingsUseCase>(relaxed = true)
            val clock = Clock.fixed(Instant.parse("2026-09-02T11:15:00Z"), ZoneOffset.UTC)
            val calculator = PrayerLogicEngine(clock)
            val formatter = mockk<com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter>(relaxed = true)
            val countdown = countdownFormatter()

            coEvery { locations.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
            coEvery { settings() } returns Settings()
            coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
                listOf(
                    prayer("Dhuhr", LocalTime(12, 30)),
                    prayer("Asr", LocalTime(16, 0))
                )
            )
            coEvery { formatter.withLocalizedNames(any()) } returns listOf(
                prayer("Dhuhr", LocalTime(12, 30)),
                prayer("Asr", LocalTime(16, 0))
            )
            coEvery { countdown.format(any(), any()) } returns "2s 15d"

            val provider = WidgetDataProvider(useCase, locations, settings, calculator, formatter, countdown, clock)
            val result = provider.load()
            assertTrue(result is WidgetResult.Success)
            val data = (result as WidgetResult.Success).data
            // 11:15 UTC = 14:15 Istanbul; Dhuhr 12:30 -> Asr 16:00 is 50% through
            assertEquals(0.5f, data.ringProgress, 0.01f)
        }
    }

    @Test
    fun `load returns error when no location`() = runTest {
        val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
        val locations = mockk<LocationsCoordinator>(relaxed = true)
        val settings = mockk<GetSettingsUseCase>(relaxed = true)
        val calculator = mockk<PrayerLogicEngine>(relaxed = true)
        val formatter = mockk<com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter>(relaxed = true)
        val countdown = countdownFormatter()

        coEvery { locations.resolveSelected() } returns null

        val provider = WidgetDataProvider(useCase, locations, settings, calculator, formatter, countdown)
        val result = provider.load()
        assertTrue(result is WidgetResult.Error)
    }

    @Test
    fun `load resolves zone from location and skips daily cache persist`() = runTest {
        withSystemDefaultZone("Europe/Berlin") {
            val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
            val locations = mockk<LocationsCoordinator>(relaxed = true)
            val settings = mockk<GetSettingsUseCase>(relaxed = true)
            val calculator = mockk<PrayerLogicEngine>(relaxed = true)
            val formatter = mockk<com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter>(relaxed = true)
            val countdown = countdownFormatter()

            coEvery { locations.resolveSelected() } returns LocationData(41.0, 29.0, "United States", "US", "New York", null)
            coEvery { settings() } returns Settings()
            coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
                listOf(prayer("Dhuhr", LocalTime(12, 30)))
            )
            coEvery { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(
                prayer("Dhuhr", LocalTime(12, 30)),
                prayer("Dhuhr", LocalTime(12, 30))
            )
            coEvery { formatter.withLocalizedNames(any()) } returns listOf(prayer("Dhuhr", LocalTime(12, 30)))

            val provider = WidgetDataProvider(useCase, locations, settings, calculator, formatter, countdown)
            provider.load()

            val expectedZone = getZoneIdFromLocation("US")
            assertNotEquals(ZoneId.of("Europe/Berlin"), expectedZone)
            coVerify {
                useCase.invoke(any(), any(), any(), eq(expectedZone), any(), any(), eq(false))
            }
        }
    }

    @Test
    fun `load forwards juristic method from settings to use case`() = runTest {
        withSystemDefaultZone("Europe/Istanbul") {
            val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
            val locations = mockk<LocationsCoordinator>(relaxed = true)
            val settings = mockk<GetSettingsUseCase>(relaxed = true)
            val calculator = mockk<PrayerLogicEngine>(relaxed = true)
            val formatter = mockk<com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter>(relaxed = true)
            val countdown = countdownFormatter()

            coEvery { locations.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
            coEvery { settings() } returns Settings(juristicMethod = "HANAFI")
            coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
                listOf(prayer("Dhuhr", LocalTime(12, 30)))
            )
            coEvery { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(
                prayer("Dhuhr", LocalTime(12, 30)),
                prayer("Dhuhr", LocalTime(12, 30))
            )
            coEvery { formatter.withLocalizedNames(any()) } returns listOf(prayer("Dhuhr", LocalTime(12, 30)))

            val provider = WidgetDataProvider(useCase, locations, settings, calculator, formatter, countdown)
            provider.load()

            coVerify {
                useCase.invoke(any(), any(), any(), any(), any(), JuristicMethod.HANAFI, any())
            }
        }
    }
}
