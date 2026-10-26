package com.kutluoglu.prayer_feature.settings.background

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@ExperimentalCoroutinesApi
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class BackgroundLocationsViewModelTest {

    private lateinit var locationsCoordinator: LocationsCoordinator
    private lateinit var backgroundImageStore: BackgroundImageStore
    private lateinit var viewModel: BackgroundLocationsViewModel

    private fun entry(
        id: String,
        displayName: String,
        backgroundPath: String? = null,
        isAutoGps: Boolean = false
    ) = LocationEntry(
        id = id,
        location = LocationData(
            latitude = 41.0,
            longitude = 29.0,
            country = "TR",
            countryCode = "TR",
            city = displayName,
            county = null
        ),
        isAutoGps = isAutoGps,
        displayName = displayName,
        backgroundPath = backgroundPath
    )

    @BeforeEach
    fun setUp() {
        locationsCoordinator = mockk()
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
        coEvery { locationsCoordinator.setBackgroundPath(any(), any()) } returns Unit
        backgroundImageStore = mockk(relaxed = true)
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)
    }

    @Test
    fun `locations state includes gps entry with its backgroundPath`() = runTest {
        // Arrange
        val state = LocationsState(
            entries = listOf(
                entry(
                    LocationsCoordinator.GPS_LOCATION_ID,
                    "Istanbul, Türkiye",
                    backgroundPath = "/data/gps.jpg",
                    isAutoGps = true
                ),
                entry("loc1", "Istanbul", backgroundPath = "/data/bg.jpg"),
                entry("loc2", "Ankara")
            ),
            gpsEnabled = true,
            selectedId = LocationsCoordinator.GPS_LOCATION_ID,
            gpsBackgroundPath = "/data/gps.jpg"
        )
        coEvery { locationsCoordinator.observeState() } returns flowOf(state)
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)

        // Act / Assert
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.locations.map { it.id })
                .containsExactly(LocationsCoordinator.GPS_LOCATION_ID, "loc1", "loc2").inOrder()
            assertThat(uiState.locations.first().backgroundPath).isEqualTo("/data/gps.jpg")
            assertThat(uiState.locations[1].backgroundPath).isEqualTo("/data/bg.jpg")
            assertThat(uiState.locations[1].displayName).isEqualTo("Istanbul")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locations state is empty when gps is disabled and nothing is saved`() = runTest {
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = emptyList(), gpsEnabled = false)
        )
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)

        viewModel.uiState.test {
            assertThat(awaitItem().locations).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savedBackgrounds exposes gallery file paths`() = runTest {
        // Arrange
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
        every { backgroundImageStore.listBackgrounds() } returns listOf(
            java.io.File("/data/other.jpg"),
            java.io.File("/data/bg.jpg")
        )
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)

        // Act / Assert
        // The refresh runs on a real IO dispatcher, so poll for the landed
        // value instead of racing turbine emissions.
        val uiState = withTimeout(2_000) {
            viewModel.uiState.first { it.savedBackgrounds.isNotEmpty() }
        }
        assertThat(uiState.savedBackgrounds).containsExactly("/data/other.jpg", "/data/bg.jpg")
    }

    @Test
    fun `ApplySavedBackground sets existing file path on location`() = runTest {
        // Arrange
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())

        // Act
        viewModel.onEvent(BackgroundLocationsEvent.ApplySavedBackground("loc1", "/data/other.jpg"))

        // Assert
        coVerify { locationsCoordinator.setBackgroundPath("loc1", "/data/other.jpg") }
    }

    @Test
    fun `ResetBackground clears the location path but keeps the file`() = runTest {
        // Arrange
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())

        // Act
        viewModel.onEvent(BackgroundLocationsEvent.ResetBackground("loc1"))

        // Assert
        coVerify { locationsCoordinator.setBackgroundPath("loc1", null) }
        coVerify(exactly = 0) { backgroundImageStore.deleteBackground(any()) }
    }

    @Test
    fun `DeleteBackground deletes file and clears path of locations referencing it`() = runTest {
        // Arrange: loc1 and loc2 both point at /data/bg.jpg
        val state = LocationsState(
            entries = listOf(
                entry("loc1", "Istanbul", backgroundPath = "/data/bg.jpg"),
                entry("loc2", "Ankara", backgroundPath = "/data/bg.jpg"),
                entry("loc3", "Izmir", backgroundPath = "/data/other.jpg")
            )
        )
        coEvery { locationsCoordinator.observeState() } returns flowOf(state)
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)

        // Act
        viewModel.onEvent(BackgroundLocationsEvent.DeleteBackground("/data/bg.jpg"))

        // Assert
        coVerify { backgroundImageStore.deleteBackground("/data/bg.jpg") }
        coVerify { locationsCoordinator.setBackgroundPath("loc1", null) }
        coVerify { locationsCoordinator.setBackgroundPath("loc2", null) }
        coVerify(exactly = 0) { locationsCoordinator.setBackgroundPath("loc3", null) }
    }

    @Test
    fun `DeleteAllBackgrounds deletes every file and clears all location paths`() = runTest {
        // Arrange
        val state = LocationsState(
            entries = listOf(
                entry("loc1", "Istanbul", backgroundPath = "/data/bg.jpg"),
                entry("loc2", "Ankara", backgroundPath = "/data/other.jpg"),
                entry("loc3", "Izmir")
            )
        )
        coEvery { locationsCoordinator.observeState() } returns flowOf(state)
        viewModel = BackgroundLocationsViewModel(locationsCoordinator, backgroundImageStore)

        // Act
        viewModel.onEvent(BackgroundLocationsEvent.DeleteAllBackgrounds)

        // Assert: the bulk delete clears every referencing location, the GPS
        // entry included, but leaves locations without a background untouched.
        verify(timeout = 2_000) { backgroundImageStore.deleteAllBackgrounds() }
        coVerify(timeout = 2_000) { locationsCoordinator.setBackgroundPath("loc1", null) }
        coVerify(timeout = 2_000) { locationsCoordinator.setBackgroundPath("loc2", null) }
        coVerify(timeout = 2_000) {
            locationsCoordinator.setBackgroundPath(LocationsCoordinator.GPS_LOCATION_ID, null)
        }
        coVerify(exactly = 0) { locationsCoordinator.setBackgroundPath("loc3", null) }
        coVerify(exactly = 0) { backgroundImageStore.deleteBackground(any()) }
    }
}
