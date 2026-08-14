# Multiple Locations with Per-Location Home Screens — Design

- **Date**: 2026-08-14
- **Status**: Approved for implementation
- **Scope**: `prayer_location`, `prayer:model`, `prayer_feature/home`, `prayer_feature/settings`, `prayer_feature/prayertimes`, `prayer_feature/qibla` — support unlimited locations, each with its own home screen page, visually distinguishing the auto GPS location from manual ones.

## Problem Statement

The app currently supports a **single** location. `LocationCoordinator` (`prayer_feature/home`) resolves one source-of-truth location with precedence settings → saved → GPS, and the home screen renders one prayer grid. The monthly prayer-times screen and qibla screen each read the single saved location via `GetSavedLocationUseCase`.

The user wants to:
1. Add **multiple locations**, each showing its own prayer times individually ("multiply the daily prayer times").
2. **Distinguish** the auto GPS-detected location from locations set up manually in settings (via a distinct visual style).
3. Add **as many locations as possible** (no hard limit).

## Goals

- Support an unlimited list of locations, each with its own full home screen page.
- Home becomes a swipeable `HorizontalPager` with tap-able location chips.
- Visually distinguish the auto GPS location (accent + "GPS" label) from manual locations.
- Manage locations from Settings (the existing Location item becomes a "My Locations" manager).
- One auto GPS location, enabled via an optional toggle, updating with the existing drift prompt.
- Monthly prayer-times and qibla screens follow the currently selected location.
- All changes TDD (RED→GREEN per project patterns).

## Chosen Approach

**Locations domain + persistence live in `prayer_location`** (per user preference). A `LocationsCoordinator` replaces the home feature's `LocationCoordinator`. A shared `ActiveLocationProvider` singleton exposes the selected location so monthly/qibla follow it. `prayer_settings` only gains the "My Locations" manager screen UI. Home renders the pager.

Module boundaries (respecting existing dependency direction `prayer_settings → prayer_location → prayer:data`):

| Module | Responsibility |
|--------|---------------|
| `prayer:model` | New `LocationEntry` model |
| `prayer_location` | `LocationsDataStore` (persistence), `LocationsCoordinator`, `ActiveLocationProvider`, GPS auto-location synthesis |
| `prayer_feature/home` | Pager + chips UI; `HomeViewModel` observes coordinator |
| `prayer_feature/settings` | "My Locations" manager screen |
| `prayer_feature/prayertimes` | Reads `ActiveLocationProvider`; per-location cache + month position |
| `prayer_feature/qibla` | Reads `ActiveLocationProvider`; recalculates on change |

## Architecture

### 1. Data model & persistence

New model in `prayer:model` (`com.kutluoglu.prayer.model.location`):

```kotlin
@Serializable
data class LocationEntry(
    val id: String,                 // stable UUID
    val location: LocationData,     // lat/long/city/country/countryCode/county
    val isAutoGps: Boolean = false, // true = the auto GPS location
    val displayName: String         // name shown in chips
)
```

New `LocationsDataStore` in `prayer_location` — Preferences DataStore with a JSON-serialized list (same pattern as `PrayerTimesCache` in `prayer:data`):

```kotlin
data class LocationsState(
    val entries: List<LocationEntry>,
    val gpsEnabled: Boolean,     // the auto-GPS toggle
    val selectedId: String?      // active location for monthly/qibla
)
```

- Keys: `locations` (JSON list), `gps_enabled` (Boolean), `selected_location_id` (String)
- API: `observeLocations(): Flow<LocationsState>`, `getLocations(): LocationsState`, `addLocation(entry)`, `removeLocation(id)`, `reorderLocations(ids)`, `setGpsEnabled(Boolean)`, `setSelectedLocation(id)`
- **Migration**: on first read, if the list is empty but a legacy saved location exists (from `LocationDataStore` in `prayer:data`), migrate it in as a manual location.
- **Build change**: `prayer_location` adds `androidx.datastore.preferences` + `kotlinx.serialization.json` deps (currently has neither).

### 2. Domain logic (`prayer_location`)

**`LocationsCoordinator`** (replaces `LocationCoordinator` in `prayer_feature/home`):
- Deps: `LocationsDataStore`, `LocationService`
- Source of truth: observes `LocationsDataStore`, exposes `StateFlow<LocationsState>`
- **GPS auto location**: when `gpsEnabled`, computes the current GPS position via `LocationService` and exposes it as a synthetic `LocationEntry(isAutoGps = true)` at the front of the list (not persisted — recomputed on each refresh)
- **Drift detection**: reuses `LocationService.isDifferentThen()` — when GPS drifts from the last known GPS position, sets the existing drift prompt
- **Selection**: `selectLocation(id)` updates `selectedId`; the pager page change calls this
- **List ops**: `addLocation`, `removeLocation`, `reorderLocations` delegate to the data store
- **Resolution**: `resolveInitial()` returns the selected location (or first entry, or GPS fallback) — replaces the old settings→saved→GPS precedence

**`ActiveLocationProvider`** (singleton in `prayer_location`):
- Exposes `StateFlow<LocationData?>` for the currently selected location
- `LocationsCoordinator` updates it on selection change
- `prayer_feature:prayertimes` and `prayer_feature:qibla` read from it instead of `GetSavedLocationUseCase`

