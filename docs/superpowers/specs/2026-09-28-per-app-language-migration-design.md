# Migrate Locale Handling to Android Per-App Language API — Design

> Date: 2026-09-28 · Status: Approved · Source: `docs/ARCHITECTURE_REVIEW.md` P0 — "runBlocking on main thread at cold start"

## Problem

Every cold start blocks the main thread on DataStore disk I/O **twice** before the first frame:

1. `NamazVakitleriApplication.attachBaseContext` (NamazVakitleriApplication.kt:54) — runs before Koin, creates a local `SettingsDataStore`, and `runBlocking`s on a full DataStore read.
2. `MainActivity.attachBaseContext` (MainActivity.kt:21) — repeats the same `runBlocking` DataStore read after Koin starts.

This adds directly to TTFB/ANR risk on slow devices. The `runBlocking` exists because the locale must be known synchronously at `attachBaseContext` time to render the first frame in the correct language.

## Goal

Eliminate both `runBlocking` DataStore reads by letting the OS store and apply the app language **before** any activity is created, using the Android per-app language API.

## Approach (chosen: Approach C)

Use `AppCompatDelegate.setApplicationLocales()` (Android 13+ framework API, AppCompat backport for Android 12-). The OS/AppCompat stores the language preference and applies the correct locale to each activity's base context before `attachBaseContext` runs — the same job the custom `LocaleManager` does today, but with zero startup disk I/O and no manual `activity.recreate()`.

Rejected alternatives:
- **Approach A (SharedPreferences mirror)**: fixes the P0 with a fast synchronous read, but keeps two sources of truth and the custom persistence layer. Valid fallback if C hits a blocker.
- **Approach B (async post-Koin)**: simplest code, but shows a wrong-language first frame + recreate on every cold start with a non-default language — a UX regression.

## Changes

### 1. `MainActivity`
- Superclass: `ComponentActivity` → `AppCompatActivity` (required for the Android 12- backport; `AppCompatActivity` is a `ComponentActivity` subclass, so Compose/splash/edge-to-edge are unaffected).
- Delete the `attachBaseContext` override.

### 2. Theme (`app/src/main/res/values/themes.xml`)
- `Theme.NamazVakitleri` parent: `android:Theme.Material.NoActionBar` → `Theme.AppCompat.DayNight.NoActionBar` (AppCompatActivity requires an AppCompat theme; only `windowBackground` matters for this 100% Compose app).
- `Theme.NamazVakitleri.Starting` (splash) needs no change.

### 3. Manifest (`app/src/main/AndroidManifest.xml`)
- `android:localeConfig="@xml/locales_config"` on `<application>` — enables Settings → Apps → NamazVakitleri → Language on Android 13+.
- Declare `AppLocalesMetadataHolderService` with `android:enabled="false"` metadata `autoStoreLocales=true` — AppCompat persists the language itself on Android 12-.

### 4. New `app/src/main/res/xml/locales_config.xml`
The 15 supported locales: tr, en, ar, de, fr, es, bn, fa, hi, id, ms, ru, ta, th, ur.

### 5. New `AppLocaleController` (app module, ~30 lines)
Thin wrapper mapping the app's language codes to the API:
- `"system"` → `LocaleListCompat.getEmptyLocaleList()` (follow device language)
- `"tr"`, `"ar"`, … → `LocaleListCompat.forLanguageTags(code)`
- `getApplicationLocales()` → `"system"` when the list is empty
- Registered in `AppModule` as a `@Single`.

### 6. Language switch flow
`MainAppScreen.applyLanguage` becomes a single call: `AppLocaleController.setApplicationLocales(code)`. The system recreates the activity itself — no manual `recreate()`, no flash. The existing callback chain (`LanguageSelectionViewModel` → `LanguageSelectionRoute` → `SettingsGraph` → `MainAppScreen`) stays as-is.

### 7. Deleted
- `LocaleManager` class (app/.../locale/LocaleManager.kt)
- Both `attachBaseContext` overrides (Application + MainActivity)
- `AppModule.provideLocaleManager`
- `LocaleManagerTest`, `LocaleManagerKoinTest`
- The `runBlocking` import in `NamazVakitleriApplication`

`LanguageProvider` (core/designsystem, reads `Locale.getDefault()`) stays — AppCompat updates the default locale when applying app locales.

## Migration (existing users)

One-time, flag-gated (SharedPreferences boolean `locale_migrated`), run async in `Application.onCreate`:
1. Read DataStore `language`.
2. If not `"system"` and it differs from the current app locales → `AppLocaleController.setApplicationLocales(language)`.
3. Set the flag.

The flag prevents later overriding a user who changes the language via **system settings** (a new capability this migration enables).

## Kept as-is

- DataStore `language` key + `UpdateLanguageUseCase` remain the source for the Settings screen display and analytics — no domain-layer changes.
- `LanguageProvider` consumers (Quran loader, update check, city search) are unaffected.

## Testing

- Robolectric test for `AppLocaleController` mapping (`"system"` ↔ empty list, code ↔ locale, round-trip).
- Delete `LocaleManagerTest` / `LocaleManagerKoinTest`; update `KoinGraphVerificationTest` if it references `LocaleManager`.
- Manual verification:
  - Cold start with a non-default language shows the correct first frame.
  - In-app language switch applies without double-recreate.
  - Language changed via system settings survives app restarts.
  - Migration: install previous build with non-default language → update → language preserved.

## Risks

- **AppCompat theme switch** could subtly affect non-Compose styling — mitigated: app is 100% Compose; only `windowBackground` is used from the theme.
- **`Locale.getDefault()` timing on Android 12-** — `LanguageProvider` consumers may read the default locale before AppCompat applies it; fallback is reading `AppCompatDelegate.getApplicationLocales()` directly in `LanguageProvider`.
- **Splash screen + AppCompatActivity** — verified compatible (splash screen API is activity-agnostic); manual check on cold start.
