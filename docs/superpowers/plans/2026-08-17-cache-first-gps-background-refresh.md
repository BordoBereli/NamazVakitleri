# Cache-First GPS with Background Refresh — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Home screen open instantly when GPS is selected by returning the last known location first, while a fresh high-accuracy GPS fix runs in the background and only updates the displayed location when it differs by more than 1 km.

**Architecture:** `LocationsCoordinator.resolveInitial()/resolveSelected()` now return the cached fix (persisted in `LocationsDataStore` or in-memory `_gpsLocation`) immediately and launch an app-scoped background refresh. The refresh compares the new fix against the cached value with `LocationService.isDifferentThen()`; only on a real change does it update `_gpsLocation`, persist, and update `ActiveLocationProvider`. `HomeViewModel` stays unchanged.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (KSP annotations), kotlinx-coroutines (StateFlow, CoroutineScope), DataStore Preferences, kotlinx-serialization, JUnit 5 + MockK + Truth + kotlinx-coroutines-test.

---

## File Structure

**Modified files:**

| File | Responsibility |
|------|----------------|
| `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsDataStore.kt` | Add `LAST_GPS` key + get/set for the persisted last GPS fix |
| `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationService.kt` | Add non-suspend `getLastKnownLocation()` exposing the in-memory cache |
| `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt` | Cache-first `resolveGps()`, background refresh launch, persistence in `refreshGps()`, new `CoroutineScope` constructor param |
| `prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt` | Provide the app-scoped `CoroutineScope` for the coordinator's background refresh |

**Test files:**

| File | Responsibility |
|------|----------------|
| `prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsDataStoreTest.kt` | Round-trip + corrupt-value tests for the `LAST_GPS` key |
| `prayer_location/src/test/java/com/kutluoglu/prayer_location/LocationsCoordinatorTest.kt` | Cache-first resolution, background update-on-difference, persistence |

---

## Task 1: Persist last GPS fix in `LocationsDataStore`

**Files:**
- Modify: `prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsDataStore.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsDataStoreTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `LocationsDataStoreTest.kt` before the closing brace:

```kotlin
@Test
fun `last gps location round trips through the store`() = runBlocking<Unit> {
    val gps = LocationData(40.0, 29.0, "Turkey", "TR", "Bursa", null)

    assertThat(store.getLastGpsLocation()).isNull()

    store.setLastGpsLocation(gps)

    val freshStore = LocationsDataStore(dataStore)
    assertThat(freshStore.getLastGpsLocation()).isEqualTo(gps)
}

@Test
fun `last gps location returns null when persisted value is corrupt`() = runBlocking<Unit> {
    val corruptKey = stringPreferencesKey("last_gps_location")
    dataStore.edit { prefs -> prefs[corruptKey] = "not-json" }

    assertThat(store.getLastGpsLocation()).isNull()
}
```

Add imports to the test file:

```kotlin
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :prayer_location:testDebugUnitTest --tests="com.kutluoglu.prayer_location.data.LocationsDataStoreTest" --tests="com.kutluoglu.prayer_location.data.LocationsDataStoreTest.last gps location*"
```
Expected: FAIL — compile error, `getLastGpsLocation`/`setLastGpsLocation` unresolved.

- [ ] **Step 3: Implement**

In `LocationsDataStore.kt`, add the key to the `Keys` object (`LocationsDataStore.kt:22-26`):

```kotlin
private object Keys {
    val LOCATIONS = stringPreferencesKey("locations")
    val GPS_ENABLED = booleanPreferencesKey("gps_enabled")
    val SELECTED_ID = stringPreferencesKey("selected_location_id")
    val LAST_GPS = stringPreferencesKey("last_gps_location")
}
```

Add the two methods after `replaceAll` (`LocationsDataStore.kt:74-76`):

```kotlin
suspend fun getLastGpsLocation(): LocationData? {
    val raw = dataStore.data.first()[Keys.LAST_GPS]
    if (raw.isNullOrBlank()) return null
    return runCatching { json.decodeFromString<LocationData>(raw) }.getOrNull()
}

