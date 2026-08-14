package com.kutluoglu.prayer_feature.settings.location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class MyLocationsViewModelTest {

    private val coordinator = mockk<LocationsCoordinator>(relaxed = true)
    private lateinit var viewModel: MyLocationsViewModel

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { coordinator.observeState() } returns MutableStateFlow(
            LocationsState(entries = listOf(istanbul), gpsEnabled = false, selectedId = "loc-1")
        )
        viewModel = MyLocationsViewModel(coordinator)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes locations state`() = runTest {
        assertThat(viewModel.uiState.value.entries).hasSize(1)
        assertThat(viewModel.uiState.value.entries.first().id).isEqualTo("loc-1")
    }

    @Test
    fun `removeLocation delegates to coordinator`() = runTest {
        viewModel.onEvent(MyLocationsEvent.RemoveLocation("loc-1"))
        coVerify { coordinator.removeLocation("loc-1") }
    }

    @Test
    fun `setGpsEnabled delegates to coordinator`() = runTest {
        viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(true))
        coVerify { coordinator.setGpsEnabled(true) }
    }

    @Test
    fun `selectLocation delegates to coordinator`() = runTest {
        viewModel.onEvent(MyLocationsEvent.SelectLocation("loc-1"))
        coVerify { coordinator.selectLocation("loc-1") }
    }
}
