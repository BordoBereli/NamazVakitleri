# Multiple Locations with Per-Location Home Screens — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. **TDD is mandatory for every code change**: write the failing test first, watch it fail, implement, watch it pass, commit.

**Goal:** Support unlimited locations, each with its own home screen page (swipeable pager + tap-able chips), visually distinguishing the auto GPS location from manual ones, managed from settings, with monthly & qibla following the selected location.

**Architecture:** Locations domain + persistence live in `prayer_location` (per user preference). A `LocationsCoordinator` (singleton) replaces the home feature's `LocationCoordinator`. A shared `ActiveLocationProvider` singleton exposes the selected location so monthly/qibla follow it. `prayer_settings` gains only the "My Locations" manager screen UI. Home renders a `HorizontalPager`.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (annotations), Preferences DataStore, kotlinx.serialization, kotlinx-datetime, JUnit 5 + MockK + Turbine + Truth.

**Spec:** `docs/superpowers/specs/2026-08-14-multiple-locations-design.md`

---

## File Structure

### New files
- `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt`
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsState.kt`
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsDataStore.kt`
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsMigration.kt`
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/ActiveLocationProvider.kt`
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt`
- `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsContract.kt`
- `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsViewModel.kt`
- `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreen.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt`
- Tests for each of the above logic classes

### Modified files
- `prayer_location/build.gradle.kts` (deps)
- `prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt` (DataStore provider)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`
- `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt`
- `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`
- `prayer_feature/prayertimes/build.gradle.kts` (add `:prayer_location`)
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`
- `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModel.kt`
- `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt` (register new VMs if needed)

---

## Phase 1 — Foundation (`prayer:model` + `prayer_location`)

### Task 1: Add `LocationEntry` model

**Files:**
- Create: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt`

- [ ] **Step 1: Create the model**

```kotlin
package com.kutluoglu.prayer.model.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String
)
```

No test needed — pure data class with no logic (TDD applies to behavior, not data holders).

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer:model:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt
git commit -m "feat(model): add LocationEntry model"
```

### Task 2: Add datastore + serialization deps to `prayer_location`

**Files:**
- Modify: `prayer_location/build.gradle.kts`

- [ ] **Step 1: Add the serialization plugin**

In the `plugins {}` block, add `alias(libs.plugins.kotlin.serialization)` (verify the alias exists in `gradle/libs.versions.toml` — `prayer_settings` uses it).

- [ ] **Step 2: Add dependencies**

In `dependencies {}`, add:

```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: Verify build**

Run: `./gradlew :prayer_location:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_location/build.gradle.kts
git commit -m "build(location): add datastore and serialization deps"
```

### Task 3: `LocationsState` + `LocationsDataStore` (TDD)

**Files:**
- Create: `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsState.kt`
- Create: `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsDataStore.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsDataStoreTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
    fun `default state is empty with gps disabled`() = runBlocking {
        val state = store.getLocations()
        assertThat(state.entries).isEmpty()
        assertThat(state.gpsEnabled).isFalse()
        assertThat(state.selectedId).isNull()
    }

    @Test
    fun `addLocation appends and persists`() = runBlocking {
        store.addLocation(istanbul)
        store.addLocation(ankara)

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-1", "loc-2")
    }

    @Test
    fun `removeLocation removes by id and clears selection if removed`() = runBlocking {
        store.addLocation(istanbul)
        store.addLocation(ankara)
        store.setSelectedLocation("loc-1")

        store.removeLocation("loc-1")

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-2")
        assertThat(state.selectedId).isNull()
    }

    @Test
    fun `reorderLocations applies the given id order`() = runBlocking {
        store.addLocation(istanbul)
        store.addLocation(ankara)

        store.reorderLocations(listOf("loc-2", "loc-1"))

        val state = store.getLocations()
        assertThat(state.entries.map { it.id }).containsExactly("loc-2", "loc-1")
    }

    @Test
    fun `gps toggle and selection persist`() = runBlocking {
        store.setGpsEnabled(true)
        store.setSelectedLocation("loc-1")

        val state = store.getLocations()
        assertThat(state.gpsEnabled).isTrue()
        assertThat(state.selectedId).isEqualTo("loc-1")
    }

    @Test
    fun `observeLocations emits updated state`() = runBlocking {
        store.addLocation(istanbul)
        val first = store.observeLocations().first()
        assertThat(first.entries).hasSize(1)

        store.addLocation(ankara)
        val second = store.observeLocations().first()
        assertThat(second.entries).hasSize(2)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsDataStoreTest"`
Expected: FAIL — `LocationsDataStore` does not exist (compile error)

- [ ] **Step 3: Write minimal implementation**

`LocationsState.kt`:
```kotlin
package com.kutluoglu.prayer_location.data

import com.kutluoglu.prayer.model.location.LocationEntry

data class LocationsState(
    val entries: List<LocationEntry> = emptyList(),
    val gpsEnabled: Boolean = false,
    val selectedId: String? = null
)
```

