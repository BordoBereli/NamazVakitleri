package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val locationCoordinator: LocationCoordinator = mockk(relaxed = true)
    private val prayerTimesLoader: PrayerTimesLoader = mockk(relaxed = true)
    private val countdownEngine: CountdownEngine = mockk(relaxed = true)
    private val quranVerseLoader: QuranVerseLoader = mockk(relaxed = true)

    private val location = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    private fun loadedData() = LoadedPrayerData(
        prayerState = PrayerUiState(),
        timeState = TimeUiState(),
        locationState = LocationUiState(location, "Istanbul, TR"),
        zoneId = ZoneId.of("Europe/Istanbul")
    )

    private fun viewModel() = HomeViewModel(
        locationCoordinator,
        prayerTimesLoader,
        countdownEngine,
        quranVerseLoader
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { countdownEngine.prayerPassedSignal } returns kotlinx.coroutines.flow.MutableSharedFlow()
        every { countdownEngine.dayChangedSignal } returns kotlinx.coroutines.flow.MutableSharedFlow()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init resolves initial location and emits Ready when load succeeds`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns location
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `refresh failure switches gate to Error`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns location
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { prayerTimesLoader.load(location) } returns
            Result.failure(RuntimeException("fetch failed"))

        val vm = viewModel()
        assertThat(vm.screenGate.value is HomeScreenGate.Error).isTrue()
    }

    @Test
    fun `onEvent OnRefresh triggers reload and resolves to Ready`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns null
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { locationCoordinator.resolveSavedAndDetectDrift() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnRefresh)
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `onEvent OnVerseClicked toggles the sheet`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns null
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        every { quranVerseLoader.quranState } returns kotlinx.coroutines.flow.MutableStateFlow(QuranUiState())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnVerseClicked)
        vm.onEvent(HomeEvent.OnVerseDetailDismissed)
    }
}
