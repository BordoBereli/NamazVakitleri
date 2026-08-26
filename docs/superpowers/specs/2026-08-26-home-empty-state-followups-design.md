# Home Empty State — Follow-up Fixes Design

> **Status:** Approved
> **Date:** 2026-08-26
> **Base feature:** `docs/superpowers/specs/2026-08-26-home-empty-state-design.md` (implemented, merged to `main`)

## Problem

The home empty-state feature (merged) shipped with four follow-up items flagged during final review:

1. **Launch flash for existing users** — `LocationPager` renders `HomeEmptyContent` whenever `entries.isEmpty()`, regardless of `uiState`. On every launch the gate starts as `Loading` with empty `LocationsState`, so users *with* saved locations see the actionable empty state flash briefly before entries populate.
2. **Permission-first UI bypassed** — `HomeScreen` sets `canProceedWithoutPermission = true`, making `PermissionRationale`, `PermissionFirstLaunchOrDenied`, and `PermissionPermanentlyDenied` unreachable dead code. A user who permanently denied permission ("Don't ask again") tapping "Use My Location" gets a silent no-op (`launchMultiplePermissionRequest()` auto-denies).
3. **No interaction test** — `HomeScreenTest` verifies the empty state renders but not the `onUseMyLocation` wiring (granted → event; permanently denied → settings; else → request permission).
4. **No analytics** — `OnUseMyLocation` is not logged, unlike `OnRefresh` (logs `PULL_TO_REFRESH`). The `USE_MY_LOCATION` event constant already exists in `AnalyticsEvents` but is never emitted.

## Design

### 1. Loading indicator instead of empty-state flash

**File:** `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`

Change the empty-state branch (currently `if (entries.isEmpty()) { HomeEmptyContent(...); return }`) to:

```kotlin
if (entries.isEmpty()) {
    if (uiState is HomeUiState.Loading) {
        LoadingIndicator()
    } else {
        HomeEmptyContent(
            onAddLocation = onAddLocation,
            onUseMyLocation = onUseMyLocation,
            permissionDenied = permissionDenied
        )
    }
    return
}
```

- `LoadingIndicator()` already exists in `core/designsystem/components/CommonUi.kt` (full-screen centered `CircularProgressIndicator`).
- `LocationPager` gains a `permissionDenied: Boolean` parameter (see #2) passed through to `HomeEmptyContent`.

**Test** (`HomeScreenTest`): `uiState = HomeUiState.Loading` + empty `LocationsState` → "Add location" and "Use My Location" are **not** displayed.

### 2. Permanently-denied permission UX

**Files:**
- `core/designsystem/src/main/java/com/kutluoglu/core/designsystem/components/PermissionHandler.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- `prayer_feature/home/src/main/res/values/strings.xml` + 14 locale files

**`PermissionActions`** gains a `permanentlyDenied` flag:

```kotlin
data class PermissionActions(
    val allPermissionsGranted: Boolean,
    val permanentlyDenied: Boolean,
    val requestPermission: () -> Unit
)
```

In `ShowOf`, compute it as:

```kotlin
permanentlyDenied = !permissionState.allPermissionsGranted &&
    !permissionState.shouldShowRationale &&
    permissionResultReceived
```

(`permissionResultReceived` is the existing `remember` state distinguishing first-launch from a real denial.)

**`HomeEmptyContent`** gains `permissionDenied: Boolean = false`. When true, a hint `Text` is shown under the buttons:

> Location permission is off. Use My Location will open settings.

New string resource `permission_denied_hint` (default + 14 locales).

**`HomeScreen`** wires `onUseMyLocation`:

```kotlin
onUseMyLocation = {
    when {
        permissionActions.allPermissionsGranted -> onEvent(HomeEvent.OnUseMyLocation)
        permissionActions.permanentlyDenied -> openAppSettings()
        else -> permissionActions.requestPermission()
    }
}
```

`openAppSettings()` launches `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with the app package URI (same pattern as the existing `PermissionPermanentlyDenied` composable).

### 3. Interaction test

**File:** `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- **Granted path:** grant `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` via Robolectric (`ShadowApplication.grantPermissions`), tap "Use My Location", assert `onEvent(HomeEvent.OnUseMyLocation)` fired.
- **Denied path:** deny permissions, tap "Use My Location", assert `OnUseMyLocation` **not** fired.

### 4. Analytics

**Files:**
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`

`OnUseMyLocation` branch logs the existing event before reloading:

```kotlin
HomeEvent.OnUseMyLocation -> {
    analyticsTracker.logEvent(AnalyticsEvents.USE_MY_LOCATION)
    loadPrayerTimesForCurrentLocation()
}
```

**Test:** verify `analyticsTracker.logEvent(AnalyticsEvents.USE_MY_LOCATION)` is called when `OnUseMyLocation` is handled.

## Files Touched

| File | Change |
|------|--------|
| `core/designsystem/.../components/PermissionHandler.kt` | `PermissionActions.permanentlyDenied` |
| `prayer_feature/home/.../components/HomeEmptyContent.kt` | `permissionDenied` param + hint text |
| `prayer_feature/home/.../LocationPager.kt` | Loading branch + `permissionDenied` param |
| `prayer_feature/home/.../HomeScreen.kt` | `onUseMyLocation` 3-branch wiring + `openAppSettings` |
| `prayer_feature/home/.../HomeViewModel.kt` | Log `USE_MY_LOCATION` |
| `prayer_feature/home/.../res/values*/strings.xml` | `permission_denied_hint` (15 files) |
| `prayer_feature/home/.../HomeScreenTest.kt` | Loading + interaction tests |
| `prayer_feature/home/.../HomeViewModelTest.kt` | Analytics test |

## Out of Scope

- Removing the now-unreachable `PermissionRationale` / `PermissionFirstLaunchOrDenied` / `PermissionPermanentlyDenied` composables. They remain reachable if `canProceedWithoutPermission` is ever set back to `false` (e.g., a future design change) and are shared designsystem components. Revisit if they stay dead after this change.
- AGENTS.md test-task-name drift (`unitTests`/`testSuites`/`allTests` don't exist as Gradle tasks). Documented separately.
