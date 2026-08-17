# Cache-First GPS with Background Refresh

## Problem

Every time the Home screen opens while GPS is the selected location, the app blocks on a
fresh high-accuracy GPS fix + reverse geocoding before showing anything. `LocationService`
caches the last fix in-memory (`currentLocation`, `LocationService.kt:28`), but it is only
written, never read as a cache. The result: the screen shows a loading state on every GPS
open, even when the user has not moved.

## Goals

1. Home opens instantly when GPS is selected by returning the last known location first.
2. A fresh high-accuracy fix still runs, but in the background.
3. The fresh fix updates the displayed location **only when it differs** from the last known
   location by more than 1 km (reusing `LocationService.isDifferentThen()`).
4. The last GPS fix survives process death (persisted to DataStore) so cold starts are also
   instant.

## Non-Goals

- Changing the manual-location selection path.
- Changing the GPS trigger policy: a fresh fix is launched on every GPS home open (no TTL).
- Changing `isDifferentThen()` semantics or its 1 km threshold.

## Approach

### 1. Persistence layer — `LocationsDataStore`

Add a `LAST_GPS` preference key in `LocationsDataStore.Keys`
(`prayer_location/.../data/LocationsDataStore.kt:22-26`) storing the serialized
`LocationData` (already `@Serializable`).

New methods:

- `suspend fun getLastGpsLocation(): LocationData?` — decodes and returns the persisted fix,
  or `null` when absent/blank/unparseable (mirror `decodeEntries` runCatching pattern).
- `suspend fun setLastGpsLocation(location: LocationData)` — encodes and writes it.

### 2. `LocationService`

Add a non-suspend accessor for the existing in-memory cache:

- `fun getLastKnownLocation(): LocationData? = currentLocation`

Keep `getCurrentLocation()` unchanged (it remains the fresh high-accuracy fix). Keep
`isDifferentThen()` unchanged; after a fresh fix `currentLocation` holds the new value, so
`isDifferentThen(cached)` compares the fresh fix against the previously known location.

### 3. `LocationsCoordinator` (core change)

Constructor gains a background `CoroutineScope` (last parameter, with a default so existing
call sites/tests keep compiling; provided by Koin). This scope is app-scoped and survives
navigation, so the background refresh completes even if the user leaves the Home screen.

New private helper:

```
private fun cacheAndLaunchRefresh(cached: LocationData) {
    _gpsLocation.value = cached
    activeLocationProvider.set(cached)
    scope.launch {
        val fresh = locationService.getCurrentLocation() ?: return@launch
        if (locationService.isDifferentThen(cached)) {
            _gpsLocation.value = fresh
            locationsDataStore.setLastGpsLocation(fresh)
            activeLocationProvider.set(fresh)
        }
    }
}
```

`resolveInitial()` and `resolveSelected()` GPS branch become:

```
val cached = locationsDataStore.getLastGpsLocation() ?: _gpsLocation.value
if (cached != null) {
    cacheAndLaunchRefresh(cached)
    return cached
}
return refreshGps()  // no cache (first-ever run) → blocking fresh fix
```

`refreshGps()` also persists the fresh fix:

```
suspend fun refreshGps(): LocationData? {
    val gps = locationService.getCurrentLocation() ?: return null
    _gpsLocation.value = gps
    locationsDataStore.setLastGpsLocation(gps)
    return gps
}
```

### 4. `HomeViewModel`

Unchanged. `resolveInitial()` / `resolveSelected()` now return the cached location
immediately, so no loading flash. The background fix updates `_gpsLocation`, which flows
through the existing `observeState()` observer (`HomeViewModel.kt:61-68`) → `handleState()`
→ silently reloads prayer data only when the GPS entry actually changes.

## Data Flow

```
Home open (GPS selected)
  └─ LocationsCoordinator.resolveInitial()/resolveSelected()
      ├─ cached = DataStore.getLastGpsLocation() ?: _gpsLocation.value
      ├─ if cached != null → return cached instantly + launch background refresh
      │      └─ background: fresh = LocationService.getCurrentLocation()
      │            └─ isDifferentThen(cached)?
      │                  ├─ true  → _gpsLocation.update + DataStore.setLastGpsLocation + provider.set
      │                  └─ false → no-op (data-class equality prevents StateFlow re-emit anyway)
      └─ else (first run) → refreshGps() blocking, persisted
```

## Testing

- `LocationsCoordinatorTest`: update constructor for the new scope; add cases:
  - cached-first resolution returns cached and does not call `getCurrentLocation()` first;
  - background refresh updates location + persists when `isDifferentThen()` returns true;
  - background refresh does not update when `isDifferentThen()` returns false;
  - fresh fix via `refreshGps()` persists to DataStore;
  - first-ever run (no cache) still blocks and resolves from GPS.
- New `LocationsDataStore` test for `getLastGpsLocation`/`setLastGpsLocation` round-trip and
  corrupt-value handling.
- Existing tests that assert GPS fallback when no cache exists
  (`LocationsCoordinatorTest.kt:79-100`) remain valid.

## Risks / Edge Cases

- **Same-location fix** does not re-emit: `LocationData` is a data class with structural
  equality, and `isDifferentThen()` guards the update, so the StateFlow only re-emits on real
  change.
- **First-ever run**: no persisted or in-memory cache → one blocking fix, matching current
  behavior.
- **Background scope lifecycle**: app-scoped coroutine is fire-and-forget; it is bounded (one
  fix) and cancels on process death.
- **Overlapping refreshes**: each Home open launches one bounded refresh; a duplicate in-flight
  fix only ever applies if it differs, so races are benign.
