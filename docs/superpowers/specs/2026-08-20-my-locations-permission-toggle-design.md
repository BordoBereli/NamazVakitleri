# My Locations: GPS Toggle Depends on Location Permission

Date: 2026-08-20

## Problem

On the Settings > Locations screen ("Konumlarım"), the "Mevcut Konumu Kullan" (Use My Current Location) toggle is bound directly to the persisted `gpsEnabled` flag in `LocationsState`. It does not consider whether the app actually holds the location permission. As a result:

- The toggle can appear ON even when the location permission has been revoked or never granted.
- Toggling it ON enables GPS tracking even though the app cannot obtain a location.

## Goal

- The toggle's displayed state must depend on the location permission: if the permission is not granted, the toggle must appear OFF.
- When the user toggles it ON, the app must check the location permission first. If not granted, it must request the permission. Only after the permission is granted should GPS be enabled.
- If the permission is denied (including permanently denied), the toggle stays OFF and a rationale is shown, with an option to open system settings when permanently denied.

## Approach

UI-layer permission handling in `MyLocationsRoute` (Approach A), mirroring the existing pattern in `LocationSelectionScreen.kt` (`rememberLauncherForActivityResult` + `ContextCompat`). No changes to `LocationsCoordinator`, `LocationsDataStore`, or `MyLocationsViewModel`.

The persisted `gpsEnabled` preference continues to represent the user's intent. Only the displayed toggle state is gated by the permission.

## Design

### `MyLocationsScreen.kt`

1. Add a permission launcher:

   ```kotlin
   val locationPermissionLauncher = rememberLauncherForActivityResult(
       contract = ActivityResultContracts.RequestMultiplePermissions()
   ) { permissions -> ... }
   ```

2. Add a `hasLocationPermission()` helper using `ContextCompat.checkSelfPermission` for `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`.

3. Switch state:

   - `checked = state.gpsEnabled && hasLocationPermission()`

4. `onCheckedChange`:

   - OFF -> `MyLocationsEvent.SetGpsEnabled(false)`.
   - ON -> if `hasLocationPermission()` -> `SetGpsEnabled(true)`; else launch the permission request.
     - On grant -> `SetGpsEnabled(true)`.
     - On deny -> keep OFF; show a rationale (snackbar). If permanently denied (`!shouldShowRequestPermissionRationale`), offer an "Open Settings" action that launches `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

5. Lifecycle `ON_RESUME` observer re-checks the permission so the toggle turns off if the permission is revoked while the screen is open (same approach as `PermissionHandler.kt`).

### Strings

Add any new user-facing strings (rationale text, open-settings action) to the settings module resources.

## Error Handling

- Permission denied -> toggle stays OFF, rationale shown.
- Permanently denied -> toggle stays OFF, rationale with "Open Settings" action.
- No behavioral change to GPS refresh logic in the coordinator.

## Testing

- Extend `MyLocationsScreenTest`:
  - Toggle renders OFF when permission is missing even if `gpsEnabled` is true.
  - Toggling ON with permission granted enables GPS.
  - Toggling ON without permission triggers the permission request and does not enable GPS until granted.
- Keep `MyLocationsViewModelTest` unchanged (no ViewModel changes).
