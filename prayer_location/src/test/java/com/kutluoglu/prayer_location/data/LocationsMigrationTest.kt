package com.kutluoglu.prayer_location.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationDataModel
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationDataStore
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LocationsMigrationTest {

    private val legacyStore = mockk<LegacyLocationDataStore>(relaxed = true)
    private val locationsStore = mockk<LocationsDataStore>(relaxed = true)

    @Test
    fun `does nothing when locations already exist and are migrated`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState(
            entries = listOf(
                LocationEntry(
                    id = "x",
                    location = LocationData(1.0, 2.0, "A", "AA", "City", null, "Europe/Istanbul"),
                    displayName = "City"
                )
            )
        )

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { legacyStore.getSavedLocation() }
        coVerify(exactly = 0) { locationsStore.replaceAll(any()) }
    }

    @Test
    fun `migrates legacy saved location when list is empty and backfills timeZoneId`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState()
        coEvery { legacyStore.getSavedLocation() } returns LegacyLocationDataModel(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )

        val slot = slot<LocationEntry>()
        coEvery { locationsStore.addLocation(capture(slot)) } returns Unit

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        val entry = slot.captured
        assertThat(entry.isAutoGps).isFalse()
        assertThat(entry.location.latitude).isEqualTo(41.0082)
        assertThat(entry.location.longitude).isEqualTo(28.9784)
        assertThat(entry.location.city).isEqualTo("Istanbul")
        assertThat(entry.location.country).isEqualTo("Turkey")
        assertThat(entry.location.timeZoneId).isEqualTo("Europe/Istanbul")
        assertThat(entry.displayName).isEqualTo("Istanbul, Turkey")
        assertThat(entry.id).isNotEmpty()
    }

    @Test
    fun `does nothing when no legacy location exists`() = runBlocking<Unit> {
        coEvery { locationsStore.getLocations() } returns LocationsState()
        coEvery { legacyStore.getSavedLocation() } returns null

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { locationsStore.addLocation(any()) }
    }

    @Test
    fun `backfills timeZoneId for entries missing it`() = runBlocking<Unit> {
        val entry = LocationEntry(
            id = "1",
            location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            displayName = "Istanbul, Turkey"
        )
        coEvery { locationsStore.getLocations() } returns LocationsState(entries = listOf(entry))

        val slot = slot<List<LocationEntry>>()
        coEvery { locationsStore.replaceAll(capture(slot)) } returns Unit

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        val updated = slot.captured
        assertThat(updated).hasSize(1)
        assertThat(updated[0].id).isEqualTo("1")
        assertThat(updated[0].location.timeZoneId).isEqualTo("Europe/Istanbul")
        assertThat(updated[0].location.latitude).isEqualTo(41.0082)
        assertThat(updated[0].location.longitude).isEqualTo(28.9784)
        assertThat(updated[0].location.country).isEqualTo("Turkey")
        assertThat(updated[0].location.countryCode).isEqualTo("TR")
        assertThat(updated[0].location.city).isEqualTo("Istanbul")
        assertThat(updated[0].displayName).isEqualTo("Istanbul, Turkey")
        coVerify(exactly = 0) { legacyStore.getSavedLocation() }
    }

    @Test
    fun `does not rewrite when all entries already have timeZoneId`() = runBlocking<Unit> {
        val entry = LocationEntry(
            id = "1",
            location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null, "Europe/Istanbul"),
            displayName = "Istanbul, Turkey"
        )
        coEvery { locationsStore.getLocations() } returns LocationsState(entries = listOf(entry))

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { locationsStore.replaceAll(any()) }
        coVerify(exactly = 0) { legacyStore.getSavedLocation() }
    }

    @Test
    fun `backfills only entries missing timeZoneId and preserves set ones`() = runBlocking<Unit> {
        val trEntry = LocationEntry(
            id = "1",
            location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            displayName = "Istanbul, Turkey"
        )
        val deEntry = LocationEntry(
            id = "2",
            location = LocationData(52.52, 13.405, "Germany", "DE", "Berlin", null, "Europe/Berlin"),
            displayName = "Berlin, Germany"
        )
        coEvery { locationsStore.getLocations() } returns LocationsState(entries = listOf(trEntry, deEntry))

        val slot = slot<List<LocationEntry>>()
        coEvery { locationsStore.replaceAll(capture(slot)) } returns Unit

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        val updated = slot.captured
        assertThat(updated).hasSize(2)
        assertThat(updated[0].id).isEqualTo("1")
        assertThat(updated[0].location.timeZoneId).isEqualTo("Europe/Istanbul")
        assertThat(updated[1].id).isEqualTo("2")
        assertThat(updated[1].location.timeZoneId).isEqualTo("Europe/Berlin")
    }
}
