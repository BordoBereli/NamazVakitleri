package com.kutluoglu.prayer_location.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LocationsMigrationTest {

    private val legacyStore = mockk<LocationDataStore>(relaxed = true)
    private val locationsStore = mockk<LocationsDataStore>(relaxed = true)

    @Test
    fun `does nothing when locations already exist`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState(
            entries = listOf(
                LocationEntry("x", LocationData(1.0, 2.0, "A", "AA", "City", null), displayName = "City")
            )
        )

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { legacyStore.getSavedLocation() }
    }

    @Test
    fun `migrates legacy saved location when list is empty`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState()
        coEvery { legacyStore.getSavedLocation() } returns LocationDataModel(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify { locationsStore.addLocation(any()) }
    }

    @Test
    fun `does nothing when no legacy location exists`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState()
        coEvery { legacyStore.getSavedLocation() } returns null

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { locationsStore.addLocation(any()) }
    }
}
