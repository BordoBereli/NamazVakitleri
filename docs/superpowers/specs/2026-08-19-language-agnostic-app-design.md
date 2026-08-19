# Language-Agnostic App (Follow System + In-App Override)

## Problem

The Settings screen has a Language feature that lists 15 languages and persists the
selection to DataStore, but the saved value is **never applied** to the app's locale.
`onLanguageSelected` in `SettingsGraph.kt:69-72` only pops the back stack. The app relies on
Android's default resource resolution, so texts follow the phone language regardless of the
in-app setting. Additionally, the default language is hardcoded to `"tr"` in the settings
stores and domain model, and translations are partial (only `tr`/`ar`/`fr`/`de`/`in` in
`core/designsystem`; only `tr` in `home`/`qibla`/`prayertimes`/`prayer_navigation`; none in
`prayer_feature/settings`).

## Goals

1. Default behavior: the app follows the phone's system language (no explicit choice).
2. When the user selects a language in Settings → Language, it overrides the system language
   and applies to **all** app texts across every module.
3. The change applies immediately (activity recreation), no app restart required.
4. A "Follow system" entry is available in the picker so the user can return to the phone
   language at any time.
5. All 15 languages listed in the picker have translations for all UI strings.

## Non-Goals

- Translating the 114 surah names into all languages — they stay as transliterations
  (already present in `values/` and `values-tr`).
- Migrating `MainActivity` to `AppCompatActivity` or using `AppCompatDelegate`.
- Using the `LocaleManager` (API 33+) per-app-locale API.
- Changing the calculation-method, location, or hijri-adjustment settings paths.

## Approach

### 1. Data model — `"system"` as the default

Change the default `language` value from `"tr"` to `"system"` in:

- `prayer_settings/.../data/local/SettingsDataStore.kt` (`observeSettings()` and
  `getSettings()`).
- `prayer_cache/.../SettingsDataStoreImp.kt` (`buildSettingsJson()` and
  `buildDefaultSettingsJson()`).
- `prayer_settings/.../domain/model/Settings.kt` (`language: String = "system"`).

Semantics: `"system"` = follow the phone's language; any other code (e.g. `"tr"`, `"ar"`) =
explicit override.

### 2. `LocaleManager` (new, in `app` module)

New Koin `@Single` class `LocaleManager`:

- Holds an in-memory `@Volatile var languageCode: String = "system"` for fast synchronous
  reads (needed because `attachBaseContext` runs before Koin is ready).
- `fun applyLocale(context: Context): Context` — returns `context` unchanged when
  `languageCode == "system"`; otherwise wraps it via `createConfigurationContext` with the
  resolved `Locale`.
- `fun setLanguage(code: String)` — updates the holder synchronously (called before
  `recreate()`).
- `fun resolveLocale(): Locale` — maps the code to a `Locale` (used by `applyLocale`).
- Loads the persisted language from DataStore in `Application.onCreate` (after Koin starts)
  and writes it into the holder.

Wiring:

- `NamazVakitleriApplication.attachBaseContext` and `MainActivity.attachBaseContext` call
  `LocaleManager.applyLocale(base)` — reading only the holder, no Koin dependency at that
  point.
- On language change: `LanguageSelectionViewModel` persists via the existing
  `UpdateLanguageUseCase` → the app-module callback calls `LocaleManager.setLanguage(code)`
  then `activity.recreate()` → all texts update instantly.

### 3. Settings UI — "Follow system" entry

- Add a `Language("system", ...)` entry at the top of the `languages` list in
  `LanguageSelectionScreen.kt` (name/nativeName resolved from string resources, e.g.
  "System default" / "Follow device language").
- `LanguageSelectionViewModel`: `currentLanguageCode` defaults to `"system"`; selecting the
  system entry saves `"system"` to DataStore.
- `onLanguageSelected` is threaded up to `MainAppScreen` (app module), which owns the
  `LocaleManager` and the `recreate()` call — keeping dependency direction correct
  (app → settings feature).

### 4. Translations — all 15 languages

Add `values-{ur,ms,fa,bn,hi,ta,th,ru,es}` to `core/designsystem` (already has
`tr/ar/fr/de/in`). Add the full 15-language set to `home`, `qibla`, `prayertimes`,
`prayer_navigation/core`, and `prayer_feature/settings` (settings currently has zero
translations). Surah names stay transliterated. The `prayers` string-array in
`core/designsystem` is translated for all 15 languages.

## Data Flow

```
App start
  └─ Application.onCreate → Koin start → LocaleManager loads persisted language into holder
  └─ Application/MainActivity.attachBaseContext → LocaleManager.applyLocale(base)
       ├─ "system" → base unchanged (phone language)
       └─ "tr"/"ar"/... → createConfigurationContext(locale)

Settings → Language → user taps a language
  └─ LanguageSelectionViewModel.selectLanguage
       └─ UpdateLanguageUseCase(language) → DataStore.saveLanguage (persists)
       └─ emits selectedLanguage
  └─ LanguageSelectionRoute collects → onLanguageSelected(language)
       └─ (app module) LocaleManager.setLanguage(language) → activity.recreate()
            └─ new attachBaseContext → applyLocale → all texts in new locale
```

## Testing

- `LocaleManager` unit tests:
  - `"system"` → `applyLocale` returns the context unchanged / resolves to device locale.
  - explicit code (e.g. `"ar"`) → `applyLocale` wraps with the matching locale.
  - `setLanguage` updates the holder synchronously.
- `LanguageSelectionViewModelTest`: default selection is `"system"`; selecting the system
  entry persists `"system"`; selecting a language persists its code.
- `SettingsDataStoreTest` (prayer_settings) and any `SettingsDataStoreImp` tests: default
  language is `"system"`.
- `SettingsTest` (domain model): default `language == "system"`.

## Risks / Edge Cases

- **`attachBaseContext` before Koin**: solved by reading only the in-memory holder, which is
  populated at startup and on change.
- **Race between save and recreate**: `setLanguage` updates the holder synchronously before
  `recreate()`, so the recreated activity's `attachBaseContext` always sees the new value.
- **Untranslated strings**: Android resource resolution falls back to `values/` (English) for
  any missing key, so partial translations degrade gracefully.
- **RTL languages** (ar, fa, ur): `createConfigurationContext` with the locale also flips
  layout direction automatically.
- **Existing users with `"tr"` persisted**: they keep Turkish (explicit override), which is
  correct — only the *default* changes to `"system"`.
