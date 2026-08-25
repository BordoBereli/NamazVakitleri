# Force & Optional Update Feature

Date: 2026-08-25

## Overview

Add an in-app update mechanism that supports two modes:

- **Force update**: when the installed version is below a configurable minimum, the app shows a non-dismissible dialog and blocks usage until the user updates.
- **Optional update**: when a newer version is available but the installed version is still above the minimum, the app shows a dismissible dialog that re-nags on the next launch.

The app is distributed via both Google Play Store and direct APK/AAB sideload, so the update flow must work for both channels.

## Problem

There is currently no way to notify users about new releases or to block usage of a critically broken version. The app is pre-release (versionCode 1, versionName "1.0") and will be distributed through Play Store and direct APK/AAB, so a Play-Store-only mechanism (In-App Updates API) is insufficient.

## Goal

- Detect the latest and minimum supported versions from a remote source (Firebase Remote Config).
- Show a non-dismissible blocking dialog when the installed version is below the minimum.
- Show a dismissible dialog when a newer (non-mandatory) version is available; re-nag on the next launch if dismissed.
- Open the correct update destination based on install source (Play Store listing vs direct download URL).
- Fail silently (no blocking, no error UI) when the update check cannot complete.

## Approach

- New module `:app_update` following the project's Clean Architecture conventions (mirrors `prayer_settings` / `prayer_notifications`).
- Firebase Remote Config is the source of truth for update metadata (latest/min version codes, release notes, direct download URL).
- Version comparison uses `versionCode` (int) — monotonic and reliable; `versionName` is used only for display.
- `MainAppScreen` hosts the `UpdateViewModel` and the dialogs so they overlay the whole app.
- Update checks run on app launch and on resume (per user preference).

## Design

### 1. Module structure

New module `:app_update`:

```
app_update/src/main/java/com/kutluoglu/app_update/
├── di/AppUpdateModule.kt              # Koin module
├── domain/
│   ├── model/UpdateInfo.kt            # latestCode, minCode, releaseNotes, directUrl
│   ├── repository/UpdateRepository.kt # interface
│   └── usecase/CheckForUpdateUseCase.kt
├── data/
│   ├── UpdateRepositoryImpl.kt
│   ├── UpdateInfoRemoteDataSource.kt  # Firebase Remote Config
│   └── InstallSourceDetector.kt       # Play Store vs sideload
└── ui/
    ├── UpdateViewModel.kt
    ├── UpdateUiState.kt               # sealed: NoUpdate / OptionalUpdate / ForceUpdate
    └── UpdateDialogs.kt               # ForceUpdateDialog + OptionalUpdateDialog
```

### 2. Model

```kotlin
data class UpdateInfo(
    val latestVersionCode: Int,
    val minVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val directDownloadUrl: String,
)
```

### 3. Remote Config keys

| Key | Type | Purpose |
|-----|------|---------|
| `update_latest_version_code` | Long | Latest available `versionCode` |
| `update_min_version_code` | Long | Minimum supported `versionCode` |
| `update_latest_version_name` | String | Latest `versionName` for display |
| `update_release_notes` | String | "What's new" text shown in dialogs |
| `update_direct_download_url` | String | Direct APK/AAB download URL for sideloaded installs |

`UpdateInfoRemoteDataSource` reads these via `FirebaseRemoteConfig` (fetch + activate with a short cache expiration, e.g. 0–5 minutes). Missing/invalid values are handled gracefully (treated as no update).

### 4. Install source detection

`InstallSourceDetector`:
- `PackageManager.getInstallerPackageName(packageName) == "com.android.vending"` → Play Store install.
- Play Store installs open `market://details?id=<package>` with an `https://play.google.com/store/apps/details?id=<package>` fallback.
- Sideloaded installs open the Remote Config `update_direct_download_url`.

### 5. Use case

