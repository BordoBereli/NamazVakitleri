package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.home.domain.CountdownEngine
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.domain.PrayerTimesLoader
import com.kutluoglu.prayer_feature.home.domain.QuranVerseLoader
import com.kutluoglu.prayer_feature.home.state.HomeScreenGate
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import com.kutluoglu.prayer_feature.home.state.QuranUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val locationsCoordinator: LocationsCoordinator = mockk(relaxed = true)
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

    private val entry = LocationEntry(
        id = "loc-1",
        location = location,
        displayName = "Istanbul, TR"
    )

    private fun loadedData(location: LocationData = this.location) = LoadedPrayerData(
        prayerState = PrayerUiState(),
        timeState = TimeUiState(),
        locationState = LocationUiState(location, "Istanbul, TR"),
        zoneId = ZoneId.of("Europe/Istanbul")
    )

    private fun viewModel() = HomeViewModel(
        locationsCoordinator,
        prayerTimesLoader,
        countdownEngine,
        quranVerseLoader
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { countdownEngine.prayerPassedSignal } returns kotlinx.coroutines.flow.MutableSharedFlow()
        every { countdownEngine.dayChangedSignal } returns kotlinx.coroutines.flow.MutableSharedFlow()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init resolves initial location and emits Ready when load succeeds`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `init loads prayer times exactly once`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()

        coVerify(exactly = 1) { prayerTimesLoader.load(location) }
    }

    @Test
    fun `refresh failure switches gate to Error`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns
            Result.failure(RuntimeException("fetch failed"))

        val vm = viewModel()
        assertThat(vm.screenGate.value is HomeScreenGate.Error).isTrue()
    }

    @Test
    fun `active location id is set even when active load fails`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns
            Result.failure(RuntimeException("fetch failed"))

        val vm = viewModel()

        assertThat(vm.activeLocationId.value).isEqualTo("loc-1")
        assertThat(vm.screenGate.value is HomeScreenGate.Error).isTrue()
    }

    @Test
    fun `onEvent OnRefresh triggers reload and resolves to Ready`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns null
        coEvery { locationsCoordinator.resolveSelected() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnRefresh)
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `onEvent OnVerseClicked toggles the sheet`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
        coEvery { locationsCoordinator.resolveInitial() } returns null
        every { quranVerseLoader.quranState } returns kotlinx.coroutines.flow.MutableStateFlow(QuranUiState())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnVerseClicked)
        vm.onEvent(HomeEvent.OnVerseDetailDismissed)
    }

    @Test
    fun `onEvent OnLocationSelected delegates to coordinator`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnLocationSelected("loc-1"))

        coVerify { locationsCoordinator.selectLocation("loc-1") }
    }

    @Test
    fun `location selection switches active location without reloading`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val entryA = LocationEntry("loc-1", locA, displayName = "Istanbul, Turkey")
        val entryB = LocationEntry("loc-2", locB, displayName = "Ankara, Turkey")
        val stateFlow = MutableStateFlow(
            LocationsState(entries = listOf(entryA, entryB), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.observeState() } returns stateFlow
        coEvery { locationsCoordinator.resolveInitial() } returns locA
        coEvery { prayerTimesLoader.load(locA) } returns success(loadedData(locA))
        coEvery { prayerTimesLoader.load(locB) } returns success(loadedData(locB))

        val vm = viewModel()
        coVerify(exactly = 1) { prayerTimesLoader.load(locB) } // pre-loaded once

        stateFlow.value = LocationsState(entries = listOf(entryA, entryB), selectedId = "loc-2")

        assertThat(vm.activeLocationId.value).isEqualTo("loc-2")
        coVerify(exactly = 1) { prayerTimesLoader.load(locB) } // still once — cache hit, no reload
    }

    @Test
    fun `location change for same id triggers reload`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val entryA = LocationEntry("loc-1", locA, displayName = "Istanbul, Turkey")
        val entryB = LocationEntry("loc-1", locB, displayName = "Ankara, Turkey")
        val stateFlow = MutableStateFlow(
            LocationsState(entries = listOf(entryA), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.observeState() } returns stateFlow
        coEvery { locationsCoordinator.resolveInitial() } returns locA
        coEvery { prayerTimesLoader.load(locA) } returns success(loadedData(locA))
        coEvery { prayerTimesLoader.load(locB) } returns success(loadedData(locB))

        val vm = viewModel()
        coVerify(exactly = 1) { prayerTimesLoader.load(locA) }

        stateFlow.value = LocationsState(entries = listOf(entryB), selectedId = "loc-1")

        coVerify { prayerTimesLoader.load(locB) }
    }

    @Test
    fun `empty locations state after initial emission shows error`() = runTest {
        val stateFlow = MutableStateFlow(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.observeState() } returns stateFlow
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        stateFlow.value = LocationsState()

        assertThat(vm.screenGate.value is HomeScreenGate.Error).isTrue()
    }

    @Test
    fun `all locations are pre-loaded on init`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val entryA = LocationEntry("loc-1", locA, displayName = "Istanbul, Turkey")
        val entryB = LocationEntry("loc-2", locB, displayName = "Ankara, Turkey")
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entryA, entryB), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns locA
        coEvery { prayerTimesLoader.load(locA) } returns success(loadedData())
        coEvery { prayerTimesLoader.load(locB) } returns success(loadedData())

        val vm = viewModel()

        assertThat(vm.prayerDataByLocation.value.keys).containsExactly("loc-1", "loc-2")
        assertThat(vm.activeLocationId.value).isEqualTo("loc-1")
    }

    @Test
    fun `prayerPassedSignal restarts countdown with recomputed prayer state`() = runTest {
        val prayerPassed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        every { countdownEngine.prayerPassedSignal } returns prayerPassed
        every { countdownEngine.dayChangedSignal } returns MutableSharedFlow()
        coEvery { prayerTimesLoader.load(location) } returns success(loadedData())

        val vm = viewModel()
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)

        val refreshed = PrayerUiState()
        coEvery { prayerTimesLoader.computePrayerState(any(), any()) } returns refreshed

        prayerPassed.emit(Unit)

        verify { countdownEngine.start(refreshed, ZoneId.of("Europe/Istanbul"), any()) }
    }
}