**Retired**: the old `LocationCoordinator` in `prayer_feature/home`. The single-location `GetSavedLocationUseCase`/`SaveLocationUseCase`/`ObserveLocationUseCase` remain for migration/legacy only.

### 3. Home UI (`prayer_feature/home`)

- `HomeScreen` becomes a `HorizontalPager`; each page = one location's full home content (prayer grid, countdown, quran verse, location info) — reuses existing per-location composables.
- **Location chips row** above the pager: one chip per location, tap to jump (`pagerState.animateScrollToPage`). Includes a "+ Add" chip → navigates to the settings location manager.
- **Page change** → `LocationsCoordinator.selectLocation(id)` so monthly/qibla follow.
- **Visual distinction**: GPS auto location gets a distinct chip/card treatment (location icon + "GPS" label, accent color); manual locations get a neutral style. Driven by `isAutoGps`.
- **Countdown**: runs only for the currently visible page (the active location). Restarts when the page changes.
- **Quran verse**: stays shared (one verse shown on the active page) — not location-specific.
- **`HomeViewModel`**: stays a thin orchestrator — observes `LocationsCoordinator` state, loads the selected location's prayer times via the existing `PrayerTimesLoader`, starts countdown for the active page.

### 4. Settings manager (`prayer_feature/settings`)

The existing **Location item** in `SettingsScreen` becomes the entry point to a **"My Locations"** manager screen:
- **List** of all locations (reorder via drag handles, delete via swipe/trash icon)
- **Add location**: reuses the existing location search flow (`LocationSelectionScreen` / `SearchLocationUseCase`) — picking a location adds it to the list
- **GPS toggle**: a switch at the top ("Use my current location") toggling `gpsEnabled`. When on, the auto GPS location appears as the first entry (marked GPS, non-deletable)
- **Selected location**: tapping a location marks it active (updates `selectedId`), so home opens on it next time

The manager screen lives in `prayer_feature:settings` and talks to `LocationsCoordinator` — no new persistence logic here.

### 5. Monthly & qibla follow active location

**`PrayerTimesViewModel`** (monthly screen):
- Replace `GetSavedLocationUseCase` with `ActiveLocationProvider` — reads the currently selected location
- **Per-location cache**: `monthCache` becomes `Map<locationId, Map<YearMonth, List<DailyPrayer>>>` — each location keeps its own cached months; switching locations loads from that location's cache (or fetches on miss); nothing is cleared
- **Per-location navigation position**: `selectedMonth` becomes `Map<locationId, YearMonth>` — each location remembers its month view position
- On location switch: swap to that location's cache + month position and reload

**`QiblaViewModel`**:
- Replace the single saved location with `ActiveLocationProvider`
- Recalculates qibla direction when the active location changes

Both screens depend on `prayer_location` (for `ActiveLocationProvider`). Verified: `prayer_feature:qibla` and `prayer_feature:settings` already depend on `prayer_location`; `prayer_feature:prayertimes` does **not** — add `implementation(project(":prayer_location"))` to its `build.gradle.kts`.

## Data Structure Changes

```kotlin
// prayer:model
@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String
)

// prayer_location
data class LocationsState(
    val entries: List<LocationEntry>,
    val gpsEnabled: Boolean,
    val selectedId: String?
)
```

## Error Handling

- `LocationsCoordinator.resolveInitial()` returns `null` when no location is resolvable (no entries, GPS disabled/unavailable) → home shows the existing error gate.
- GPS auto-location synthesis failure (permission denied / GPS unavailable) → GPS entry omitted; manual entries still render.
- DataStore corruption handled by the existing `ReplaceFileCorruptionHandler` pattern.

## Test Plan (TDD — every change RED→GREEN)

| Test file | Covers |
|-----------|--------|
| `LocationsDataStoreTest` | add/remove/reorder, GPS toggle, selected id, JSON round-trip, migration from legacy saved location |
| `LocationsCoordinatorTest` | list state exposure, GPS auto-location synthesis, drift prompt, selection updates `ActiveLocationProvider`, `resolveInitial` precedence |
| `ActiveLocationProviderTest` | emits selected location, updates on selection change |
| `HomeViewModelTest` (updated) | pager selection drives prayer loading, countdown restarts on page change |
| `PrayerTimesViewModelTest` (updated) | per-location cache isolation, per-location month position, location switch reload |
| `QiblaViewModelTest` (updated) | recalculates on active location change |
| Settings manager VM tests | add/remove/reorder/toggle/select-active |

## Impact Assessment

- **HIGH-ish risk surface** (many consumers): `LocationCoordinator` is replaced by `LocationsCoordinator`; `HomeViewModel`, `HomeRoute`, `HomeScreen` change. Monthly/qibla ViewModels change their location source.
- Mitigation: `LocationsCoordinator`/`ActiveLocationProvider` are new components with their own tests; existing `PrayerTimesLoader`, `CountdownEngine`, `QuranVerseLoader` are reused unchanged.
- Run `gitnexus_impact` on `LocationCoordinator`, `HomeViewModel`, `PrayerTimesViewModel`, `QiblaViewModel` before editing each.

## Out of Scope

- No changes to prayer calculation, localization, or error messages.
- No notification changes (`notificationEnabled` on `Prayer` stays as-is).
- No changes to the quran verse feature beyond keeping it shared across pages.
- Legacy single-location DataStores (`SettingsDataStore` location keys, `LocationDataStore`) are not removed — only used for migration.