`CheckForUpdateUseCase`:
1. Fetch update info from the repository.
2. Compare installed `versionCode` (from `BuildConfig.VERSION_CODE`) against `minVersionCode` and `latestVersionCode`.
3. Return `UpdateDecision`:
   - `ForceUpdate(info)` when installed < min.
   - `OptionalUpdate(info)` when installed >= min and installed < latest.
   - `NoUpdate` otherwise (including fetch failure or invalid config).

### 6. ViewModel & UI state

```kotlin
sealed interface UpdateUiState {
    data object NoUpdate : UpdateUiState
    data class OptionalUpdate(val info: UpdateInfo) : UpdateUiState
    data class ForceUpdate(val info: UpdateInfo) : UpdateUiState
}
```

`UpdateViewModel`:
- Exposes `uiState: StateFlow<UpdateUiState>`.
- `checkForUpdate()` dedupes concurrent checks (single in-flight check; latest result wins).
- `onOptionalUpdateDismissed()` → state becomes `NoUpdate` for the current session.
- `onUpdateClicked()` → opens the update destination via `InstallSourceDetector`; on failure, keeps the dialog and exposes a retry state.

### 7. UI

- `MainAppScreen` hosts `UpdateViewModel`; a `LaunchedEffect` triggers `checkForUpdate()` on launch and on resume.
- **ForceUpdateDialog**: non-dismissible `AlertDialog` (`onDismissRequest = {}`, back button disabled). Shows release notes + a single **Update** button. If opening the destination fails, the dialog stays with an inline error/retry state.
- **OptionalUpdateDialog**: dismissible `AlertDialog` with **Update** and **Later** buttons. "Later" → `NoUpdate` for this session; re-nags on next launch.
- Both use the existing `NamazVakitleriTheme` / Material3 `AlertDialog` and show release notes (plain text, wrapped).
- Silent when up to date or when the check fails.

### 8. Build wiring

- Add `:app_update` to `settings.gradle.kts`.
- Add `implementation(project(":app_update"))` to `app/build.gradle.kts`.
- Add `firebase-config-ktx` (Remote Config) dependency to `:app_update`.
- Add `:app_update` to `AppModule.kt` (or its own Koin module) so the ViewModel/repository are available to the app.

## Data flow

1. `MainAppScreen` creates `UpdateViewModel` (Koin).
2. `LaunchedEffect` on launch/resume → `checkForUpdate()`.
3. `CheckForUpdateUseCase` → `UpdateRepository` → `UpdateInfoRemoteDataSource` (Remote Config) + `InstallSourceDetector`.
4. Use case compares versions → emits `ForceUpdate` / `OptionalUpdate` / `NoUpdate`.
5. UI observes `uiState` and shows the matching dialog overlaying the whole app.

## Error handling & edge cases

- Remote Config fetch fails / times out → silent `NoUpdate`; app runs normally. Never blocks on network failure.
- Remote Config values missing/invalid → treated as `NoUpdate`.
- Installed version > latest (e.g., beta/test build) → `NoUpdate`, no nagging.
- URL open fails (no browser/Play Store) → force dialog shows a retry state; optional dialog stays dismissible. No crash.
- Concurrent checks (launch + resume racing) → ViewModel dedupes; one in-flight check, latest result wins.
- Check runs on a background dispatcher; UI state updated on main. No blocking of the splash/UI thread.

## Testing

- **CheckForUpdateUseCaseTest** — version comparison boundaries: force (below min), optional (>= min, < latest), none (>= latest), missing/invalid config → none, fetch failure → none.
- **UpdateViewModelTest** — state transitions on check result; "Later" dismissal → `NoUpdate`; dedupe of concurrent checks; force dialog not dismissible.
- **InstallSourceDetectorTest** — Play Store installer → Play URL; null/other installer → direct URL.
- **UpdateInfoRemoteDataSourceTest** — maps Remote Config values to `UpdateInfo`; missing keys handled.

## Out of scope

- Google Play In-App Updates API (does not cover sideloaded installs).
- Periodic background update checks (launch + resume only).
- A manual "Check for updates" button in Settings.
- Changelog/version history screen.
