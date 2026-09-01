# Preset Cities Tab — Turkey Pin, Localization, Landscape

**Date:** 2026-09-01
**Status:** Approved
**Scope:** `LocationSelectionScreen` → Preset Cities tab

## Goal

Improve the Preset Cities tab of the Select Location screen with three changes:

1. Pin **Turkey** at the top of the "Popular Countries" section with a stronger highlight.
2. Add **multi-language support** for the preset data (country, province, and city/district names) in **Turkish, English, Arabic, and Farsi**. All other supported languages fall back to English.
3. Make the **landscape** layout user-friendly via a **two-pane master/detail** layout on the Preset tab.

## Current State

- `LocationSelectionScreen.kt` — `PresetCitiesContent` shows `CountryList` with "Popular Countries" (priority) and "Other Countries" sections. Turkey is in `PRIORITY_COUNTRIES` but sorted alphabetically within the section.
- `LocationSelectionViewModel.kt` — groups `allCities` (from `cities.json`) by English `country`; `PRIORITY_COUNTRIES` list; single-state machine `CountrySelection → CitySelection → ProvinceSelection`.
- `cities.json` — 328 cities, 68 countries, all names hardcoded in English. Turkey has 113 districts across 74 provinces (district in `name`, province in `city`).
- App supports 15 languages via `values-*` folders + `LocaleManager`; `LanguageProvider.getLanguageCode()` returns the active language.

## Design

### 1. Data model & `cities.json`

**`City` model** (`prayer/model/.../City.kt`) — add nullable localized fields (default `null`, backward compatible):

```kotlin
data class City(
    val name: String, val country: String, val latitude: Double,
    val longitude: Double, val timezone: String,
    val city: String? = null, val county: String? = null,
    val nameTr: String? = null, val nameAr: String? = null, val nameFa: String? = null,
    val countryTr: String? = null, val countryAr: String? = null, val countryFa: String? = null,
    val cityTr: String? = null, val cityAr: String? = null, val cityFa: String? = null,
)
```

- `name*` = city/district name, `city*` = province name, `country*` = country name.
- **`cities.json`**: add these fields for all 328 cities. Turkey districts are already Turkish → `nameTr` omitted (falls back to `name`). Turkey provinces (`city` field, e.g. "Istanbul") get `cityTr` ("İstanbul"), `cityAr`, `cityFa`.
- No changes to `LocationRepositoryImpl` / `CityLocalDataSource` (nullable defaults).

### 2. Localization helper & ViewModel

- **`CityLocalizer`** (settings feature): pure functions `localizedName(lang)`, `localizedCountry(lang)`, `localizedProvince(lang)` — `tr`/`ar`/`fa` → localized field, else English fallback.
- **`LocationSelectionViewModel`**: inject `LanguageProvider`; use localized names for:
  - `loadCountries()` / `searchCountries()` — display + sort by localized country name.
  - `selectCountry()` / `selectProvince()` — localized province/district display.
  - Storage/analytics keep English names (unchanged).

### 3. Turkey pinned first in "Popular Countries"

- Sort priority countries: **Turkey first**, then the rest alphabetically (by localized name).
- Turkey card gets a stronger highlight: flag emoji 🇹🇷 + `primaryContainer` background (vs. current 0.3 alpha).

### 4. Landscape two-pane (Preset tab only)

- Detect landscape via `BoxWithConstraints`.
- **Landscape + Preset tab**: left pane = "Use My Location" + tabs + country list (master); right pane = provinces → districts drill-down (detail). Carry the `countries` list through `CitySelection`/`ProvinceSelection` states so the master stays visible.
- **Portrait** (and Search/Map tabs): current single-pane behavior.

### 5. Testing

- ViewModel: Turkey pinned first; localized names for tr/ar/fa; English fallback; localized country search.
- Data: `cities.json` contains localized fields; model deserializes with defaults.

## Out of Scope

- Localizing the Search tab results (separate tab).
- Localizing the Map tab.
- Localizing preset data into the other 11 supported languages (fall back to English).
