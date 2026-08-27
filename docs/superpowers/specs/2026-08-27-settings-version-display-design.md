# Settings Page — Version Display Design

**Date:** 2026-08-27
**Status:** Approved

## Goal

Show the app version name and code to the user on the Settings page, as an informative "about" footer at the bottom of the list.

## Approach

Use **BuildConfig + Koin provider** (Option A):

1. Enable `buildFeatures { buildConfig = true }` in `app/build.gradle.kts`.
2. Add a small `AppVersion` data class in `core:common`:

   ```kotlin
   data class AppVersion(
       val name: String,
       val code: Int
   )
   ```

3. Expose it as a Koin `single` in `AppModule`:

   ```kotlin
   single {
       AppVersion(
           name = BuildConfig.VERSION_NAME,
           code = BuildConfig.VERSION_CODE
       )
   }
   ```

4. Inject `AppVersion` into `SettingsViewModel` and expose it in the UI state.

## UI

At the bottom of the settings list (below the Clear Cache card), a centered "about" footer:

- App icon (rounded, small)
- App name ("Namaz Vakitleri") in `titleMedium`
- Version line: version name + code (e.g., "Sürüm 2.0.0 (200)") — in `bodySmall`, muted color. The label ("Version") must be a localized string resource; name and code are appended.
- Lightweight, no card border

## Files Changed

- `app/build.gradle.kts` — enable `buildConfig`
- `core/common/.../AppVersion.kt` — new data class
- `app/.../AppModule.kt` — Koin `single` for `AppVersion`
- `prayer_feature/settings/.../SettingsContract.kt` — add version to `Success` state
- `prayer_feature/settings/.../SettingsViewModel.kt` — inject `AppVersion`, populate state
- `prayer_feature/settings/.../SettingsScreen.kt` — render footer
- `prayer_feature/settings/.../SettingsScreenTest.kt` — update tests
- `prayer_feature/settings/.../SettingsViewModelTest.kt` — update tests

## Testing

- Update `SettingsViewModelTest` to verify version name/code are exposed in `Success` state.
- Update `SettingsScreenTest` to assert the version footer renders the name and code.
