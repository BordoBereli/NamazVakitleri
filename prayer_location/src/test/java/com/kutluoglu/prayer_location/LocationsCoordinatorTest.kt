package com.kutluoglu.prayer_location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.data.LocationsDataStore
import com.kutluoglu.prayer_location.data.LocationsMigration
import com.kutluoglu.prayer_location.data.LocationsState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LocationsCoordinatorTest {

    private val dataStore = mockk<LocationsDataStore>(relaxed = true)
    private val locationService = mockk<LocationService>(relaxed = true)
    private val migration = mockk<LocationsMigration>(relaxed = true)
    private val provider = ActiveLocationProvider()
    private val coordinator = LocationsCoordinator(dataStore, locationService, provider, migration)

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )

    @Test
    fun `observeState includes synthetic gps entry when enabled`() = runBlocking<Unit> {
        val gps = LocationData(40.0, 29.0, "Turkey", "TR", "Bursa", null)
        coEvery { dataStore.observeLocations() } returns MutableStateFlow(
            LocationsState(entries = listOf(istanbul), gpsEnabled = true)
        )
        coordinator.setGpsLocation(gps)

        val state = coordinator.observeState().first()
        assertThat(state.entries.first().isAutoGps).isTrue()
        assertThat(state.entries.first().location).isEqualTo(gps)
        assertThat(state.entries).hasSize(2)
    }

    @Test
    fun `observeState omits gps entry when disabled`() = runBlocking<Unit> {
        coEvery { dataStore.observeLocations() } returns MutableStateFlow(
            LocationsState(entries = listOf(istanbul), gpsEnabled = false)
        )

        val state = coordinator.observeState().first()
        assertThat(state.entries).hasSize(1)
        assertThat(state.entries.first().isAutoGps).isFalse()
    }

    @Test
    fun `resolveInitial returns selected location and updates provider`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState(
            entries = listOf(istanbul),
            selectedId = "loc-1"
        )

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(istanbul.location)
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }

    @Test
    fun `resolveInitial falls back to first entry when no selection`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(istanbul.location)
    }

    @Test
    fun `resolveInitial returns gps when no manual entries and gps enabled`() = runBlocking<Unit> {
        val gps = LocationData(40.0, 29.0, "Turkey", "TR", "Bursa", null)
        coEvery { dataStore.getLocations() } returns LocationsState(gpsEnabled = true)
        coEvery { locationService.getCurrentLocation() } returns gps

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(gps)
    }

    @Test
    fun `resolveInitial returns null when nothing resolvable`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState()

        val result = coordinator.resolveInitial()

        assertThat(result).isNull()
    }

    @Test
    fun `selectLocation persists selection and updates provider`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))

        coordinator.selectLocation("loc-1")

        coVerify { dataStore.setSelectedLocation("loc-1") }
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }

    @Test
    fun `addLocation sets as selected when no selection exists`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState()

        coordinator.addLocation(istanbul)

        coVerify { dataStore.addLocation(istanbul) }
        coVerify { dataStore.setSelectedLocation("loc-1") }
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }

    @Test
    fun `resolveInitial runs migration first`() = runBlocking<Unit> {
        coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))

        coordinator.resolveInitial()

        coVerify { migration.migrateIfNeeded() }
    }
}