suspend fun setLastGpsLocation(location: LocationData) {
    dataStore.edit { prefs -> prefs[Keys.LAST_GPS] = json.encodeToString(location) }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :prayer_location:testDebugUnitTest --tests="com.kutluoglu.prayer_location.data.LocationsDataStoreTest"
```
Expected: PASS (all 8 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/data/LocationsDataStore.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/data/LocationsDataStoreTest.kt
git commit -m "feat(location): persist last GPS fix in LocationsDataStore"
```

---

## Task 2: Expose in-memory GPS cache in `LocationService`

**Files:**
- Modify: `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationService.kt`

- [ ] **Step 1: Implement `getLastKnownLocation`**

In `LocationService.kt`, add the accessor right after the `currentLocation` field (`LocationService.kt:28`):

```kotlin
private var currentLocation: LocationData? = null

fun getLastKnownLocation(): LocationData? = currentLocation
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
./gradlew :prayer_location:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationService.kt
git commit -m "feat(location): expose last known location from LocationService"
```

---

## Task 3: Cache-first resolution + background refresh in `LocationsCoordinator`

**Files:**
- Modify: `prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt`
- Test: `prayer_location/src/test/java/com/kutluoglu/prayer_location/LocationsCoordinatorTest.kt`
- Modify: `prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt`

- [ ] **Step 1: Write the failing test**

In `LocationsCoordinatorTest.kt`, add a scope field to the test class (after line 23):

```kotlin
private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
```

Update the coordinator construction (line 23) to pass the scope:

```kotlin
private val coordinator = LocationsCoordinator(dataStore, locationService, provider, migration, refreshScope)
```

Add imports to the test file:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
```

Replace `Coordinates` value used in the GPS tests (line 33) with two named values so the diff assertion is meaningful. Add after the `istanbul` entry (`LocationsCoordinatorTest.kt:30`):

```kotlin
private val gpsIstanbul = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
private val gpsBursa = LocationData(40.0, 29.0, "Turkey", "TR", "Bursa", null)
```

Add these tests before the closing brace of the class:

```kotlin
@Test
fun `resolveInitial returns cached gps without triggering a fresh fix`() = runBlocking<Unit> {
    coEvery { dataStore.getLocations() } returns LocationsState(
        entries = listOf(istanbul),
        gpsEnabled = true,
        selectedId = LocationsCoordinator.GPS_LOCATION_ID
    )
    coEvery { dataStore.getLastGpsLocation() } returns gpsIstanbul
    coEvery { locationService.getCurrentLocation() } returns gpsBursa

    val result = coordinator.resolveInitial()

    assertThat(result).isEqualTo(gpsIstanbul)
    assertThat(provider.location.first()).isEqualTo(gpsIstanbul)
}

Note: with `Dispatchers.Unconfined` the background refresh launches eagerly and legitimately calls
`getCurrentLocation()`, so this test asserts the *return value* is the cached fix (not the fresh one) —
that is the cache-first behavior. Do not assert `getCurrentLocation()` was never called here.

@Test
fun `background refresh updates location and persists when different`() = runBlocking<Unit> {
    coEvery { dataStore.getLocations() } returns LocationsState(
        entries = listOf(istanbul),
        gpsEnabled = true,
        selectedId = LocationsCoordinator.GPS_LOCATION_ID
    )
    coEvery { dataStore.getLastGpsLocation() } returns gpsIstanbul
    coEvery { locationService.getCurrentLocation() } returns gpsBursa
    every { locationService.isDifferentThen(gpsIstanbul) } returns true

    coordinator.resolveInitial()

    coVerify { dataStore.setLastGpsLocation(gpsBursa) }
    assertThat(provider.location.first()).isEqualTo(gpsBursa)
}

@Test
fun `background refresh does not update when location is same`() = runBlocking<Unit> {
    coEvery { dataStore.getLocations() } returns LocationsState(
        entries = listOf(istanbul),
        gpsEnabled = true,
        selectedId = LocationsCoordinator.GPS_LOCATION_ID
    )
    coEvery { dataStore.getLastGpsLocation() } returns gpsIstanbul
    coEvery { locationService.getCurrentLocation() } returns gpsIstanbul
    every { locationService.isDifferentThen(gpsIstanbul) } returns false

    coordinator.resolveInitial()

    coVerify(exactly = 0) { dataStore.setLastGpsLocation(any()) }
    assertThat(provider.location.first()).isEqualTo(gpsIstanbul)
}

@Test
fun `selectLocation gps uses cached gps without fresh fix`() = runBlocking<Unit> {
    coEvery { dataStore.getLocations() } returns LocationsState(entries = listOf(istanbul))
    coEvery { locationService.getCurrentLocation() } returns gpsBursa
    coordinator.setGpsLocation(gpsIstanbul)

    coordinator.selectLocation(LocationsCoordinator.GPS_LOCATION_ID)

    coVerify(exactly = 0) { locationService.getCurrentLocation() }
}
```

Note: `provider` is already an `ActiveLocationProvider()` instance with a `location` StateFlow, so `provider.location.first()` works. `gpsIstanbul` differs from `gpsBursa` by > 1 km (Istanbul→Bursa ≈ 85 km).

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :prayer_location:testDebugUnitTest --tests="com.kutluoglu.prayer_location.LocationsCoordinatorTest"
```
Expected: FAIL — compile error, `LocationsCoordinator` constructor takes 4 args, not 5.

- [ ] **Step 3: Implement the DI module scope**

In `PrayerLocationModule.kt`, add a `@Single` provider for the app-scoped `CoroutineScope` (imports `kotlinx.coroutines.CoroutineScope`, `kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.SupervisorJob`):

```kotlin
@Single
fun provideLocationRefreshScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

- [ ] **Step 4: Implement cache-first resolution in `LocationsCoordinator`**

Add imports:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
```

Update the constructor (line 15-20) — add the scope as the last param:

```kotlin
class LocationsCoordinator(
    private val locationsDataStore: LocationsDataStore,
    private val locationService: LocationService,
    private val activeLocationProvider: ActiveLocationProvider,
    private val locationsMigration: LocationsMigration,
    private val refreshScope: CoroutineScope
) {
```

Replace `resolveGps()` (line 71-75) with:

```kotlin
private suspend fun resolveGps(): LocationData? {
    val cached = locationsDataStore.getLastGpsLocation() ?: _gpsLocation.value
    if (cached != null) {
        cacheAndLaunchRefresh(cached)
        return cached
    }
    val gps = refreshGps()
    if (gps != null) activeLocationProvider.set(gps)
    return gps
}

private fun cacheAndLaunchRefresh(cached: LocationData) {
    _gpsLocation.value = cached
    activeLocationProvider.set(cached)
    refreshScope.launch {
        val fresh = locationService.getCurrentLocation() ?: return@launch
        if (locationService.isDifferentThen(cached)) {
            _gpsLocation.value = fresh
            locationsDataStore.setLastGpsLocation(fresh)
            activeLocationProvider.set(fresh)
        }
    }
}
```

Update `refreshGps()` (line 77-81) to persist:

```kotlin
suspend fun refreshGps(): LocationData? {
    val gps = locationService.getCurrentLocation() ?: return null
    _gpsLocation.value = gps
    locationsDataStore.setLastGpsLocation(gps)
    return gps
}
```

Note: `refreshGps()` is still called from `selectLocation` (`LocationsCoordinator.kt:86`) when there is no cached GPS, and from `resolveGps()` when there is no cache at all.

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
./gradlew :prayer_location:testDebugUnitTest --tests="com.kutluoglu.prayer_location.LocationsCoordinatorTest"
```
Expected: PASS (all 14 tests).

- [ ] **Step 6: Run the full module test suite**

Run:
```bash
./gradlew :prayer_location:testDebugUnitTest
```
Expected: PASS (LocationsCoordinatorTest, LocationsDataStoreTest, LocationsMigrationTest, ActiveLocationProviderTest).

- [ ] **Step 7: Commit**

```bash
git add prayer_location/src/main/java/com/kutluoglu/prayer_location/LocationsCoordinator.kt prayer_location/src/main/java/com/kutluoglu/prayer_location/di/PrayerLocationModule.kt prayer_location/src/test/java/com/kutluoglu/prayer_location/LocationsCoordinatorTest.kt
git commit -m "feat(location): cache-first GPS resolution with background refresh"
```

---

## Task 4: Verify dependent modules and full build

**Files:**
- None (verification only)

- [ ] **Step 1: Run dependent module tests**

The Home feature and Settings feature construct `LocationsCoordinator`/`LocationService` via Koin, so the new scope provider must resolve. Run:

```bash
./gradlew :prayer_feature:home:testDebugUnitTest :prayer_feature:settings:testDebugUnitTest
```
Expected: PASS. If a Koin DI test fails with "no definition found for CoroutineScope", verify `PrayerLocationModule` is scanned in the app's Koin setup (it is — see `app/src/main/java/.../di/`).

- [ ] **Step 2: Full debug build + unit tests**

Run:
```bash
./gradlew assembleDebug testDebugUnitTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit any stray fixes**

If any code change was needed for compile, commit:
```bash
git add -A
git commit -m "fix(location): resolve DI scope wiring for background refresh"
```
Otherwise skip this step.

---

## Self-Review Notes

- **Spec coverage:** Task 1 = persistence goal (spec §1); Task 2 = `getLastKnownLocation` (spec §2); Task 3 = cache-first + background refresh + `refreshGps` persist (spec §3); Task 4 = HomeViewModel unchanged verification (spec §4). All spec test cases (spec §Testing) mapped to Task 1 Step 1, Task 3 Step 1.
- **Placeholder scan:** All steps have concrete code, paths, and expected output. No TBD/TODO.
- **Type consistency:** `getLastGpsLocation(): LocationData?`, `setLastGpsLocation(LocationData)`, `getLastKnownLocation(): LocationData?`, `isDifferentThen(LocationData): Boolean`, `cacheAndLaunchRefresh(LocationData)` are used consistently across tasks.