`LocationsDataStore.kt`:
```kotlin
package com.kutluoglu.prayer_location.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.location.LocationEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class LocationsDataStore(
    @Named("locations") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val LOCATIONS = stringPreferencesKey("locations")
        val GPS_ENABLED = booleanPreferencesKey("gps_enabled")
        val SELECTED_ID = stringPreferencesKey("selected_location_id")
    }

    fun observeLocations(): Flow<LocationsState> = dataStore.data.map { prefs ->
        LocationsState(
            entries = decodeEntries(prefs[Keys.LOCATIONS]),
            gpsEnabled = prefs[Keys.GPS_ENABLED] ?: false,
            selectedId = prefs[Keys.SELECTED_ID]
        )
    }

    suspend fun getLocations(): LocationsState = observeLocations().first()

    suspend fun addLocation(entry: LocationEntry) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS])
            prefs[Keys.LOCATIONS] = json.encodeToString(current + entry)
        }
    }

    suspend fun removeLocation(id: String) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS]).filterNot { it.id == id }
            prefs[Keys.LOCATIONS] = json.encodeToString(current)
            if (prefs[Keys.SELECTED_ID] == id) prefs.remove(Keys.SELECTED_ID)
        }
    }

    suspend fun reorderLocations(ids: List<String>) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS])
            val byId = current.associateBy { it.id }
            val reordered = ids.mapNotNull { byId[it] }
            prefs[Keys.LOCATIONS] = json.encodeToString(reordered)
        }
    }

    suspend fun setGpsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.GPS_ENABLED] = enabled }
    }

    suspend fun setSelectedLocation(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.SELECTED_ID) else prefs[Keys.SELECTED_ID] = id
        }
    }

    suspend fun replaceAll(entries: List<LocationEntry>) {
        dataStore.edit { prefs -> prefs[Keys.LOCATIONS] = json.encodeToString(entries) }
    }

    private fun decodeEntries(raw: String?): List<LocationEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LocationEntry>>(raw) }
            .getOrDefault(emptyList())
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsDataStoreTest"`
Expected: PASS (6 tests)

- [ ] **Step 5: Provide the DataStore in the DI module**

Modify `prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt`:

```kotlin
package com.kutluoglu.prayer_location.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_location**")
object PrayerLocationModule {
    @Single
    @Named("locations")
    fun provideLocationsDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("locations_store") }
        )
}
```

Note: the `@Named("locations")` qualifier avoids a Koin conflict with `prayer:data`'s unqualified `DataStore<Preferences>` provider.

- [ ] **Step 6: Verify Koin graph resolves**

Run: `./gradlew :prayer_location:testDebugUnitTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/data/ prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsDataStoreTest.kt
git commit -m "feat(location): add LocationsDataStore with TDD"
```

### Task 4: `LocationsMigration` (TDD)

**Files:**
- Create: `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsMigration.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsMigrationTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.kutluoglu.prayer_location.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LocationsMigrationTest {

    private val legacyStore = mockk<LocationDataStore>(relaxed = true)
    private val locationsStore = mockk<LocationsDataStore>(relaxed = true)

    @Test
    fun `does nothing when locations already exist`() = runBlocking {
        coEvery { locationsStore.getLocations() } returns LocationsState(
            entries = listOf(
                LocationEntry("x", LocationData(1.0, 2.0, "A", "AA", "City", null), displayName = "City")
            )
        )

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { legacyStore.getSavedLocation() }
    }

    @Test
    fun `migrates legacy saved location when list is empty`() = runBlocking {
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
    fun `does nothing when no legacy location exists`() = runBlocking {
        coEvery { locationsStore.getLocations() } returns LocationsState()
        coEvery { legacyStore.getSavedLocation() } returns null

        LocationsMigration(locationsStore, legacyStore).migrateIfNeeded()

        coVerify(exactly = 0) { locationsStore.addLocation(any()) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsMigrationTest"`
Expected: FAIL — `LocationsMigration` does not exist

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.kutluoglu.prayer_location.data

import com.kutluoglu.prayer.data.mapper.location.LocationMapper
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationEntry
import org.koin.core.annotation.Factory
import java.util.UUID

