package com.kutluoglu.prayer_location.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class LocationsDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: LocationsDataStore
    private lateinit var tempDir: File

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )
    private val ankara = LocationEntry(
        id = "loc-2",
        location = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null),
        displayName = "Ankara, Turkey"
    )

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        store = LocationsDataStore(dataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `default state is empty with gps disabled`() = runBlocking<Unit> {
        val state = store.getLocations()
        assertThat(state.entries).isEmpty()
        assertThat(state.gpsEnabled).isFalse()
        assertThat(state.selectedId).isNull()
    }

    @Test
    fun `addLocation appends and persists`() = runBlocking<Unit> {
        store.addLocation(istanbul)
        store.addLocation(ankara)

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-1", "loc-2")
    }

    @Test
    fun `removeLocation removes by id and clears selection if removed`() = runBlocking<Unit> {
        store.addLocation(istanbul)
        store.addLocation(ankara)
        store.setSelectedLocation("loc-1")

        store.removeLocation("loc-1")

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-2")
        assertThat(state.selectedId).isNull()
    }

    @Test
    fun `reorderLocations applies the given id order`() = runBlocking<Unit> {
        store.addLocation(istanbul)
        store.addLocation(ankara)

        store.reorderLocations(listOf("loc-2", "loc-1"))

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-2", "loc-1")
    }

    @Test
    fun `gps toggle and selection persist`() = runBlocking<Unit> {
        store.setGpsEnabled(true)
        store.setSelectedLocation("loc-1")

        val state = store.getLocations()
        assertThat(state.gpsEnabled).isTrue()
        assertThat(state.selectedId).isEqualTo("loc-1")
    }

    @Test
    fun `observeLocations emits updated state`() = runBlocking<Unit> {
        store.addLocation(istanbul)
        val first = store.observeLocations().first()
        assertThat(first.entries).hasSize(1)

        store.addLocation(ankara)
        val second = store.observeLocations().first()
        assertThat(second.entries).hasSize(2)
    }
}
