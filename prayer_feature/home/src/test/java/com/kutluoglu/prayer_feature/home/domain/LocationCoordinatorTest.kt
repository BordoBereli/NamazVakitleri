package com.kutluoglu.prayer_feature.home.domain

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.ObserveLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_location.LocationService
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCoordinatorTest {

    private val settingsRepository: SettingsRepository = mockk()
    private val getSavedLocationUseCase: GetSavedLocationUseCase = mockk()
    private val saveLocationUseCase: SaveLocationUseCase = mockk()
    private val observeLocationUseCase: ObserveLocationUseCase = mockk()
    private val locationService: LocationService = mockk()

    private val gpsLocation = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
    private val savedLocation = LocationData(41.1, 29.1, "Turkey", "TR", "Istanbul", null)

    private val testSettings = Settings(
        location = LocationSettings(
            latitude = 41.0,
            longitude = 29.0,
            cityName = "Istanbul",
            district = null,
            country = "Turkey",
            timeZone = "Europe/Istanbul"
        )
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    private fun coordinator() = LocationCoordinator(
        settingsRepository = settingsRepository,
        getSavedLocationUseCase = getSavedLocationUseCase,
        saveLocationUseCase = saveLocationUseCase,
        observeLocationUseCase = observeLocationUseCase,
        locationService = locationService
    )

    @Test
    fun `resolveInitial prefers settings location`() = runTest {
        coEvery { settingsRepository.getSettings() } returns testSettings
        val result = coordinator().resolveInitial()
        assertThat(result?.city).isEqualTo("Istanbul")
        assertThat(result?.countryCode).isEqualTo("TR")
    }

    @Test
    fun `resolveInitial falls back to saved location when settings throw`() = runTest {
        coEvery { settingsRepository.getSettings() } throws RuntimeException("no settings")
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        val result = coordinator().resolveInitial()
        assertThat(result).isEqualTo(savedLocation)
    }

    @Test
    fun `resolveInitial falls back to GPS when settings and saved both fail`() = runTest {
        coEvery { settingsRepository.getSettings() } throws RuntimeException("no settings")
        coEvery { getSavedLocationUseCase() } returns Result.failure(RuntimeException("no saved"))
        coEvery { locationService.getCurrentLocation() } returns gpsLocation
        coEvery { saveLocationUseCase.invoke(gpsLocation) } returns Unit

        val result = coordinator().resolveInitial()

        assertThat(result).isEqualTo(gpsLocation)
        coVerify { saveLocationUseCase.invoke(gpsLocation) }
    }

    @Test
    fun `observeSettingsChanges maps LocationSettings to LocationData`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(testSettings, testSettings)
        coordinator().observeSettingsChanges().test {
            val first = awaitItem()
            assertThat(first.countryCode).isEqualTo("TR")
            assertThat(first.city).isEqualTo("Istanbul")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLocationChanges relays location service values`() = runTest {
        every { observeLocationUseCase() } returns flowOf(savedLocation)
        coordinator().observeLocationChanges().test {
            assertThat(awaitItem()).isEqualTo(savedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resolveSavedAndDetectDrift returns saved location and does not set prompt when same`() = runTest {
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        coEvery { locationService.getCurrentLocation() } returns savedLocation
        every { locationService.isDifferentThen(savedLocation) } returns false

        val result = coordinator().resolveSavedAndDetectDrift()

        assertThat(result).isEqualTo(savedLocation)
        assertThat(coordinator().locationUpdatePrompt.value).isFalse()
    }

    @Test
    fun `resolveSavedAndDetectDrift sets prompt when GPS differs`() = runTest {
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        coEvery { locationService.getCurrentLocation() } returns gpsLocation
        every { locationService.isDifferentThen(savedLocation) } returns true

        val coordinator = coordinator()
        coordinator.resolveSavedAndDetectDrift()

        assertThat(coordinator.locationUpdatePrompt.value).isTrue()
    }
}