@Factory
class LocationsMigration(
    private val locationsDataStore: LocationsDataStore,
    private val legacyLocationDataStore: LocationDataStore,
    private val locationMapper: LocationMapper = LocationMapper()
) {
    suspend fun migrateIfNeeded() {
        val state = locationsDataStore.getLocations()
        if (state.entries.isNotEmpty()) return
        val legacy = legacyLocationDataStore.getSavedLocation() ?: return
        val location = locationMapper.mapToDomain(legacy)
        locationsDataStore.addLocation(
            LocationEntry(
                id = UUID.randomUUID().toString(),
                location = location,
                displayName = listOfNotNull(location.city, location.country)
                    .joinToString(", ").ifBlank { "My Location" }
            )
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsMigrationTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsMigration.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsMigrationTest.kt
git commit -m "feat(location): add legacy location migration with TDD"
```

---

## Phase 2 — Domain logic (`prayer_location`)

### Task 5: `ActiveLocationProvider` (TDD)

**Files:**
- Create: `prayer_location/src/main/java/com/kutluoglu/prayer_location/ActiveLocationProvider.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/ActiveLocationProviderTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.kutluoglu.prayer_location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ActiveLocationProviderTest {

    private val provider = ActiveLocationProvider()

    @Test
    fun `starts with null location`() = runBlocking {
        assertThat(provider.location.first()).isNull()
    }

    @Test
    fun `set updates the emitted location`() = runBlocking {
        val location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        provider.set(location)
        assertThat(provider.location.first()).isEqualTo(location)
    }

    @Test
    fun `set null clears the location`() = runBlocking {
        provider.set(LocationData(1.0, 2.0, "A", "AA", "C", null))
        provider.set(null)
        assertThat(provider.location.first()).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*ActiveLocationProviderTest"`
Expected: FAIL — `ActiveLocationProvider` does not exist

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class ActiveLocationProvider {
    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location

    fun set(location: LocationData?) {
        _location.value = location
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*ActiveLocationProviderTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/ActiveLocationProvider.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/ActiveLocationProviderTest.kt
git commit -m "feat(location): add ActiveLocationProvider with TDD"
```

### Task 6: `LocationsCoordinator` (TDD)

**Files:**
- Create: `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/LocationsCoordinatorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.kutluoglu.prayer_location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.data.LocationsDataStore
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
    fun `observeState includes synthetic gps entry when enabled`() = runBlocking {
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
    fun `observeState omits gps entry when disabled`() = runBlocking {
        coEvery { dataStore.observeLocations() } returns MutableStateFlow(
            LocationsState(entries = listOf(istanbul), gpsEnabled = false)
        )

        val state = coordinator.observeState().first()
        assertThat(state.entries).hasSize(1)
        assertThat(state.entries.first().isAutoGps).isFalse()
    }

    @Test
    fun `resolveInitial returns selected location and updates provider`() = runBlocking {
        coEvery { dataStore.getLocations() } returns LocationsState(
            entries = listOf(istanbul),
            selectedId = "loc-1"
        )

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(istanbul.location)
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }

    @Test
    fun `resolveInitial falls back to first entry when no selection`() = runBlocking {
        coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(istanbul.location)
    }

    @Test
    fun `resolveInitial returns gps when no manual entries and gps enabled`() = runBlocking {
        val gps = LocationData(40.0, 29.0, "Turkey", "TR", "Bursa", null)
        coEvery { dataStore.getLocations() } returns LocationsState(gpsEnabled = true)
        coEvery { locationService.getCurrentLocation() } returns gps

        val result = coordinator.resolveInitial()

        assertThat(result).isEqualTo(gps)
    }

    @Test
    fun `resolveInitial returns null when nothing resolvable`() = runBlocking {
        coEvery { dataStore.getLocations() } returns LocationsState()

        val result = coordinator.resolveInitial()

        assertThat(result).isNull()
    }

    @Test
    fun `selectLocation persists selection and updates provider`() = runBlocking {
        coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))

        coordinator.selectLocation("loc-1")

        coVerify { dataStore.setSelectedLocation("loc-1") }
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }

    @Test
    fun `addLocation sets as selected when no selection exists`() = runBlocking {
        coEvery { dataStore.getLocations() } returns LocationsState()

        coordinator.addLocation(istanbul)

        coVerify { dataStore.addLocation(istanbul) }
        coVerify { dataStore.setSelectedLocation("loc-1") }
        assertThat(provider.location.first()).isEqualTo(istanbul.location)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsCoordinatorTest"`
Expected: FAIL — `LocationsCoordinator` does not exist

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.data.LocationsDataStore
import com.kutluoglu.prayer_location.data.LocationsMigration
import com.kutluoglu.prayer_location.data.LocationsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class LocationsCoordinator(
    private val locationsDataStore: LocationsDataStore,
    private val locationService: LocationService,
    private val activeLocationProvider: ActiveLocationProvider,
    private val locationsMigration: LocationsMigration
) {
    private val _gpsLocation = MutableStateFlow<LocationData?>(null)

    fun observeState(): Flow<LocationsState> =
        combine(locationsDataStore.observeLocations(), _gpsLocation) { state, gps ->
            if (state.gpsEnabled && gps != null) {
                state.copy(entries = listOf(gps.toEntry()) + state.entries)
            } else {
                state
            }
        }

    fun setGpsLocation(location: LocationData?) {
        _gpsLocation.value = location
    }

    suspend fun resolveInitial(): LocationData? {
        locationsMigration.migrateIfNeeded()
        val state = locationsDataStore.getLocations()
        val selected = state.entries.firstOrNull { it.id == state.selectedId }
            ?: state.entries.firstOrNull()
        if (selected != null) {
            activeLocationProvider.set(selected.location)
            return selected.location
        }
        if (state.gpsEnabled) {
            return refreshGps()
        }
        return null
    }

    suspend fun resolveSelected(): LocationData? {
        val state = locationsDataStore.getLocations()
        val selected = state.entries.firstOrNull { it.id == state.selectedId }
            ?: state.entries.firstOrNull()
        return selected?.location
    }

    suspend fun refreshGps(): LocationData? {
        val gps = locationService.getCurrentLocation() ?: return null
        _gpsLocation.value = gps
        return gps
    }

    suspend fun selectLocation(id: String) {
        locationsDataStore.setSelectedLocation(id)
        val entry = locationsDataStore.getLocations().entries.firstOrNull { it.id == id }
        entry?.let { activeLocationProvider.set(it.location) }
    }

    suspend fun addLocation(entry: LocationEntry) {
        locationsDataStore.addLocation(entry)
        val state = locationsDataStore.getLocations()
        if (state.selectedId == null) {
            locationsDataStore.setSelectedLocation(entry.id)
            activeLocationProvider.set(entry.location)
        }
    }

    suspend fun removeLocation(id: String) {
        locationsDataStore.removeLocation(id)
    }

    suspend fun reorderLocations(ids: List<String>) {
        locationsDataStore.reorderLocations(ids)
    }

    suspend fun setGpsEnabled(enabled: Boolean) {
        locationsDataStore.setGpsEnabled(enabled)
    }

    private fun LocationData.toEntry(): LocationEntry =
        LocationEntry(
            id = "gps",
            location = this,
            isAutoGps = true,
            displayName = listOfNotNull(city, country).joinToString(", ").ifBlank { "GPS" }
        )
}
```

Note: `LocationsCoordinator` injects `LocationsMigration` and calls `migrateIfNeeded()` at the start of `resolveInitial()` — this wires up the legacy-location migration (Task 4) so it actually runs at startup.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_location:testDebugUnitTest --tests="*LocationsCoordinatorTest"`
Expected: PASS (8 tests)

- [ ] **Step 5: Run the full `prayer_location` suite**

Run: `./gradlew :prayer_location:testDebugUnitTest`
Expected: PASS (all)

- [ ] **Step 6: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/LocationsCoordinatorTest.kt
git commit -m "feat(location): add LocationsCoordinator with TDD"
```

---

## Phase 3 — Home UI (`prayer_feature:home`)

### Task 7: Rewrite `HomeViewModel` to use `LocationsCoordinator` (TDD)

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`

First read the current `HomeEvent.kt` and `HomeViewModelTest.kt` to preserve existing test conventions.

- [ ] **Step 1: Add `OnLocationSelected` to `HomeEvent`**

```kotlin
sealed interface HomeEvent {
    object OnRefresh : HomeEvent
    object OnPermissionsGranted : HomeEvent
    object OnUpdateLocationConfirmed : HomeEvent
    object OnLoadQuranVerse : HomeEvent
    object OnVerseClicked : HomeEvent
    object OnVerseDetailDismissed : HomeEvent
    data class OnLocationSelected(val locationId: String) : HomeEvent
}
```

**Keep `OnUpdateLocationConfirmed` and `HomeUiState.Success.showLocationUpdatePrompt` for now** — they are removed in Task 8 when `HomeScreen` is rewritten. This keeps the build green at each step. The new `HomeViewModel` simply never emits the prompt (`_promptState` stays `false`).

- [ ] **Step 2: Write the failing test**

Update `HomeViewModelTest` to mock `LocationsCoordinator` instead of `LocationCoordinator`. Key new assertions:

```kotlin
@Test
fun `location selection reloads prayer times for the selected location`() = runTest {
    val location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
    coEvery { locationsCoordinator.observeState() } returns flowOf(
        LocationsState(entries = listOf(entry), selectedId = "loc-1")
    )
    coEvery { locationsCoordinator.resolveInitial() } returns location
    coEvery { prayerTimesLoader.load(location) } returns Result.success(loadedData)

    viewModel.onEvent(HomeEvent.OnLocationSelected("loc-1"))

    coVerify { locationsCoordinator.selectLocation("loc-1") }
}
```

Also assert `locationsState` is exposed and reflects the observed `LocationsState`.

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: FAIL (compile — `LocationsCoordinator` not injected)

- [ ] **Step 5: Rewrite `HomeViewModel`**

```kotlin
@OptIn(FlowPreview::class)
@KoinViewModel
class HomeViewModel(
    private val locationsCoordinator: LocationsCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader
) : ViewModel() {

    private val _screenGate = MutableStateFlow<HomeScreenGate>(HomeScreenGate.Loading)
    val screenGate: StateFlow<HomeScreenGate> = _screenGate

    private val _timeState = MutableStateFlow<TimeUiState?>(null)
    val timeState: StateFlow<TimeUiState?> = _timeState

    private val _locationState = MutableStateFlow<LocationUiState?>(null)
    val locationState: StateFlow<LocationUiState?> = _locationState

    private val _prayerState = MutableStateFlow<PrayerUiState?>(null)
    val prayerState: StateFlow<PrayerUiState?> = _prayerState

    private val _locationsState = MutableStateFlow<LocationsState>(LocationsState())
    val locationsState: StateFlow<LocationsState> = _locationsState

    private val _promptState = MutableStateFlow(false)
    val promptState: StateFlow<Boolean> = _promptState

    val countdownState: StateFlow<CountdownUiState> = countdownEngine.countdownState
    val quranState: StateFlow<QuranUiState> = quranVerseLoader.quranState

    private var locationsObserverJob: Job? = null
    private var prayerPassedObserverJob: Job? = null
    private var dayChangedObserverJob: Job? = null

    init {
        locationsObserverJob = viewModelScope.launch {
            locationsCoordinator.observeState().collect { state ->
                _locationsState.value = state
                val selected = resolveSelected(state)
                if (selected != null) {
                    onLocationResolved(selected)
                } else {
                    fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
                }
            }
        }
        prayerPassedObserverJob = viewModelScope.launch {
            countdownEngine.prayerPassedSignal.collect { refreshPrayerState() }
        }
        dayChangedObserverJob = viewModelScope.launch {
            countdownEngine.dayChangedSignal.collect { loadPrayerTimesForCurrentLocation() }
        }
        loadInitialLocation()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> loadPrayerTimesForCurrentLocation()
            HomeEvent.OnPermissionsGranted -> loadPrayerTimesForCurrentLocation()
            HomeEvent.OnUpdateLocationConfirmed -> Unit // retired; removed in Task 8
            HomeEvent.OnLoadQuranVerse -> loadRandomVerse()
            HomeEvent.OnVerseClicked -> setVerseSheetVisibility(isVisible = true)
            HomeEvent.OnVerseDetailDismissed -> setVerseSheetVisibility(isVisible = false)
            is HomeEvent.OnLocationSelected -> selectLocation(event.locationId)
        }
    }

    private fun resolveSelected(state: LocationsState): LocationData? =
        state.entries.firstOrNull { it.id == state.selectedId }?.location
            ?: state.entries.firstOrNull()?.location

    private fun loadInitialLocation() {
        viewModelScope.launch {
            val location = locationsCoordinator.resolveInitial()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val location = locationsCoordinator.resolveSelected()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    private fun selectLocation(locationId: String) {
        viewModelScope.launch {
            locationsCoordinator.selectLocation(locationId)
        }
    }

    private suspend fun onLocationResolved(location: LocationData) {
        prayerTimesLoader.load(location)
            .onSuccess { loaded ->
                _locationState.value = loaded.locationState
                _timeState.value = loaded.timeState
                _prayerState.value = loaded.prayerState
                _screenGate.value = HomeScreenGate.Ready
                startCountdown()
            }
            .onFailure { error ->
                _screenGate.value = HomeScreenGate.Error(
                    error.message ?: HomeErrorMapper.getUserFriendlyErrorMessage(error)
                )
            }
    }

    private fun refreshPrayerState() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        val refreshed = prayerTimesLoader.computePrayerState(currentState.prayers, zoneId)
        _prayerState.value = refreshed
        _screenGate.value = HomeScreenGate.Ready
        startCountdown()
    }

    private fun startCountdown() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        countdownEngine.start(currentState, zoneId, viewModelScope)
    }

    private fun loadRandomVerse() {
        quranVerseLoader.loadVerse(
            scope = viewModelScope,
            isScreenReady = { _screenGate.value == HomeScreenGate.Ready }
        )
    }

    private fun setVerseSheetVisibility(isVisible: Boolean) {
        quranVerseLoader.setSheetVisible(isVisible)
    }

    private fun fail(message: String) {
        _screenGate.value = HomeScreenGate.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        countdownEngine.stop()
        locationsObserverJob?.cancel()
        prayerPassedObserverJob?.cancel()
        dayChangedObserverJob?.cancel()
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: PASS

- [ ] **Step 7: Run the full home suite**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt
git commit -m "feat(home): rewrite HomeViewModel for multiple locations with TDD"
```

### Task 8: Home pager + location chips UI

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`

- [ ] **Step 1: Create `LocationChipsRow`**

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEach { entry ->
            FilterChip(
                selected = entry.id == selectedId,
                onClick = { onLocationSelected(entry.id) },
                leadingIcon = {
                    if (entry.isAutoGps) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = entry.displayName,
                        color = if (entry.isAutoGps) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            )
        }
        FilterChip(
            selected = false,
            onClick = onAddLocation,
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null)
            },
            label = { Text("+") }
        )
    }
}
```

- [ ] **Step 2: Update `HomeRoute` to pass `locationsState`**

```kotlin
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject<QuranVerseFormatter>(),
    navController: NavController
) {
    val gate by viewModel.screenGate.collectAsState()
    val time by viewModel.timeState.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val prayer by viewModel.prayerState.collectAsState()
    val countdown by viewModel.countdownState.collectAsState()
    val quran by viewModel.quranState.collectAsState()
    val locations by viewModel.locationsState.collectAsState()

    val uiState = remember(gate, time, location, prayer, countdown, quran) {
        mergeToHomeUiState(gate, location, time, prayer, countdown, quran)
    }

    HomeScreen(
        navController = navController,
        uiState = uiState,
        locationsState = locations,
        quranVerseFormatter = verseFormatter,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}
```

Note: `mergeToHomeUiState` no longer takes `prompt` (retired). Update its signature and the `HomeUiStateMergerTest` accordingly.

- [ ] **Step 3: Update `HomeScreen` to add the pager**

Wrap the existing `PrayerContent` in a `HorizontalPager`:

```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState,
    locationsState: LocationsState,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            val entries = locationsState.entries
            val selectedIndex = entries.indexOfFirst { it.id == locationsState.selectedId }
                .coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = selectedIndex,
                pageCount = { entries.size.coerceAtLeast(1) }
            )

            LaunchedEffect(locationsState.selectedId) {
                val index = entries.indexOfFirst { it.id == locationsState.selectedId }
                if (index >= 0 && index != pagerState.currentPage) {
                    pagerState.animateScrollToPage(index)
                }
            }

            LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                val entry = entries.getOrNull(pagerState.currentPage)
                if (entry != null && entry.id != locationsState.selectedId) {
                    onEvent(HomeEvent.OnLocationSelected(entry.id))
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LocationChipsRow(
                    entries = entries,
                    selectedId = locationsState.selectedId,
                    onLocationSelected = { id -> onEvent(HomeEvent.OnLocationSelected(id)) },
                    onAddLocation = {
                        navController.navigate(Screen.SettingsScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                HorizontalPager(state = pagerState) { page ->
                    val entry = entries.getOrNull(page)
                    if (entry != null && entry.id == locationsState.selectedId) {
                        PrayerContent(navController, uiState, quranVerseFormatter, onEvent)
                    } else {
                        LocationPlaceholder(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPlaceholder(entry: LocationEntry?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = entry?.displayName ?: "",
            style = MaterialTheme.typography.titleLarge
        )
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

Imports needed in `HomeScreen.kt`: `androidx.compose.foundation.pager.HorizontalPager`, `androidx.compose.foundation.pager.rememberPagerState`, `com.kutluoglu.prayer_location.data.LocationsState`, `com.kutluoglu.prayer.model.location.LocationEntry`, `com.kutluoglu.prayer_navigation.core.Screen`. Remove the now-unused snackbar imports (`SnackbarHost`, `SnackbarHostState`, `SnackbarDuration`, `SnackbarResult`) since the drift prompt is retired.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt
git commit -m "feat(home): add location pager and chips"
```

### Task 9: Visual distinction (GPS vs manual)

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt` (already done in Task 8)
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/PrayerCard.kt` (add GPS accent)

- [ ] **Step 1: Read `PrayerCard.kt` and add a GPS indicator**

Read `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/PrayerCard.kt`. Add an optional `isAutoGps: Boolean = false` param. When true, render a small "GPS" label/badge on the card (e.g., a `Text` with `MaterialTheme.colorScheme.primary` and a location icon).

- [ ] **Step 2: Pass `isAutoGps` from the pager**

In `HomeScreen`, pass `isAutoGps = entry.isAutoGps` into the prayer grid/card chain (`DailyPrayers` → `PrayerGrid` → `PrayerCard`). Add the param through `DailyPrayers` and `PrayerGrid`.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/
git commit -m "feat(home): visually distinguish GPS location cards"
```

---

## Phase 4 — Settings manager (`prayer_feature:settings`)

### Task 10: `MyLocationsViewModel` + contract (TDD)

**Files:**
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsContract.kt`
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsViewModel.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
import kotlinx.coroutines.test.StandardTestDispatcher
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
        Dispatchers.setMain(StandardTestDispatcher())
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*MyLocationsViewModelTest"`
Expected: FAIL — `MyLocationsViewModel` does not exist

- [ ] **Step 3: Write minimal implementation**

`MyLocationsContract.kt`:
```kotlin
package com.kutluoglu.prayer_feature.settings.location

import com.kutluoglu.prayer_location.data.LocationsState

sealed class MyLocationsEvent {
    data class RemoveLocation(val id: String) : MyLocationsEvent()
    data class SetGpsEnabled(val enabled: Boolean) : MyLocationsEvent()
    data class SelectLocation(val id: String) : MyLocationsEvent()
}
```

`MyLocationsViewModel.kt`:
```kotlin
package com.kutluoglu.prayer_feature.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MyLocationsViewModel(
    private val locationsCoordinator: LocationsCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationsState>(LocationsState())
    val uiState: StateFlow<LocationsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationsCoordinator.observeState().collect { _uiState.value = it }
        }
    }

    fun onEvent(event: MyLocationsEvent) {
        when (event) {
            is MyLocationsEvent.RemoveLocation -> viewModelScope.launch {
                locationsCoordinator.removeLocation(event.id)
            }
            is MyLocationsEvent.SetGpsEnabled -> viewModelScope.launch {
                locationsCoordinator.setGpsEnabled(event.enabled)
            }
            is MyLocationsEvent.SelectLocation -> viewModelScope.launch {
                locationsCoordinator.selectLocation(event.id)
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*MyLocationsViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsContract.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsViewModel.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsViewModelTest.kt
git commit -m "feat(settings): add MyLocationsViewModel with TDD"
```

### Task 11: `MyLocationsScreen` + settings wiring

**Files:**
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreen.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`
- Modify: `prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt`

- [ ] **Step 1: Add `MyLocationsScreen` route**

In `PrayerScreens.kt`:
```kotlin
data object MyLocationsScreen: Screen("my_locations")
```

- [ ] **Step 2: Create `MyLocationsScreen`**

A `Scaffold` with a `TopAppBar` ("My Locations"), a GPS toggle `Switch` at the top, and a `LazyColumn` of location rows. Each row shows the display name, a GPS/manual badge, a delete icon (except the GPS entry), and is clickable to select. An "Add location" button navigates to `LocationSelectionScreen`. Uses `MyLocationsViewModel`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLocationsRoute(
    onNavigateBack: () -> Unit,
    onAddLocation: () -> Unit,
    viewModel: MyLocationsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Locations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use my current location", style = MaterialTheme.typography.bodyLarge)
                    Text("Auto-updates as you travel", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.gpsEnabled,
                    onCheckedChange = { viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(it)) }
                )
            }
            Button(
                onClick = onAddLocation,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add location")
            }
            LazyColumn {
                items(state.entries, key = { it.id }) { entry ->
                    LocationRow(
                        entry = entry,
                        isSelected = entry.id == state.selectedId,
                        onSelect = { viewModel.onEvent(MyLocationsEvent.SelectLocation(entry.id)) },
                        onDelete = {
                            if (!entry.isAutoGps) {
                                viewModel.onEvent(MyLocationsEvent.RemoveLocation(entry.id))
                            }
                        }
                    )
                }
            }
        }
    }
}
```

Implement `LocationRow` as a `Card` with the display name, a "GPS"/"Manual" badge, a check icon when selected, and a delete `IconButton` (hidden for `isAutoGps`).

- [ ] **Step 3: Wire into `SettingsGraph`**

Add a `MyLocationsScreen` composable destination. Change the existing `LocationSelectionScreen` destination so `onCitySelected` adds the city to the locations list (via a small helper that converts `City` → `LocationEntry` and calls `LocationsCoordinator.addLocation`), then pops back to `MyLocationsScreen`.

- [ ] **Step 4: Update `SettingsScreen`**

Change the Location `SettingsItem` subtitle to show the count of locations (e.g., "3 locations") and navigate to `MyLocationsScreen` instead of `LocationSelectionScreen`. Add an `onNavigateToMyLocations` callback.

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/
git commit -m "feat(settings): add My Locations manager screen"
```

---

## Phase 5 — Monthly & qibla follow active location

### Task 12: `PrayerTimesViewModel` per-location cache (TDD)

**Files:**
- Modify: `prayer_feature/prayertimes/build.gradle.kts` (add `implementation(project(":prayer_location"))`)
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`

- [ ] **Step 1: Add the dependency**

In `prayer_feature/prayertimes/build.gradle.kts`, add `implementation(project(":prayer_location"))`.

- [ ] **Step 2: Write the failing test**

Update `PrayerTimesViewModelTest` to inject `ActiveLocationProvider` and assert:
- `loadMonthlyPrayerTimes` reads the location from `ActiveLocationProvider`
- Switching the active location clears nothing but loads from that location's cache (per-location cache isolation)

```kotlin
@Test
fun `month cache is keyed per location`() = runTest {
    val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
    val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
    provider.set(locA)
    viewModel.loadMonthlyPrayerTimes()
    // ... load month for locA
    provider.set(locB)
    viewModel.loadMonthlyPrayerTimes()
    // ... assert locB's month is fetched (not locA's cache)
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: FAIL

- [ ] **Step 4: Rewrite `PrayerTimesViewModel`**

Replace `getSavedLocationUseCase` with `ActiveLocationProvider`:

```kotlin
@KoinViewModel
class PrayerTimesViewModel(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val activeLocationProvider: ActiveLocationProvider,
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter
) : ViewModel() {
    // monthCache: Map<locationId, Map<YearMonth, List<DailyPrayer>>>
    private val monthCache = mutableMapOf<String, MutableMap<YearMonth, List<DailyPrayer>>>()
    // selectedMonthByLocation: Map<locationId, YearMonth>
    private val selectedMonthByLocation = mutableMapOf<String, YearMonth>()
    private var activeLocationId: String? = null
    ...
}
```

Key changes:
- `loadMonthlyPrayerTimes()` reads `activeLocationProvider.location.first()`; derive a stable `locationId` (e.g., `"${lat},${lon}"`); load the current month for that location.
- `loadMonth(month)` uses `monthCache[locationId]` (per-location map).
- `navigateToMonth` updates `selectedMonthByLocation[locationId]`.
- On location change, `selectedMonth` is read from `selectedMonthByLocation[locationId] ?: currentMonth()`.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: PASS

- [ ] **Step 6: Run the full prayertimes suite**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/prayertimes/build.gradle.kts prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "feat(prayertimes): per-location month cache with TDD"
```

### Task 13: `QiblaViewModel` uses `ActiveLocationProvider` (TDD)

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModel.kt`
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Update `QiblaViewModelTest` to inject `ActiveLocationProvider` and assert qibla uses the provider's location (not `locationService.getCurrentLocation()`).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="*QiblaViewModelTest"`
Expected: FAIL

- [ ] **Step 3: Rewrite `QiblaViewModel`**

```kotlin
@KoinViewModel
class QiblaViewModel(
    private val activeLocationProvider: ActiveLocationProvider,
    private val calculateQiblaUseCase: CalculateQiblaUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QiblaUiState()
        )

    private var observationJob: Job? = null

    fun onEvent(event: QiblaEvent) {
        when (event) {
            QiblaEvent.OnStart -> startQiblaObservation()
            QiblaEvent.OnStop -> stopQiblaObservation()
        }
    }

    private fun startQiblaObservation() {
        if (observationJob?.isActive == true) return
        observationJob = viewModelScope.launch {
            try {
                val location = activeLocationProvider.location.first()
                location?.let {
                    calculateQiblaUseCase.observeQiblaDirection(it.latitude, it.longitude)
                        .collectLatest { currQiblaState ->
                            _uiState.update {
                                it.copy(
                                    qiblaAngle = currQiblaState.qiblaAngle,
                                    deviceAzimuth = currQiblaState.deviceAzimuth,
                                    sensorAccuracy = currQiblaState.sensorAccuracy,
                                    qiblaBearing = currQiblaState.qiblaBearing,
                                    isLocationAvailable = true,
                                    error = null
                                )
                            }
                        }
                } ?: throw Exception("Location not found")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i("QiblaViewModel", "Observation Job was cancelled as expected.")
                } else {
                    Log.e("QiblaViewModel", "Error observing Qibla state", e)
                    _uiState.update { it.copy(error = e.message, isLocationAvailable = false) }
                }
            }
        }
    }

    private fun stopQiblaObservation() {
        observationJob?.cancel()
        observationJob = null
        calculateQiblaUseCase.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopQiblaObservation()
    }
}
```

Imports to add: `com.kutluoglu.prayer_location.ActiveLocationProvider`, `kotlinx.coroutines.flow.first`. Remove the now-unused `LocationService` import.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="*QiblaViewModelTest"`
Expected: PASS

- [ ] **Step 5: Run the full qibla suite**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModel.kt prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModelTest.kt
git commit -m "feat(qibla): use ActiveLocationProvider with TDD"
```

---

## Final Verification

- [ ] **Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all modules)

- [ ] **Run `gitnexus_detect_changes`**

Run the GitNexus `detect_changes` tool to verify the changes only affect expected symbols/flows.

- [ ] **Build the app**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Update `TODO.md`** — mark the multiple-locations feature as done and note the retired drift prompt.

---

## Self-Review Notes

- **Spec coverage:** Every spec section maps to a task: model+persistence (1-4), domain (5-6), home UI (7-9), settings manager (10-11), monthly/qibla (12-13).
- **Retired behavior:** The GPS drift prompt (`OnUpdateLocationConfirmed`, `showLocationUpdatePrompt`, snackbar) is removed — the auto GPS location updates on refresh instead. Update `HomeUiStateMergerTest` accordingly.
- **Koin conflict avoided:** `@Named("locations")` qualifier on the `DataStore<Preferences>` provider in `prayer_location` prevents a clash with `prayer:data`'s unqualified provider.
- **Type consistency:** `LocationsState`, `LocationEntry`, `LocationsCoordinator`, `ActiveLocationProvider` names are used consistently across all tasks.
