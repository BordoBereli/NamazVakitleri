package com.kutluoglu.prayer_feature.qibla

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.qibla.QiblaState
import com.kutluoglu.prayer.usecases.qibla.CalculateQiblaUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.ObserveSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCompassAutoRotateUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLockPortraitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {

    private val calculateQiblaUseCase = mockk<CalculateQiblaUseCase>(relaxed = true)
    private val provider = ActiveLocationProvider()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val formatter = mockk<PrayerFormatter>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val observeSettingsUseCase = mockk<ObserveSettingsUseCase>(relaxed = true)
    private val updateLockPortraitUseCase = mockk<UpdateLockPortraitUseCase>(relaxed = true)
    private val updateCompassAutoRotateUseCase = mockk<UpdateCompassAutoRotateUseCase>(relaxed = true)
    private lateinit var viewModel: QiblaViewModel

    private val location = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { formatter.locationInfo(any()) } answers {
            val loc = firstArg<LocationData>()
            "${loc.city ?: ""}, ${loc.countryCode ?: ""}"
        }

        coEvery { getSettingsUseCase() } returns Settings()
        every { observeSettingsUseCase() } returns MutableStateFlow(Settings())

        Dispatchers.setMain(UnconfinedTestDispatcher())
        provider.set(location)
        viewModel = QiblaViewModel(
            provider,
            calculateQiblaUseCase,
            analyticsTracker,
            formatter,
            getSettingsUseCase,
            observeSettingsUseCase,
            updateLockPortraitUseCase,
            updateCompassAutoRotateUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `OnStart observes qibla direction for the active location`() = runTest {
        val qiblaState = QiblaState(
            qiblaBearing = 150.0,
            deviceAzimuth = 10.0f,
            qiblaAngle = 140.0f,
            sensorAccuracy = 3
        )
        coEvery { calculateQiblaUseCase.observeQiblaDirection(location.latitude, location.longitude) } returns
            MutableStateFlow(qiblaState)

        viewModel.onEvent(QiblaEvent.OnStart)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.qiblaBearing).isEqualTo(150.0)
            assertThat(state.isLocationAvailable).isTrue()
            assertThat(state.locationName).isEqualTo("Istanbul, TR")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnStart with no active location shows error`() = runTest {
        provider.set(null)

        viewModel.onEvent(QiblaEvent.OnStart)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLocationAvailable).isFalse()
            assertThat(state.error).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `qibla follows active location changes`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val stateA = QiblaState(qiblaBearing = 150.0, deviceAzimuth = 10.0f, qiblaAngle = 140.0f, sensorAccuracy = 3)
        val stateB = QiblaState(qiblaBearing = 160.0, deviceAzimuth = 20.0f, qiblaAngle = 140.0f, sensorAccuracy = 3)
        coEvery { calculateQiblaUseCase.observeQiblaDirection(locA.latitude, locA.longitude) } returns
            MutableStateFlow(stateA)
        coEvery { calculateQiblaUseCase.observeQiblaDirection(locB.latitude, locB.longitude) } returns
            MutableStateFlow(stateB)

        provider.set(locA)
        viewModel.onEvent(QiblaEvent.OnStart)

        viewModel.uiState.test {
            val first = awaitItem()
            assertThat(first.qiblaBearing).isEqualTo(150.0)
            assertThat(first.locationName).isEqualTo("Istanbul, TR")

            provider.set(locB)
            val second = awaitItem()
            assertThat(second.qiblaBearing).isEqualTo(160.0)
            assertThat(second.locationName).isEqualTo("Ankara, TR")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnStart loads default preferences lockPortrait true compassAutoRotate true`() = runTest {
        val qiblaState = QiblaState(
            qiblaBearing = 150.0,
            deviceAzimuth = 10.0f,
            qiblaAngle = 140.0f,
            sensorAccuracy = 3
        )
        coEvery { calculateQiblaUseCase.observeQiblaDirection(location.latitude, location.longitude) } returns
            MutableStateFlow(qiblaState)

        viewModel.onEvent(QiblaEvent.OnStart)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.lockPortrait).isTrue()
            assertThat(state.compassAutoRotate).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ToggleLockPortrait writes false to use case`() = runTest {
        viewModel.onEvent(QiblaEvent.ToggleLockPortrait)
        coVerify { updateLockPortraitUseCase(false) }
    }

    @Test
    fun `ToggleCompassAutoRotate writes false to use case`() = runTest {
        viewModel.onEvent(QiblaEvent.ToggleCompassAutoRotate)
        coVerify { updateCompassAutoRotateUseCase(false) }
    }
}
