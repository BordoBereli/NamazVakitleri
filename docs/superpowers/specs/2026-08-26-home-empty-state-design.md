# Home: Dedicated Empty State When No Locations Exist

Date: 2026-08-26

## Problem

When the app has no saved locations (fresh install, or all locations deleted), the home page falls into the generic error state (`HomeScreenGate.Error`) with a GPS-focused message: "Konum alınamadı. Lütfen GPS'i etkinleştirin ve uygulamayı yeniden başlatın." (Location could not be obtained. Please enable GPS and restart the app.)

This is misleading and a poor UX:

- The message blames GPS even when the real problem is simply that no location has been added.
- The "Retry" button is a dead-end for the no-locations case — it re-runs location resolution, which fails again.
- "No locations" is an expected, recoverable state, not an error. It deserves its own screen.

## Goal

- Show a dedicated, full-screen empty state on the home page when there are no saved locations.
- The empty state offers two actions:
  - **Add Location** — navigates to the location selection screen.
  - **Use My Location** — checks the location permission; if not granted, requests it; if granted, resolves GPS.
- The empty state replaces the permission-first UI: on first launch with no locations, the user sees the empty state (not the permission rationale screen). Permission is requested on demand via "Use My Location".
- Transient errors (network/GPS failures with locations present) continue to use the existing error state.

## Approach

Extend the existing gate pattern (Approach A from brainstorming):

1. Add a distinct `Empty` state to `HomeScreenGate` and `HomeUiState`.
2. The ViewModel sets `Empty` (instead of `Error`) whenever there is no active location.
3. A new `HomeEmptyContent` composable renders full-screen when there are no entries.
4. Modify the shared `PermissionHandler` (only consumer is `HomeScreen`) to expose permission actions to its content so the empty state can request permission on demand.

## Design

### 1. State layer

`prayer_feature/home/.../state/HomeScreenGate.kt`:

```kotlin
sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Empty : HomeScreenGate
    data object Ready : HomeScreenGate
}
```

`prayer_feature/home/.../state/HomeUiStates.kt`:

```kotlin
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(...) : HomeUiState()
}
```

`prayer_feature/home/.../state/HomeUiStateMerger.kt` — `mergeToHomeUiState` maps `HomeScreenGate.Empty` → `HomeUiState.Empty`.

### 2. ViewModel (`HomeViewModel.kt`)

- Add a helper:

  ```kotlin
  private fun noLocation() {
      _screenGate.value = HomeScreenGate.Empty
  }
  ```

- Replace the four "no location" `fail(...)` calls with `noLocation()`:
  - `loadInitialLocation()` — when `resolveInitial()` returns null (line ~177).
  - `loadPrayerTimesForCurrentLocation()` — when `activeId == null` (line ~208) and when `location == null` (line ~211).
  - `handleState()` — when `activeId == null` (line ~225).
- Keep `fail(...)` for real load failures (network/GPS errors, lines ~203-205 and ~239).
- Add a new event `HomeEvent.OnUseMyLocation` → `loadPrayerTimesForCurrentLocation()` (re-runs GPS resolution). A dedicated event avoids mislabeling the action as pull-to-refresh in analytics.

### 3. PermissionHandler (`core/designsystem/.../components/PermissionHandler.kt`)

- Change the `content` lambda to receive a `PermissionActions` object:

  ```kotlin
  data class PermissionActions(
      val allPermissionsGranted: Boolean,
      val requestPermission: () -> Unit
  )

  fun PermissionHandler(
      onPermissionsGranted: () -> Unit,
      onChooseLocation: (() -> Unit)? = null,
      canProceedWithoutPermission: Boolean = false,
      content: @Composable (PermissionActions) -> Unit
  )
  ```

- When showing content, pass `PermissionActions(permissionState.allPermissionsGranted) { permissionState.launchMultiplePermissionRequest() }`.
- Only consumer is `HomeScreen` — no other call sites to update.

### 4. Home screen (`HomeScreen.kt`)

- Set `canProceedWithoutPermission = true` so the empty state is reachable without permission (empty state replaces the permission-first UI).
- Thread the "Use My Location" logic:

  ```kotlin
  PermissionHandler(
      onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) },
      onChooseLocation = onChooseLocation,
      canProceedWithoutPermission = true
  ) { permissionActions ->
      LocationPager(
          ...,
          onUseMyLocation = {
              if (permissionActions.allPermissionsGranted) onEvent(HomeEvent.OnUseMyLocation)
              else permissionActions.requestPermission()
          }
      )
  }
  ```

- When permission is granted via the request, the existing `LaunchedEffect(permissionState.allPermissionsGranted)` fires `onPermissionsGranted` → `HomeEvent.OnPermissionsGranted` → GPS resolution.

### 5. Empty state composable

New file `prayer_feature/home/.../components/HomeEmptyContent.kt`, mirroring `HomeErrorContent`:

- Location icon (e.g. `Icons.Outlined.LocationOn`).
- Message: reuse existing `no_location_selected` string ("No location selected. Please choose a location to see prayer times.").
- **Add Location** button → `onAddLocation`.
- **Use My Location** button → `onUseMyLocation`.

`LocationPager.kt`: when `entries.isEmpty()`, render `HomeEmptyContent` full-screen and skip the chips row + pager.

### 6. Strings

Add to the home module resources (`prayer_feature/home/src/main/res/values*/strings.xml`, all 15 locales), reusing the settings module's existing translations:

- `add_location` — "Add location"
- `use_my_location` — "Use My Location"

Reuse the existing `no_location_selected` string for the message.

## Error Handling

- No locations → `HomeScreenGate.Empty` → empty state UI (not an error).
- Transient load failures (network/GPS) with locations present → `HomeScreenGate.Error` → existing `HomeErrorContent`.
- "Use My Location" with permission not granted → system permission dialog; on grant, GPS resolves; on deny, stays on empty state.
- "Use My Location" with permission permanently denied → request is a no-op (system will not re-prompt); user can still use "Add Location". Acceptable for this scope.

## Testing

- `HomeViewModelTest`:
  - Update `empty locations state after initial emission shows error` → asserts `HomeScreenGate.Empty`.
  - Add: `resolveInitial returns null sets Empty`.
  - Add: `loadPrayerTimesForCurrentLocation with no location sets Empty`.
- `HomeUiStateMergerTest`: add `Empty gate returns HomeUiState.Empty`.
- `HomeScreenTest` (Robolectric): add `renders empty state with add location and use my location`.
- `PermissionHandler` has no dedicated test; its behavior is covered via `HomeScreenTest`.
