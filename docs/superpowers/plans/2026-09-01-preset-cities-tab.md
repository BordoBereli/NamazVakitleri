# Preset Cities Tab — Turkey Pin, Localization, Landscape Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pin Turkey at the top of the Preset Cities country list, localize all preset country/province/city names into Turkish, English, Arabic, and Farsi, and make the Preset tab landscape-friendly with a two-pane master/detail layout.

**Architecture:** Extend the `City` model with nullable localized fields (`nameTr/Ar/Fa`, `countryTr/Ar/Fa`, `cityTr/Ar/Fa`) populated in `cities.json`. A pure `CityLocalizer` object resolves localized display names from the active language (`LanguageProvider`). The `LocationSelectionViewModel` injects `LanguageProvider`, builds/sorts countries with Turkey pinned first, and carries the master country list through `CitySelection`/`ProvinceSelection` states so the UI can render a two-pane layout in landscape.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3), Koin (KSP), kotlinx.serialization, JUnit 5 + MockK + Turbine + Truth, Robolectric.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/City.kt` | Add nullable localized fields |
| `prayer_settings/src/main/assets/cities.json` | Add localized fields for all 328 cities |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizer.kt` (NEW) | Pure localization helper |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionContract.kt` | Add `key` to `CountryInfo`; carry `countries` in `CitySelection`/`ProvinceSelection` |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt` | Inject `LanguageProvider`; localized build/sort; Turkey pin; carry countries |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionScreen.kt` | Localized display; Turkey highlight; landscape two-pane |
| `scripts/localize_countries.py` (NEW) | Add `countryTr/Ar/Fa` to cities.json |
| `scripts/localize_turkey_provinces.py` (NEW) | Add `cityTr/Ar/Fa` for Turkey provinces |
| `scripts/localize_cities.py` (NEW) | Add `nameTr/Ar/Fa` for non-Turkey cities |
| `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizerTest.kt` (NEW) | CityLocalizer unit tests |
| `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt` | Update + new localization/Turkey-pin tests |
| `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/repository/PresetCitiesLocalizationTest.kt` (NEW) | Verify cities.json localized fields |

---

## Task 1: City model — add localized fields

**Files:**
- Modify: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/City.kt`

- [ ] **Step 1: Add localized fields to the `City` data class**

Replace the `City` data class body with:

```kotlin
@Serializable
data class City(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val city: String? = null,
    val county: String? = null,
    val nameTr: String? = null,
    val nameAr: String? = null,
    val nameFa: String? = null,
    val countryTr: String? = null,
    val countryAr: String? = null,
    val countryFa: String? = null,
    val cityTr: String? = null,
    val cityAr: String? = null,
    val cityFa: String? = null
) {
    val province: String get() = city ?: name

    fun displayName(): String = when {
        city != null && county != null -> "$name, $county, $city, $country"
        city != null -> "$name, $city, $country"
        county != null -> "$name, $county, $country"
        else -> "$name, $country"
    }
}
```

- [ ] **Step 2: Verify existing tests still compile**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="*LocationRepository*"`

Expected: PASS (all new fields have defaults, so existing constructors are unaffected).

- [ ] **Step 3: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/location/City.kt
git commit -m "feat(model): add localized name fields to City"
```

---

## Task 2: CityLocalizer helper

**Files:**
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizer.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `CityLocalizerTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.City
import org.junit.jupiter.api.Test

class CityLocalizerTest {

    private val city = City(
        name = "Istanbul",
        country = "Turkey",
        latitude = 41.0082,
        longitude = 28.9784,
        timezone = "Europe/Istanbul",
        city = "Istanbul",
        nameTr = "İstanbul",
        nameAr = "إسطنبول",
        nameFa = "استانبول",
        countryTr = "Türkiye",
        countryAr = "تركيا",
        countryFa = "ترکیه",
        cityTr = "İstanbul",
        cityAr = "إسطنبول",
        cityFa = "استانبول"
    )

    @Test
    fun `localizedName returns Turkish name for tr`() {
        assertThat(CityLocalizer.localizedName(city, "tr")).isEqualTo("İstanbul")
    }

    @Test
    fun `localizedName returns Arabic name for ar`() {
        assertThat(CityLocalizer.localizedName(city, "ar")).isEqualTo("إسطنبول")
    }

    @Test
    fun `localizedName returns Farsi name for fa`() {
        assertThat(CityLocalizer.localizedName(city, "fa")).isEqualTo("استانبول")
    }

    @Test
    fun `localizedName falls back to English for unsupported language`() {
        assertThat(CityLocalizer.localizedName(city, "de")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedName falls back to English when localized field missing`() {
        val plain = city.copy(nameTr = null, nameAr = null, nameFa = null)
        assertThat(CityLocalizer.localizedName(plain, "tr")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedCountry returns localized country name`() {
        assertThat(CityLocalizer.localizedCountry(city, "tr")).isEqualTo("Türkiye")
        assertThat(CityLocalizer.localizedCountry(city, "ar")).isEqualTo("تركيا")
        assertThat(CityLocalizer.localizedCountry(city, "fa")).isEqualTo("ترکیه")
        assertThat(CityLocalizer.localizedCountry(city, "en")).isEqualTo("Turkey")
    }

    @Test
    fun `localizedProvince returns localized province name`() {
        assertThat(CityLocalizer.localizedProvince(city, "tr")).isEqualTo("İstanbul")
        assertThat(CityLocalizer.localizedProvince(city, "en")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedProvince falls back to name when city field missing`() {
        val noCity = city.copy(city = null)
        assertThat(CityLocalizer.localizedProvince(noCity, "en")).isEqualTo("Istanbul")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CityLocalizerTest"`

Expected: FAIL — `CityLocalizer` is not defined.

- [ ] **Step 3: Write the implementation**

Create `CityLocalizer.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.location

import com.kutluoglu.prayer.model.location.City

object CityLocalizer {

    fun localizedName(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.nameTr ?: city.name
        "ar" -> city.nameAr ?: city.name
        "fa" -> city.nameFa ?: city.name
        else -> city.name
    }

    fun localizedCountry(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.countryTr ?: city.country
        "ar" -> city.countryAr ?: city.country
        "fa" -> city.countryFa ?: city.country
        else -> city.country
    }

    fun localizedProvince(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.cityTr ?: (city.city ?: city.name)
        "ar" -> city.cityAr ?: (city.city ?: city.name)
        "fa" -> city.cityFa ?: (city.city ?: city.name)
        else -> city.city ?: city.name
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CityLocalizerTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizer.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/CityLocalizerTest.kt
git commit -m "feat(settings): add CityLocalizer for preset city localization"
```

---

## Task 3: Contract — CountryInfo key + carry countries in states

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionContract.kt`

- [ ] **Step 1: Add `key` to `CountryInfo` and `countries` to detail states**

Replace the `CountryInfo` data class:

```kotlin
data class CountryInfo(
    val name: String,
    val key: String,
    val cityCount: Int,
    val isPriority: Boolean = false
)
```

Replace the `CitySelection` data class:

```kotlin
    data class CitySelection(
        val country: String,
        val cities: List<City>,
        val citiesByProvince: Map<String, List<City>>,
        val selectedProvince: String? = null,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
        val countries: List<CountryInfo> = emptyList()
    ) : LocationSelectionUiState()
```

Replace the `ProvinceSelection` data class:

```kotlin
    data class ProvinceSelection(
        val country: String,
        val province: String,
        val mainCity: City,
        val districts: List<City>,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
        val countries: List<CountryInfo> = emptyList()
    ) : LocationSelectionUiState()
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`

Expected: COMPILES. (The ViewModel's `CountryInfo(...)` call sites still compile because `key` is a required positional param — they will be updated in Task 4. If compilation fails because of the missing `key` argument, that is expected; proceed to Task 4 which fixes it.)

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionContract.kt
git commit -m "feat(settings): add country key and carry master list in location states"
```

---

## Task 4: ViewModel — LanguageProvider, localized build/sort, Turkey pin, carry countries

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Update the test setup. Replace the `presetCities` field and `setUp()` in `LocationSelectionViewModelTest.kt`:

```kotlin
    private val languageProvider = mockk<LanguageProvider>()

    private val presetCities = listOf(
        City(
            "Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul", "Istanbul",
            nameTr = "İstanbul", nameAr = "إسطنبول", nameFa = "استانبول",
            countryTr = "Türkiye", countryAr = "تركيا", countryFa = "ترکیه",
            cityTr = "İstanbul", cityAr = "إسطنبول", cityFa = "استانبول"
        ),
        City(
            "Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul", "Ankara",
            nameTr = "Ankara", nameAr = "أنقرة", nameFa = "آنکارا",
            countryTr = "Türkiye", countryAr = "تركيا", countryFa = "ترکیه",
            cityTr = "Ankara", cityAr = "أنقرة", cityFa = "آنکارا"
        ),
        City(
            "Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh", "Riyadh",
            nameTr = "Riyad", nameAr = "الرياض", nameFa = "ریاض",
            countryTr = "Suudi Arabistan", countryAr = "السعودية", countryFa = "عربستان سعودی",
            cityTr = "Riyad", cityAr = "الرياض", cityFa = "ریاض"
        ),
        City(
            "London", "United Kingdom", 51.5074, -0.1278, "Europe/London", "London",
            nameTr = "Londra", nameAr = "لندن", nameFa = "لندن",
            countryTr = "Birleşik Krallık", countryAr = "المملكة المتحدة", countryFa = "بریتانیا",
            cityTr = "Londra", cityAr = "لندن", cityFa = "لندن"
        )
    )

    @BeforeEach
    fun setUp() {
        locationRepository = mockk()
        searchLocationUseCase = mockk()
        locationServiceHelper = mockk()
        locationsCoordinator = mockk(relaxed = true)
        coEvery { locationRepository.getPresetCities() } returns presetCities
        every { languageProvider.getLanguageCode() } returns "en"
        viewModel = LocationSelectionViewModel(
            locationRepository,
            searchLocationUseCase,
            locationServiceHelper,
            locationsCoordinator,
            analyticsTracker,
            languageProvider,
            defaultDispatcher = mainCoroutineRule.dispatcher
        )
    }
```

Add these imports to the top of the test file (alphabetical, after the existing `io.mockk` imports):

```kotlin
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import io.mockk.every
```

Add these new tests at the end of the class (before the closing brace):

```kotlin
    @Test
    fun `Country list should pin Turkey first among priority countries`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            val countryState = state as LocationSelectionUiState.CountrySelection
            assertThat(countryState.countries.map { it.key })
                .containsExactly("Turkey", "Saudi Arabia", "United Kingdom")
            assertThat(countryState.countries.first().key).isEqualTo("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Country list should show localized country names for Turkish`() = runTest {
        every { languageProvider.getLanguageCode() } returns "tr"

        viewModel.uiState.test {
            val state = awaitItem()
            val countryState = state as LocationSelectionUiState.CountrySelection
            assertThat(countryState.countries.first().name).isEqualTo("Türkiye")
            assertThat(countryState.countries.first().key).isEqualTo("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Country list should show localized country names for Arabic`() = runTest {
        every { languageProvider.getLanguageCode() } returns "ar"

        viewModel.uiState.test {
            val state = awaitItem()
            val countryState = state as LocationSelectionUiState.CountrySelection
            assertThat(countryState.countries.first().name).isEqualTo("تركيا")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Country list should fall back to English for unsupported language`() = runTest {
        every { languageProvider.getLanguageCode() } returns "de"

        viewModel.uiState.test {
            val state = awaitItem()
            val countryState = state as LocationSelectionUiState.CountrySelection
            assertThat(countryState.countries.first().name).isEqualTo("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SearchCountry should match localized country names`() = runTest {
        every { languageProvider.getLanguageCode() } returns "tr"
        viewModel.onEvent(LocationSelectionEvent.SearchCountry("Türkiye"))

        viewModel.uiState.test {
            var filtered: LocationSelectionUiState.CountrySelection? = null
            while (filtered == null) {
                val state = awaitItem()
                if (state is LocationSelectionUiState.CountrySelection && state.searchQuery == "Türkiye") {
                    filtered = state
                }
            }
            assertThat(filtered.countries.map { it.key }).containsExactly("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectCountry should carry the master country list for two-pane`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))

        viewModel.uiState.test {
            val state = awaitItem()
            val cityState = state as LocationSelectionUiState.CitySelection
            assertThat(cityState.countries.map { it.key })
                .containsExactly("Turkey", "Saudi Arabia", "United Kingdom")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectProvince should carry the master country list for two-pane`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))
        viewModel.uiState.test { awaitItem(); cancelAndIgnoreRemainingEvents() }

        val mainCity = presetCities.first()
        viewModel.onEvent(LocationSelectionEvent.SelectProvince("Istanbul", mainCity))

        viewModel.uiState.test {
            val state = awaitItem()
            val provinceState = state as LocationSelectionUiState.ProvinceSelection
            assertThat(provinceState.countries.map { it.key })
                .containsExactly("Turkey", "Saudi Arabia", "United Kingdom")
            cancelAndIgnoreRemainingEvents()
        }
    }
```

- [ ] **Step 2: Update the existing `SearchCountry with blank query` test**

The new mock data adds `Saudi Arabia`, so update the existing test `SearchCountry with blank query should restore full country list` in `LocationSelectionViewModelTest.kt`. Change its assertion:

```kotlin
            assertThat(restored.countries.map { it.name })
                .containsExactly("Turkey", "Saudi Arabia", "United Kingdom")
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*LocationSelectionViewModelTest"`

Expected: FAIL — constructor signature mismatch (`LanguageProvider` param missing) and `CountryInfo` missing `key`.

- [ ] **Step 4: Implement the ViewModel changes**

Add the import to `LocationSelectionViewModel.kt`:

```kotlin
import com.kutluoglu.core.designsystem.utils.LanguageProvider
```

Update the constructor:

```kotlin
class LocationSelectionViewModel(
    private val locationRepository: LocationRepository,
    private val searchLocationUseCase: SearchLocationUseCase,
    private val locationServiceHelper: LocationServiceHelper,
    private val locationsCoordinator: LocationsCoordinator,
    private val analyticsTracker: AnalyticsTracker,
    private val languageProvider: LanguageProvider,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
```

Replace `loadCountries()`:

```kotlin
    private fun loadCountries() {
        viewModelScope.launch {
            try {
                _uiState.value = LocationSelectionUiState.Loading
                allCities = locationRepository.getPresetCities()

                _uiState.value = LocationSelectionUiState.CountrySelection(buildCountries(allCities))
            } catch (e: Exception) {
                _uiState.value = LocationSelectionUiState.Error(getUserFriendlyErrorMessage(e))
            }
        }
    }

    private fun buildCountries(cities: List<City>): List<CountryInfo> {
        val lang = languageProvider.getLanguageCode()
        return cities
            .groupBy { it.country }
            .map { (country, cities) ->
                val sample = cities.first()
                CountryInfo(
                    name = CityLocalizer.localizedCountry(sample, lang),
                    key = country,
                    cityCount = cities.size,
                    isPriority = PRIORITY_COUNTRIES.contains(country)
                )
            }
            .sortedWith(
                compareBy(
                    { !it.isPriority },
                    { it.key != "Turkey" },
                    { it.name.lowercase() }
                )
            )
    }
```

Replace `searchCountries()`:

```kotlin
    private fun searchCountries(query: String) {
        searchCountriesJob?.cancel()
        searchCountriesJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            val currentState = _uiState.value
            if (currentState !is LocationSelectionUiState.CountrySelection) return@launch

            val filtered = withContext(defaultDispatcher) {
                if (query.isBlank()) {
                    buildCountries(allCities)
                } else {
                    val lang = languageProvider.getLanguageCode()
                    allCities
                        .filter { city ->
                            matchesTurkish(CityLocalizer.localizedName(city, lang), query) ||
                                matchesTurkish(CityLocalizer.localizedCountry(city, lang), query)
                        }
                        .let { buildCountries(it) }
                }
            }

            _uiState.value = currentState.copy(countries = filtered, searchQuery = query)
        }
    }
```

Replace `selectCountry()`:

```kotlin
    private fun selectCountry(country: String) {
        selectedCountry = country
        val citiesInCountry = allCities.filter { it.country == country }

        val citiesByProvince = citiesInCountry
            .groupBy { it.city ?: it.name }
            .toSortedMap()

        _uiState.value = LocationSelectionUiState.CitySelection(
            country = country,
            cities = citiesInCountry,
            citiesByProvince = citiesByProvince,
            selectedProvince = null,
            sortOrder = currentSortOrder,
            countries = buildCountries(allCities)
        )
    }
```

Replace `selectProvince()`:

```kotlin
    private fun selectProvince(province: String, mainCity: City) {
        selectedProvince = province
        val citiesInCountry = allCities.filter { it.country == selectedCountry }

        val districts = if (province == mainCity.name) {
            citiesInCountry.filter { it.city == province && it.name != province }
        } else {
            citiesInCountry.filter { it.city == province }
        }

        _uiState.value = LocationSelectionUiState.ProvinceSelection(
            country = selectedCountry ?: "",
            province = province,
            mainCity = mainCity,
            districts = districts.sortedBy { it.name.lowercase() },
            sortOrder = currentSortOrder,
            countries = buildCountries(allCities)
        )
    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*LocationSelectionViewModelTest"`

Expected: PASS (all existing + new tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt
git commit -m "feat(settings): localize preset country list and pin Turkey first"
```

---

## Task 5: Data — localize country names in cities.json

**Files:**
- Create: `scripts/localize_countries.py`

- [ ] **Step 1: Create the localization script**

Create `scripts/localize_countries.py` with the full country translation dictionary:

```python
#!/usr/bin/env python3
"""Add countryTr/countryAr/countryFa fields to every city in cities.json."""
import json
import os

CITIES_JSON = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "prayer_settings", "src", "main", "assets", "cities.json")
)

COUNTRIES = {
    "Turkey": {"tr": "Türkiye", "ar": "تركيا", "fa": "ترکیه"},
    "Saudi Arabia": {"tr": "Suudi Arabistan", "ar": "السعودية", "fa": "عربستان سعودی"},
    "Egypt": {"tr": "Mısır", "ar": "مصر", "fa": "مصر"},
    "Indonesia": {"tr": "Endonezya", "ar": "إندونيسيا", "fa": "اندونزی"},
    "Pakistan": {"tr": "Pakistan", "ar": "باكستان", "fa": "پاکستان"},
    "Iran": {"tr": "İran", "ar": "إيران", "fa": "ایران"},
    "China": {"tr": "Çin", "ar": "الصين", "fa": "چین"},
    "Algeria": {"tr": "Cezayir", "ar": "الجزائر", "fa": "الجزایر"},
    "Malaysia": {"tr": "Malezya", "ar": "ماليزيا", "fa": "مالزی"},
    "India": {"tr": "Hindistan", "ar": "الهند", "fa": "هند"},
    "Morocco": {"tr": "Fas", "ar": "المغرب", "fa": "مراکش"},
    "Iraq": {"tr": "Irak", "ar": "العراق", "fa": "عراق"},
    "Russia": {"tr": "Rusya", "ar": "روسيا", "fa": "روسیه"},
    "Syria": {"tr": "Suriye", "ar": "سوريا", "fa": "سوریه"},
    "USA": {"tr": "ABD", "ar": "الولايات المتحدة", "fa": "ایالات متحده آمریکا"},
    "Tunisia": {"tr": "Tunus", "ar": "تونس", "fa": "تونس"},
    "Jordan": {"tr": "Ürdün", "ar": "الأردن", "fa": "اردن"},
    "UAE": {"tr": "BAE", "ar": "الإمارات", "fa": "امارات متحده عربی"},
    "Australia": {"tr": "Avustralya", "ar": "أستراليا", "fa": "استرالیا"},
    "Germany": {"tr": "Almanya", "ar": "ألمانيا", "fa": "آلمان"},
    "Bahrain": {"tr": "Bahreyn", "ar": "البحرين", "fa": "بحرین"},
    "Oman": {"tr": "Umman", "ar": "عمان", "fa": "عمان"},
    "Canada": {"tr": "Kanada", "ar": "كندا", "fa": "کانادا"},
    "France": {"tr": "Fransa", "ar": "فرنسا", "fa": "فرانسه"},
    "Japan": {"tr": "Japonya", "ar": "اليابان", "fa": "ژاپن"},
    "Lebanon": {"tr": "Lübnan", "ar": "لبنان", "fa": "لبنان"},
    "South Africa": {"tr": "Güney Afrika", "ar": "جنوب أفريقيا", "fa": "آفریقای جنوبی"},
    "United Kingdom": {"tr": "Birleşik Krallık", "ar": "المملكة المتحدة", "fa": "بریتانیا"},
    "Bangladesh": {"tr": "Bangladeş", "ar": "بنغلاديش", "fa": "بنگلادش"},
    "Nigeria": {"tr": "Nijerya", "ar": "نيجيريا", "fa": "نیجریه"},
    "Kuwait": {"tr": "Kuveyt", "ar": "الكويت", "fa": "کویت"},
    "Qatar": {"tr": "Katar", "ar": "قطر", "fa": "قطر"},
    "Greece": {"tr": "Yunanistan", "ar": "اليونان", "fa": "یونان"},
    "Italy": {"tr": "İtalya", "ar": "إيطاليا", "fa": "ایتالیا"},
    "Libya": {"tr": "Libya", "ar": "ليبيا", "fa": "لیبی"},
    "Netherlands": {"tr": "Hollanda", "ar": "هولندا", "fa": "هلند"},
    "Poland": {"tr": "Polonya", "ar": "بولندا", "fa": "لهستان"},
    "Portugal": {"tr": "Portekiz", "ar": "البرتغال", "fa": "پرتغال"},
    "South Korea": {"tr": "Güney Kore", "ar": "كوريا الجنوبية", "fa": "کره جنوبی"},
    "Spain": {"tr": "İspanya", "ar": "إسبانيا", "fa": "اسپانیا"},
    "Switzerland": {"tr": "İsviçre", "ar": "سويسرا", "fa": "سوئیس"},
    "Vietnam": {"tr": "Vietnam", "ar": "فيتنام", "fa": "ویتنام"},
    "Austria": {"tr": "Avusturya", "ar": "النمسا", "fa": "اتریش"},
    "Belgium": {"tr": "Belçika", "ar": "بلجيكا", "fa": "بلژیک"},
    "Cambodia": {"tr": "Kamboçya", "ar": "كمبوديا", "fa": "کامبوج"},
    "Czech Republic": {"tr": "Çek Cumhuriyeti", "ar": "التشيك", "fa": "جمهوری چک"},
    "Denmark": {"tr": "Danimarka", "ar": "الدنمارك", "fa": "دانمارک"},
    "Ethiopia": {"tr": "Etiyopya", "ar": "إثيوبيا", "fa": "اتیوپی"},
    "Finland": {"tr": "Finlandiya", "ar": "فنلندا", "fa": "فنلاند"},
    "Ghana": {"tr": "Gana", "ar": "غانا", "fa": "غنا"},
    "Hong Kong": {"tr": "Hong Kong", "ar": "هونغ كونغ", "fa": "هنگ کنگ"},
    "Hungary": {"tr": "Macaristan", "ar": "المجر", "fa": "مجارستان"},
    "Ireland": {"tr": "İrlanda", "ar": "أيرلندا", "fa": "ایرلند"},
    "Kenya": {"tr": "Kenya", "ar": "كينيا", "fa": "کنیا"},
    "Madagascar": {"tr": "Madagaskar", "ar": "مدغشقر", "fa": "ماداگاسکار"},
    "Nepal": {"tr": "Nepal", "ar": "نيبال", "fa": "نپال"},
    "Norway": {"tr": "Norveç", "ar": "النرويج", "fa": "نروژ"},
    "Philippines": {"tr": "Filipinler", "ar": "الفلبين", "fa": "فیلیپین"},
    "Romania": {"tr": "Romanya", "ar": "رومانيا", "fa": "رومانی"},
    "Singapore": {"tr": "Singapur", "ar": "سنغافورة", "fa": "سنگاپور"},
    "Somalia": {"tr": "Somali", "ar": "الصومال", "fa": "سومالی"},
    "Sri Lanka": {"tr": "Sri Lanka", "ar": "سريلانكا", "fa": "سریلانکا"},
    "Sudan": {"tr": "Sudan", "ar": "السودان", "fa": "سودان"},
    "Sweden": {"tr": "İsveç", "ar": "السويد", "fa": "سوئد"},
    "Taiwan": {"tr": "Tayvan", "ar": "تايوان", "fa": "تایوان"},
    "Tanzania": {"tr": "Tanzanya", "ar": "تنزانيا", "fa": "تانزانیا"},
    "Thailand": {"tr": "Tayland", "ar": "تايلاند", "fa": "تایلند"},
    "Uganda": {"tr": "Uganda", "ar": "أوغندا", "fa": "اوگاندا"},
}

def main():
    with open(CITIES_JSON, encoding="utf-8") as f:
        data = json.load(f)
    updated = 0
    for city in data["cities"]:
        country = city["country"]
        if country in COUNTRIES:
            for lang, value in COUNTRIES[country].items():
                city[f"country{lang.capitalize()}"] = value
            updated += 1
    with open(CITIES_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Localized country names for {updated} cities")

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run the script**

Run: `python3 scripts/localize_countries.py`

Expected: `Localized country names for 328 cities`

- [ ] **Step 3: Verify the output**

Run: `python3 -c "import json; d=json.load(open('prayer_settings/src/main/assets/cities.json')); c=[x for x in d['cities'] if x['country']=='Turkey'][0]; print(c['countryTr'], c['countryAr'], c['countryFa'])`

Expected: `Türkiye تركيا ترکیه`

- [ ] **Step 4: Commit**

```bash
git add scripts/localize_countries.py prayer_settings/src/main/assets/cities.json
git commit -m "data(settings): localize country names in preset cities"
```

---

## Task 6: Data — localize Turkey province names in cities.json

**Files:**
- Create: `scripts/localize_turkey_provinces.py`

- [ ] **Step 1: Create the localization script**

Create `scripts/localize_turkey_provinces.py` with the full Turkey province translation dictionary (keys are the English `city` field values):

```python
#!/usr/bin/env python3
"""Add cityTr/cityAr/cityFa fields for Turkey provinces in cities.json."""
import json
import os

CITIES_JSON = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "prayer_settings", "src", "main", "assets", "cities.json")
)

PROVINCES = {
    "Adana": {"tr": "Adana", "ar": "أضنة", "fa": "آدانا"},
    "Afyon": {"tr": "Afyonkarahisar", "ar": "أفيون قره حصار", "fa": "افیون قرهحصار"},
    "Aksaray": {"tr": "Aksaray", "ar": "أقصراي", "fa": "آقسرای"},
    "Amasya": {"tr": "Amasya", "ar": "أماسيا", "fa": "آماسیه"},
    "Ankara": {"tr": "Ankara", "ar": "أنقرة", "fa": "آنکارا"},
    "Antalya": {"tr": "Antalya", "ar": "أنطاليا", "fa": "آنتالیا"},
    "Ardahan": {"tr": "Ardahan", "ar": "أردهان", "fa": "اردهان"},
    "Artvin": {"tr": "Artvin", "ar": "أرتوين", "fa": "آرتوین"},
    "Aydin": {"tr": "Aydın", "ar": "أيدين", "fa": "آیدین"},
    "Balikesir": {"tr": "Balıkesir", "ar": "باليكسير", "fa": "بالیکسیر"},
    "Bartin": {"tr": "Bartın", "ar": "بارتين", "fa": "بارتین"},
    "Batman": {"tr": "Batman", "ar": "بطمان", "fa": "باطمان"},
    "Bayburt": {"tr": "Bayburt", "ar": "بايبورت", "fa": "بایبورت"},
    "Bilecik": {"tr": "Bilecik", "ar": "بيله جك", "fa": "بیلهجیک"},
    "Bingol": {"tr": "Bingöl", "ar": "بينغول", "fa": "بینگول"},
    "Bitlis": {"tr": "Bitlis", "ar": "بدليس", "fa": "بتلیس"},
    "Bolu": {"tr": "Bolu", "ar": "بولو", "fa": "بولو"},
    "Burdur": {"tr": "Burdur", "ar": "بوردور", "fa": "بوردور"},
    "Bursa": {"tr": "Bursa", "ar": "بورصة", "fa": "بورسا"},
    "Canakkale": {"tr": "Çanakkale", "ar": "جناق قلعة", "fa": "چاناققلعه"},
    "Cankiri": {"tr": "Çankırı", "ar": "جانقري", "fa": "چانقری"},
    "Denizli": {"tr": "Denizli", "ar": "دنيزلي", "fa": "دنیزلی"},
    "Diyarbakir": {"tr": "Diyarbakır", "ar": "ديار بكر", "fa": "دیاربکر"},
    "Duzce": {"tr": "Düzce", "ar": "دوزجه", "fa": "دوزجه"},
    "Edirne": {"tr": "Edirne", "ar": "أدرنة", "fa": "ادرنه"},
    "Elazig": {"tr": "Elazığ", "ar": "إلازيغ", "fa": "الازیغ"},
    "Erzurum": {"tr": "Erzurum", "ar": "أرضروم", "fa": "ارزروم"},
    "Eskisehir": {"tr": "Eskişehir", "ar": "إسكي شهر", "fa": "اسکیشهر"},
    "Gaziantep": {"tr": "Gaziantep", "ar": "غازي عنتاب", "fa": "غازیعینتاب"},
    "Giresun": {"tr": "Giresun", "ar": "غيرسون", "fa": "گیرسون"},
    "Gumushane": {"tr": "Gümüşhane", "ar": "كوموش خانة", "fa": "گوموشخانه"},
    "Hakkari": {"tr": "Hakkâri", "ar": "هكاري", "fa": "حکاری"},
    "Hatay": {"tr": "Hatay", "ar": "هاتاي", "fa": "هاتای"},
    "Igdir": {"tr": "Iğdır", "ar": "إغدير", "fa": "ایغدیر"},
    "Isparta": {"tr": "Isparta", "ar": "إسبرطة", "fa": "اسپارتا"},
    "Istanbul": {"tr": "İstanbul", "ar": "إسطنبول", "fa": "استانبول"},
    "Izmir": {"tr": "İzmir", "ar": "إزمير", "fa": "ازمیر"},
    "Kahramanmaras": {"tr": "Kahramanmaraş", "ar": "قهرمان مرعش", "fa": "قهرمانمرعش"},
    "Karabuk": {"tr": "Karabük", "ar": "كارابوك", "fa": "کارابوک"},
    "Karaman": {"tr": "Karaman", "ar": "قرة مان", "fa": "کارامان"},
    "Kastamonu": {"tr": "Kastamonu", "ar": "قسطموني", "fa": "کاستامونو"},
    "Kayseri": {"tr": "Kayseri", "ar": "قيصرية", "fa": "قیصریه"},
    "Kilis": {"tr": "Kilis", "ar": "كلس", "fa": "کیلیس"},
    "Kirikkale": {"tr": "Kırıkkale", "ar": "قيريق قلعة", "fa": "قیریققلعه"},
    "Kirklareli": {"tr": "Kırklareli", "ar": "قرقلر ايلي", "fa": "قرقلرایلی"},
    "Kirsehir": {"tr": "Kırşehir", "ar": "قير شهير", "fa": "قرشهر"},
    "Kocaeli": {"tr": "Kocaeli", "ar": "قوجه ايلي", "fa": "قوجاایلی"},
    "Konya": {"tr": "Konya", "ar": "قونية", "fa": "قونیه"},
    "Kutahya": {"tr": "Kütahya", "ar": "كوتاهية", "fa": "کوتاهیه"},
    "Malatya": {"tr": "Malatya", "ar": "ملطية", "fa": "ملطیه"},
    "Manisa": {"tr": "Manisa", "ar": "مانيسا", "fa": "مانیسا"},
    "Mardin": {"tr": "Mardin", "ar": "ماردين", "fa": "ماردین"},
    "Mersin": {"tr": "Mersin", "ar": "مرسين", "fa": "مرسین"},
    "Mus": {"tr": "Muş", "ar": "موش", "fa": "موش"},
    "Nevsehir": {"tr": "Nevşehir", "ar": "نوشهر", "fa": "نوشهر"},
    "Nigde": {"tr": "Niğde", "ar": "نيدا", "fa": "نیغده"},
    "Ordu": {"tr": "Ordu", "ar": "أوردو", "fa": "اردو"},
    "Rize": {"tr": "Rize", "ar": "ريزه", "fa": "ریزه"},
    "Sakarya": {"tr": "Sakarya", "ar": "سكاريا", "fa": "ساکاریا"},
    "Samsun": {"tr": "Samsun", "ar": "سامسون", "fa": "سامسون"},
    "Sanliurfa": {"tr": "Şanlıurfa", "ar": "شانلي أورفة", "fa": "شانلیاورفا"},
    "Siirt": {"tr": "Siirt", "ar": "سعرد", "fa": "سعرد"},
    "Sinop": {"tr": "Sinop", "ar": "سينوب", "fa": "سینوپ"},
    "Sirnak": {"tr": "Şırnak", "ar": "شرناق", "fa": "شرناق"},
    "Sivas": {"tr": "Sivas", "ar": "سيواس", "fa": "سیواس"},
    "Tekirdag": {"tr": "Tekirdağ", "ar": "تكيرداغ", "fa": "تکیرداغ"},
    "Tokat": {"tr": "Tokat", "ar": "توقات", "fa": "توقات"},
    "Trabzon": {"tr": "Trabzon", "ar": "طرابزون", "fa": "ترابزون"},
    "Tunceli": {"tr": "Tunceli", "ar": "تونجلي", "fa": "تونجلی"},
    "Usak": {"tr": "Uşak", "ar": "أوشاك", "fa": "اوشاک"},
    "Van": {"tr": "Van", "ar": "وان", "fa": "وان"},
    "Yalova": {"tr": "Yalova", "ar": "يالوفا", "fa": "یالووا"},
    "Yozgat": {"tr": "Yozgat", "ar": "يوزغات", "fa": "یوزگات"},
    "Zonguldak": {"tr": "Zonguldak", "ar": "زونغولداك", "fa": "زونگولداک"},
}

def main():
    with open(CITIES_JSON, encoding="utf-8") as f:
        data = json.load(f)
    updated = 0
    for city in data["cities"]:
        prov = city.get("city")
        if prov and prov in PROVINCES:
            for lang, value in PROVINCES[prov].items():
                city[f"city{lang.capitalize()}"] = value
            updated += 1
    with open(CITIES_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Localized province names for {updated} cities")

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run the script**

Run: `python3 scripts/localize_turkey_provinces.py`

Expected: `Localized province names for 113 cities`

- [ ] **Step 3: Verify the output**

Run: `python3 -c "import json; d=json.load(open('prayer_settings/src/main/assets/cities.json')); c=[x for x in d['cities'] if x['country']=='Turkey' and x.get('city')=='Istanbul'][0]; print(c['cityTr'], c['cityAr'], c['cityFa'])`

Expected: `İstanbul إسطنبول استانبول`

- [ ] **Step 4: Commit**

```bash
git add scripts/localize_turkey_provinces.py prayer_settings/src/main/assets/cities.json
git commit -m "data(settings): localize Turkey province names in preset cities"
```

---

## Task 7: Data — localize non-Turkey city names in cities.json

**Files:**
- Create: `scripts/localize_cities.py`

- [ ] **Step 1: Create the localization script**

Create `scripts/localize_cities.py` with the full non-Turkey city translation dictionary (keys are the English `name` field values). This also covers Turkey province-center entries whose `name` equals the province (e.g. `name="Istanbul"`):

```python
#!/usr/bin/env python3
"""Add nameTr/nameAr/nameFa fields for non-Turkey cities in cities.json."""
import json
import os

CITIES_JSON = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "prayer_settings", "src", "main", "assets", "cities.json")
)

CITIES = {
    "Abha": {"tr": "Abha", "ar": "أبها", "fa": "ابها"},
    "Abu Dhabi": {"tr": "Abu Dabi", "ar": "أبو ظبي", "fa": "ابوظبی"},
    "Accra": {"tr": "Akra", "ar": "أكرا", "fa": "آکرا"},
    "Addis Ababa": {"tr": "Addis Ababa", "ar": "أديس أبابا", "fa": "آدیس آبابا"},
    "Agadir": {"tr": "Agadir", "ar": "أكادير", "fa": "اگادیر"},
    "Ahvaz": {"tr": "Ahvaz", "ar": "الأهواز", "fa": "اهواز"},
    "Al Ahmadi": {"tr": "El Ahmadi", "ar": "الأحمدي", "fa": "الاحمدی"},
    "Al Ain": {"tr": "El Ayn", "ar": "العين", "fa": "العین"},
    "Al Qunfudhah": {"tr": "El Kunfuze", "ar": "القنفذة", "fa": "قنفذه"},
    "Al Rayyan": {"tr": "Er Reyyan", "ar": "الريان", "fa": "الریان"},
    "Al-Baha": {"tr": "El Baha", "ar": "الباحة", "fa": "الباحه"},
    "Al-Jubail": {"tr": "El Cübeyl", "ar": "الجبيل", "fa": "جبیل"},
    "Al-Khobar": {"tr": "El Hubar", "ar": "الخبر", "fa": "خبر"},
    "Al-Madinah": {"tr": "Medine", "ar": "المدينة المنورة", "fa": "مدینه"},
    "Aleppo": {"tr": "Halep", "ar": "حلب", "fa": "حلب"},
    "Alexandria": {"tr": "İskenderiye", "ar": "الإسكندرية", "fa": "اسکندریه"},
    "Algiers": {"tr": "Cezayir", "ar": "الجزائر", "fa": "الجزیره"},
    "Amman": {"tr": "Amman", "ar": "عمّان", "fa": "امان"},
    "Amsterdam": {"tr": "Amsterdam", "ar": "أمستردام", "fa": "آمستردام"},
    "Annaba": {"tr": "Annabe", "ar": "عنابة", "fa": "عنابه"},
    "Antananarivo": {"tr": "Antananarivo", "ar": "أنتاناناريفو", "fa": "آنتاناناریوو"},
    "Aqaba": {"tr": "Akabe", "ar": "العقبة", "fa": "عقبه"},
    "Arar": {"tr": "Arar", "ar": "عرعر", "fa": "عرعر"},
    "Athens": {"tr": "Atina", "ar": "أثينا", "fa": "آتن"},
    "Baghdad": {"tr": "Bağdat", "ar": "بغداد", "fa": "بغداد"},
    "Bahawalpur": {"tr": "Bahavalpur", "ar": "بهاولبور", "fa": "بهاولپور"},
    "Bandung": {"tr": "Bandung", "ar": "باندونغ", "fa": "باندونگ"},
    "Bangalore": {"tr": "Bangalore", "ar": "بنغالور", "fa": "بنگلور"},
    "Bangkok": {"tr": "Bangkok", "ar": "بانكوك", "fa": "بانکوک"},
    "Barcelona": {"tr": "Barselona", "ar": "برشلونة", "fa": "بارسلونا"},
    "Basra": {"tr": "Basra", "ar": "البصرة", "fa": "بصره"},
    "Beijing": {"tr": "Pekin", "ar": "بكين", "fa": "پکن"},
    "Beirut": {"tr": "Beyrut", "ar": "بيروت", "fa": "بیروت"},
    "Benghazi": {"tr": "Bingazi", "ar": "بنغازي", "fa": "بنغازی"},
    "Berlin": {"tr": "Berlin", "ar": "برلين", "fa": "برلین"},
    "Birmingham": {"tr": "Birmingham", "ar": "برمنغهام", "fa": "بیرمنگام"},
    "Biskra": {"tr": "Biskra", "ar": "بسكرة", "fa": "بسکره"},
    "Brisbane": {"tr": "Brisbane", "ar": "بريزبن", "fa": "بریزبن"},
    "Brussels": {"tr": "Brüksel", "ar": "بروكسل", "fa": "بروکسل"},
    "Bucharest": {"tr": "Bükreş", "ar": "بوخارست", "fa": "بخارست"},
    "Budapest": {"tr": "Budapeşte", "ar": "بودابست", "fa": "بوداپست"},
    "Buraydah": {"tr": "Bureyde", "ar": "بريدة", "fa": "بریده"},
    "Busan": {"tr": "Busan", "ar": "بوسان", "fa": "بوسان"},
    "Cairo": {"tr": "Kahire", "ar": "القاهرة", "fa": "قاهره"},
    "Cape Town": {"tr": "Cape Town", "ar": "كيب تاون", "fa": "کیپتاون"},
    "Casablanca": {"tr": "Kazablanka", "ar": "الدار البيضاء", "fa": "کازابلانکا"},
    "Chengdu": {"tr": "Çengdu", "ar": "تشنغدو", "fa": "چنگدو"},
    "Chennai": {"tr": "Chennai", "ar": "تشيناي", "fa": "چنای"},
    "Chicago": {"tr": "Şikago", "ar": "شيكاغو", "fa": "شیکاگو"},
    "Chittagong": {"tr": "Çitagong", "ar": "شيتاغونغ", "fa": "چیتاگونگ"},
    "Colombo": {"tr": "Kolombo", "ar": "كولومبو", "fa": "کلمبو"},
    "Constantine": {"tr": "Konstantin", "ar": "قسنطينة", "fa": "قسنطینه"},
    "Copenhagen": {"tr": "Kopenhag", "ar": "كوبنهاغن", "fa": "کپنهاگ"},
    "Damascus": {"tr": "Şam", "ar": "دمشق", "fa": "دمشق"},
    "Dammam": {"tr": "Dammam", "ar": "الدمام", "fa": "دمام"},
    "Dar es Salaam": {"tr": "Darüsselam", "ar": "دار السلام", "fa": "دارالسلام"},
    "Delhi": {"tr": "Delhi", "ar": "دلهي", "fa": "دهلی"},
    "Depok": {"tr": "Depok", "ar": "ديبوك", "fa": "دپوک"},
    "Dhahran": {"tr": "Zahran", "ar": "الظهران", "fa": "ظهران"},
    "Dhaka": {"tr": "Dakka", "ar": "دكا", "fa": "داکا"},
    "Doha": {"tr": "Doha", "ar": "الدوحة", "fa": "دوحه"},
    "Dubai": {"tr": "Dubai", "ar": "دبي", "fa": "دبی"},
    "Dublin": {"tr": "Dublin", "ar": "دبلن", "fa": "دوبلین"},
    "Durban": {"tr": "Durban", "ar": "ديربان", "fa": "دوربان"},
    "Erbil": {"tr": "Erbil", "ar": "أربيل", "fa": "اربیل"},
    "Faisalabad": {"tr": "Faysalabad", "ar": "فيصل آباد", "fa": "فیصلآباد"},
    "Faiyum": {"tr": "Feyyum", "ar": "الفيوم", "fa": "فیوم"},
    "Fes": {"tr": "Fes", "ar": "فاس", "fa": "فاس"},
    "Frankfurt": {"tr": "Frankfurt", "ar": "فرانكفورت", "fa": "فرانکفورت"},
    "Geneva": {"tr": "Cenevre", "ar": "جنيف", "fa": "ژنو"},
    "Ghardaia": {"tr": "Gardaya", "ar": "غرداية", "fa": "غردایه"},
    "Giza": {"tr": "Gize", "ar": "الجيزة", "fa": "جیزه"},
    "Guangzhou": {"tr": "Guangzhou", "ar": "غوانزو", "fa": "گوانگژو"},
    "Gujranwala": {"tr": "Gucranvala", "ar": "غوجرانوالا", "fa": "گوجرانواله"},
    "Hafar Al-Batin": {"tr": "Hafar El Batin", "ar": "حفر الباطن", "fa": "حفر الباطن"},
    "Hail": {"tr": "Hail", "ar": "حائل", "fa": "حائل"},
    "Hama": {"tr": "Hama", "ar": "حماة", "fa": "حماه"},
    "Hamburg": {"tr": "Hamburg", "ar": "هامبورغ", "fa": "هامبورگ"},
    "Hanoi": {"tr": "Hanoi", "ar": "هانوي", "fa": "هانوی"},
    "Helsinki": {"tr": "Helsinki", "ar": "هلسنكي", "fa": "هلسینکی"},
    "Ho Chi Minh City": {"tr": "Ho Chi Minh Kenti", "ar": "مدينة هو تشي منه", "fa": "هوشیمین"},
    "Homs": {"tr": "Humus", "ar": "حمص", "fa": "حمص"},
    "Hong Kong": {"tr": "Hong Kong", "ar": "هونغ كونغ", "fa": "هنگ کنگ"},
    "Houston": {"tr": "Houston", "ar": "هيوستن", "fa": "هیوستون"},
    "Hyderabad": {"tr": "Haydarabad", "ar": "حيدر آباد", "fa": "حیدرآباد"},
    "Ipoh": {"tr": "Ipoh", "ar": "إيبوه", "fa": "ایپوه"},
    "Irbid": {"tr": "İrbid", "ar": "إربد", "fa": "اربد"},
    "Isfahan": {"tr": "İsfahan", "ar": "أصفهان", "fa": "اصفهان"},
    "Ismailia": {"tr": "İsmailiye", "ar": "الإسماعيلية", "fa": "اسماعیلیه"},
    "Jakarta": {"tr": "Cakarta", "ar": "جاكرتا", "fa": "جاکارتا"},
    "Jeddah": {"tr": "Cidde", "ar": "جدة", "fa": "جده"},
    "Jizan": {"tr": "Cizan", "ar": "جازان", "fa": "جازان"},
    "Johannesburg": {"tr": "Johannesburg", "ar": "جوهانسبرغ", "fa": "ژوهانسبورگ"},
    "Johor Bahru": {"tr": "Johor Bahru", "ar": "جوهر بهرو", "fa": "جوهور بهرو"},
    "Jouf": {"tr": "El Cevf", "ar": "الجوف", "fa": "جوف"},
    "Kairouan": {"tr": "Kayrevan", "ar": "القيروان", "fa": "قیروان"},
    "Kampala": {"tr": "Kampala", "ar": "كمبالا", "fa": "کامپالا"},
    "Kano": {"tr": "Kano", "ar": "كانو", "fa": "کانو"},
    "Karachi": {"tr": "Karaçi", "ar": "كراتشي", "fa": "کراچی"},
    "Karaj": {"tr": "Kerec", "ar": "كرج", "fa": "کرج"},
    "Karbala": {"tr": "Kerbela", "ar": "كربلاء", "fa": "کربلا"},
    "Kathmandu": {"tr": "Katmandu", "ar": "كاتماندو", "fa": "کاتماندو"},
    "Kazan": {"tr": "Kazan", "ar": "قازان", "fa": "قازان"},
    "Kermanshah": {"tr": "Kirmanşah", "ar": "كرمانشاه", "fa": "کرمانشاه"},
    "Khamis Mushait": {"tr": "Hamis Muşayt", "ar": "خميس مشيط", "fa": "خمیس مشیط"},
    "Khartoum": {"tr": "Hartum", "ar": "الخرطوم", "fa": "خارطوم"},
    "Kolkata": {"tr": "Kalküta", "ar": "كلكتا", "fa": "کلکته"},
    "Kota Bharu": {"tr": "Kota Bharu", "ar": "كوتا بهارو", "fa": "کوتا بهارو"},
    "Krakow": {"tr": "Kraków", "ar": "كراكوف", "fa": "کراکوف"},
    "Kuala Lumpur": {"tr": "Kuala Lumpur", "ar": "كوالالمبور", "fa": "کوالالامپور"},
    "Kuala Terengganu": {"tr": "Kuala Terengganu", "ar": "كوالا ترغكانو", "fa": "کوالا ترنگانو"},
    "Kudus": {"tr": "Kudus", "ar": "قدس", "fa": "کودوس"},
    "Kuwait City": {"tr": "Kuveyt Şehri", "ar": "مدينة الكويت", "fa": "کویت"},
    "Kyoto": {"tr": "Kyoto", "ar": "كيوتو", "fa": "کیوتو"},
    "Lagos": {"tr": "Lagos", "ar": "لاغوس", "fa": "لاگوس"},
    "Lahore": {"tr": "Lahor", "ar": "لاهور", "fa": "لاهور"},
    "Latakia": {"tr": "Lazkiye", "ar": "اللاذقية", "fa": "لاذقیه"},
    "Lisbon": {"tr": "Lizbon", "ar": "لشبونة", "fa": "لیسبون"},
    "London": {"tr": "Londra", "ar": "لندن", "fa": "لندن"},
    "Los Angeles": {"tr": "Los Angeles", "ar": "لوس أنجلوس", "fa": "لسآنجلس"},
    "Luxor": {"tr": "Luksor", "ar": "الأقصر", "fa": "اقصر"},
    "Lyon": {"tr": "Lyon", "ar": "ليون", "fa": "لیون"},
    "Madrid": {"tr": "Madrid", "ar": "مدريد", "fa": "مادرید"},
    "Makassar": {"tr": "Makassar", "ar": "ماكاسار", "fa": "ماکاسار"},
    "Manama": {"tr": "Manama", "ar": "المنامة", "fa": "منامه"},
    "Manchester": {"tr": "Manchester", "ar": "مانشستر", "fa": "منچستر"},
    "Manila": {"tr": "Manila", "ar": "مانيلا", "fa": "مانیل"},
    "Mansoura": {"tr": "Mansure", "ar": "المنصورة", "fa": "منصوره"},
    "Marrakech": {"tr": "Marakeş", "ar": "مراكش", "fa": "مراکش"},
    "Marseille": {"tr": "Marsilya", "ar": "مارسيليا", "fa": "مارسی"},
    "Mashhad": {"tr": "Meşhed", "ar": "مشهد", "fa": "مشهد"},
    "Medan": {"tr": "Medan", "ar": "ميدان", "fa": "مدان"},
    "Medine": {"tr": "Medine", "ar": "المدينة", "fa": "مدینه"},
    "Mekka": {"tr": "Mekke", "ar": "مكة المكرمة", "fa": "مکه"},
    "Melbourne": {"tr": "Melbourne", "ar": "ملبورن", "fa": "ملبورن"},
    "Milan": {"tr": "Milano", "ar": "ميلانو", "fa": "میلان"},
    "Mogadishu": {"tr": "Mogadişu", "ar": "مقديشو", "fa": "موگادیشو"},
    "Montreal": {"tr": "Montreal", "ar": "مونتريال", "fa": "مونترآل"},
    "Moscow": {"tr": "Moskova", "ar": "موسكو", "fa": "مسکو"},
    "Mosul": {"tr": "Musul", "ar": "الموصل", "fa": "موصل"},
    "Muharraq": {"tr": "Muharrak", "ar": "المحرق", "fa": "محرق"},
    "Multan": {"tr": "Multan", "ar": "ملتان", "fa": "مولتان"},
    "Mumbai": {"tr": "Mumbai", "ar": "مومباي", "fa": "بمبئی"},
    "Munich": {"tr": "Münih", "ar": "ميونخ", "fa": "مونیخ"},
    "Muscat": {"tr": "Maskat", "ar": "مسقط", "fa": "مسقط"},
    "Nairobi": {"tr": "Nairobi", "ar": "نيروبي", "fa": "نایروبی"},
    "Najaf": {"tr": "Necef", "ar": "النجف", "fa": "نجف"},
    "Najran": {"tr": "Necran", "ar": "نجران", "fa": "نجران"},
    "Nanjing": {"tr": "Nankin", "ar": "نانجينغ", "fa": "نانجینگ"},
    "New York": {"tr": "New York", "ar": "نيويورك", "fa": "نیویورک"},
    "Novosibirsk": {"tr": "Novosibirsk", "ar": "نوفوسيبيرسك", "fa": "نووسیبیرسک"},
    "Oran": {"tr": "Vahran", "ar": "وهران", "fa": "وهران"},
    "Osaka": {"tr": "Osaka", "ar": "أوساكا", "fa": "اوساکا"},
    "Oslo": {"tr": "Oslo", "ar": "أوسلو", "fa": "اسلو"},
    "Ouargla": {"tr": "Vargla", "ar": "ورقلة", "fa": "ورقله"},
    "Palembang": {"tr": "Palembang", "ar": "باليمبانغ", "fa": "پالمبانگ"},
    "Paris": {"tr": "Paris", "ar": "باريس", "fa": "پاریس"},
    "Penang": {"tr": "Penang", "ar": "بينانغ", "fa": "پنانگ"},
    "Perth": {"tr": "Perth", "ar": "بيرث", "fa": "پرت"},
    "Peshawar": {"tr": "Peşaver", "ar": "بيشاور", "fa": "پیشاور"},
    "Phnom Penh": {"tr": "Phnom Penh", "ar": "بنوم بنه", "fa": "پنومپن"},
    "Phoenix": {"tr": "Phoenix", "ar": "فينيكس", "fa": "فینیکس"},
    "Porto": {"tr": "Porto", "ar": "بورتو", "fa": "پورتو"},
    "Prague": {"tr": "Prag", "ar": "براغ", "fa": "پراگ"},
    "Qassim": {"tr": "El Kasim", "ar": "القصيم", "fa": "قصیم"},
    "Qom": {"tr": "Kum", "ar": "قم", "fa": "قم"},
    "Rabat": {"tr": "Rabat", "ar": "الرباط", "fa": "رباط"},
    "Rawalpindi": {"tr": "Ravalpindi", "ar": "روالبندي", "fa": "راولپندی"},
    "Riffa": {"tr": "Rifa", "ar": "الرفاع", "fa": "رفاع"},
    "Riyadh": {"tr": "Riyad", "ar": "الرياض", "fa": "ریاض"},
    "Rome": {"tr": "Roma", "ar": "روما", "fa": "رم"},
    "Rotterdam": {"tr": "Rotterdam", "ar": "روتردام", "fa": "روتردام"},
    "Saint Petersburg": {"tr": "Sankt-Peterburg", "ar": "سانت بطرسبرغ", "fa": "سنپترزبورگ"},
    "Salalah": {"tr": "Salale", "ar": "صلالة", "fa": "صلاله"},
    "Sargodha": {"tr": "Sargodha", "ar": "سرغودها", "fa": "سرگودها"},
    "Semarang": {"tr": "Semarang", "ar": "سمارانغ", "fa": "سمارانگ"},
    "Seoul": {"tr": "Seul", "ar": "سيول", "fa": "سئول"},
    "Sfax": {"tr": "Safaks", "ar": "صفاقس", "fa": "صفاقس"},
    "Shanghai": {"tr": "Şanghay", "ar": "شنغهاي", "fa": "شانگهای"},
    "Sharjah": {"tr": "Şarika", "ar": "الشارقة", "fa": "شارجه"},
    "Shenzhen": {"tr": "Şenzen", "ar": "شنتشن", "fa": "شنژن"},
    "Shiraz": {"tr": "Şiraz", "ar": "شيراز", "fa": "شیراز"},
    "Sialkot": {"tr": "Siyalkot", "ar": "سيالكوت", "fa": "سیالکوت"},
    "Sidon": {"tr": "Sayda", "ar": "صيدا", "fa": "صیدا"},
    "Singapore": {"tr": "Singapur", "ar": "سنغافورة", "fa": "سنگاپور"},
    "Sohar": {"tr": "Suhar", "ar": "صحار", "fa": "صحار"},
    "Sousse": {"tr": "Susa", "ar": "سوسة", "fa": "سوسه"},
    "Stockholm": {"tr": "Stockholm", "ar": "ستوكهولم", "fa": "استکهلم"},
    "Suez": {"tr": "Süveyş", "ar": "السويس", "fa": "سوئز"},
    "Surabaya": {"tr": "Surabaya", "ar": "سورابايا", "fa": "سورابایا"},
    "Sydney": {"tr": "Sidney", "ar": "سيدني", "fa": "سیدنی"},
    "Tabriz": {"tr": "Tebriz", "ar": "تبريز", "fa": "تبریز"},
    "Tabuk": {"tr": "Tebük", "ar": "تبوك", "fa": "تبوک"},
    "Taif": {"tr": "Taif", "ar": "الطائف", "fa": "طائف"},
    "Taipei": {"tr": "Taipei", "ar": "تايبيه", "fa": "تایپه"},
    "Tangerang": {"tr": "Tangerang", "ar": "تانغيرانغ", "fa": "تانگرانگ"},
    "Tangier": {"tr": "Tanca", "ar": "طنجة", "fa": "طنجه"},
    "Tanta": {"tr": "Tanta", "ar": "طنطا", "fa": "طنطا"},
    "Tehran": {"tr": "Tahran", "ar": "طهران", "fa": "تهران"},
    "Thessaloniki": {"tr": "Selanik", "ar": "سالونيك", "fa": "سالونیک"},
    "Tokyo": {"tr": "Tokyo", "ar": "طوكيو", "fa": "توکیو"},
    "Toronto": {"tr": "Toronto", "ar": "تورونتو", "fa": "تورنتو"},
    "Tripoli": {"tr": "Trablus", "ar": "طرابلس", "fa": "طرابلس"},
    "Tunis": {"tr": "Tunus", "ar": "تونس", "fa": "تونس"},
    "Vancouver": {"tr": "Vancouver", "ar": "فانكوفر", "fa": "ونکوور"},
    "Vienna": {"tr": "Viyana", "ar": "فيينا", "fa": "وین"},
    "Warsaw": {"tr": "Varşova", "ar": "وارسو", "fa": "ورشو"},
    "Wuhan": {"tr": "Vuhan", "ar": "ووهان", "fa": "ووهان"},
    "Xian": {"tr": "Şian", "ar": "شيان", "fa": "شیان"},
    "Yanbu": {"tr": "Yenbu", "ar": "ينبع", "fa": "ینبع"},
    "Yekaterinburg": {"tr": "Yekaterinburg", "ar": "يكاترينبورغ", "fa": "یکاترینبورگ"},
    "Zagazig": {"tr": "Zekazik", "ar": "الزقازيق", "fa": "زقازیق"},
    "Zarqa": {"tr": "Zerka", "ar": "الزرقاء", "fa": "زرقاء"},
    "Zurich": {"tr": "Zürih", "ar": "زيورخ", "fa": "زوریخ"},
}

def main():
    with open(CITIES_JSON, encoding="utf-8") as f:
        data = json.load(f)
    updated = 0
    for city in data["cities"]:
        name = city["name"]
        name_trans = CITIES.get(name)
        if name_trans is None and city["country"] == "Turkey":
            name_trans = PROVINCES.get(name)
        if name_trans:
            for lang, value in name_trans.items():
                city[f"name{lang.capitalize()}"] = value
            updated += 1
    with open(CITIES_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Localized city names for {updated} cities")

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run the script**

Run: `python3 scripts/localize_cities.py`

Expected: `Localized city names for 288 cities` (215 non-Turkey entries + 73 Turkey province-center entries whose `name` equals the province, e.g. `name="Izmir"`).

- [ ] **Step 3: Verify the output**

Run: `python3 -c "import json; d=json.load(open('prayer_settings/src/main/assets/cities.json')); c=[x for x in d['cities'] if x['name']=='Riyadh'][0]; print(c['nameTr'], c['nameAr'], c['nameFa'])`

Expected: `Riyad الرياض ریاض`

- [ ] **Step 4: Commit**

```bash
git add scripts/localize_cities.py prayer_settings/src/main/assets/cities.json
git commit -m "data(settings): localize non-Turkey city names in preset cities"
```

---

## Task 8: Data verification test

**Files:**
- Create: `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/repository/PresetCitiesLocalizationTest.kt`

- [ ] **Step 1: Write the failing test**

Create `PresetCitiesLocalizationTest.kt` (Robolectric, loads the real `cities.json` asset):

```kotlin
package com.kutluoglu.prayer_settings.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.CityList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PresetCitiesLocalizationTest {

    private lateinit var context: Context
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        context = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
    }

    private fun loadCities(): CityList {
        val input = context.assets.open("cities.json")
        return json.decodeFromString<CityList>(input.bufferedReader().use { it.readText() })
    }

    @Test
    fun `every city has localized country fields`() {
        val cities = loadCities().cities
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.countryTr).isNotNull()
            assertThat(city.countryAr).isNotNull()
            assertThat(city.countryFa).isNotNull()
        }
    }

    @Test
    fun `every Turkey city has localized province fields`() {
        val cities = loadCities().cities.filter { it.country == "Turkey" }
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.cityTr).isNotNull()
            assertThat(city.cityAr).isNotNull()
            assertThat(city.cityFa).isNotNull()
        }
    }

    @Test
    fun `every non-Turkey city has localized name fields`() {
        val cities = loadCities().cities.filter { it.country != "Turkey" }
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.nameTr).isNotNull()
            assertThat(city.nameAr).isNotNull()
            assertThat(city.nameFa).isNotNull()
        }
    }

    @Test
    fun `Istanbul has Turkish province name`() {
        val istanbul = loadCities().cities.first { it.name == "Istanbul" && it.country == "Turkey" }
        assertThat(istanbul.cityTr).isEqualTo("İstanbul")
    }

    @Test
    fun `Turkey province centers have localized name fields`() {
        val centers = loadCities().cities.filter { it.country == "Turkey" && it.city == it.name }
        assertThat(centers).isNotEmpty()
        centers.forEach { city ->
            assertThat(city.nameTr).isNotNull()
            assertThat(city.nameAr).isNotNull()
            assertThat(city.nameFa).isNotNull()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="*PresetCitiesLocalizationTest"`

Expected: PASS (data tasks 5-7 already populated the fields).

- [ ] **Step 3: Commit**

```bash
git add prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/repository/PresetCitiesLocalizationTest.kt
git commit -m "test(settings): verify preset cities localization data"
```

---

## Task 9: Screen — localized display + Turkey highlight

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionScreen.kt`
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-tr/strings.xml`

- [ ] **Step 1: Add the new string resource**

Add to `prayer_feature/settings/src/main/res/values/strings.xml` (after the `other_countries` line):

```xml
    <string name="select_country_hint">Select a country</string>
```

Add to `prayer_feature/settings/src/main/res/values-tr/strings.xml` (after the `other_countries` line):

```xml
    <string name="select_country_hint">Bir ülke seçin</string>
```

- [ ] **Step 2: Add imports to `LocationSelectionScreen.kt`**

Add these imports (alphabetical, in the existing import block):

```kotlin
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import java.util.Locale
```

- [ ] **Step 3: Compute `languageCode` in the route**

In `LocationSelectionRoute`, add this line after `val analyticsTracker: AnalyticsTracker = koinInject()`:

```kotlin
    val languageCode = Locale.getDefault().language
```

- [ ] **Step 4: Pass `languageCode` to `PresetCitiesContent`**

In `LocationSelectionRoute`, update the `PresetCitiesContent(...)` call to add the `languageCode` argument:

```kotlin
                PresetCitiesContent(
                    uiState = uiState,
                    languageCode = languageCode,
                    searchQuery = searchQuery,
                    ...
```

- [ ] **Step 5: Update `PresetCitiesContent` signature and pass `languageCode` down**

Update the `PresetCitiesContent` signature to add `languageCode: String` as the second parameter:

```kotlin
private fun PresetCitiesContent(
    uiState: LocationSelectionUiState,
    languageCode: String,
    searchQuery: String,
    ...
```

In the `CountrySelection` branch, update the `CountryList(...)` call:

```kotlin
                CountryList(
                    countries = uiState.countries,
                    onCountryClick = onCountryClick
                )
```

In the `CitySelection` branch, update the `ProvinceListByProvince(...)` call:

```kotlin
                ProvinceListByProvince(
                    citiesByProvince = uiState.citiesByProvince,
                    selectedProvince = uiState.selectedProvince,
                    languageCode = languageCode,
                    onProvinceClick = onSelectProvince
                )
```

In the `ProvinceSelection` branch, update the `ProvinceDetailContent(...)` call:

```kotlin
                ProvinceDetailContent(
                    provinceSelection = uiState,
                    languageCode = languageCode,
                    onDistrictClick = onSelectDistrict,
                    onMainCityClick = { mainCity ->
                        onCityClick(mainCity)
                    }
                )
```

- [ ] **Step 6: Update `CountryList` to use `key` for clicks and pass `isTurkey`**

Replace the `CountryList` composable:

```kotlin
@Composable
private fun CountryList(
    countries: List<CountryInfo>,
    onCountryClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Priority countries section
        val priorityCountries = countries.filter { it.isPriority }
        val otherCountries = countries.filter { !it.isPriority }

        if (priorityCountries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.popular_countries),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(priorityCountries) { countryInfo ->
                CountryItem(
                    country = countryInfo.name,
                    cityCount = countryInfo.cityCount,
                    isPriority = true,
                    isTurkey = countryInfo.key == "Turkey",
                    onClick = { onCountryClick(countryInfo.key) }
                )
            }
        }

        if (otherCountries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.other_countries),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(otherCountries) { countryInfo ->
                CountryItem(
                    country = countryInfo.name,
                    cityCount = countryInfo.cityCount,
                    isPriority = false,
                    isTurkey = false,
                    onClick = { onCountryClick(countryInfo.key) }
                )
            }
        }
    }
}
```

- [ ] **Step 7: Update `CountryItem` with Turkey flag + stronger highlight**

Replace the `CountryItem` composable:

```kotlin
@Composable
private fun CountryItem(
    country: String,
    cityCount: Int,
    isPriority: Boolean,
    isTurkey: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTurkey -> MaterialTheme.colorScheme.primaryContainer
                isPriority -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTurkey) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTurkey) {
                    Text(
                        text = "🇹🇷",
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPriority) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "$cityCount cities",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 8: Update `ProvinceListByProvince` to display localized province names**

Replace the `ProvinceListByProvince` composable:

```kotlin
@Composable
private fun ProvinceListByProvince(
    citiesByProvince: Map<String, List<City>>,
    selectedProvince: String?,
    languageCode: String,
    onProvinceClick: (String, City) -> Unit
) {
    val filteredData = if (selectedProvince != null) {
        mapOf(selectedProvince to (citiesByProvince[selectedProvince] ?: emptyList()))
    } else {
        citiesByProvince
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredData.forEach { (province, cities) ->
            val mainCity = cities.firstOrNull { it.city == province && it.name == province }
                ?: cities.firstOrNull()
            val displayProvince = CityLocalizer.localizedProvince(cities.first(), languageCode)

            item {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayProvince,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${cities.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(cities.distinctBy { it.name }) { city ->
                ProvinceItem(
                    city = city,
                    isMainCity = city.name == province,
                    languageCode = languageCode,
                    onClick = { onProvinceClick(province, mainCity ?: city) }
                )
            }
        }
    }
}
```

- [ ] **Step 9: Update `ProvinceItem` to display localized city names**

Replace the `ProvinceItem` composable signature and the city-name `Text`:

```kotlin
@Composable
private fun ProvinceItem(
    city: City,
    isMainCity: Boolean,
    languageCode: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isMainCity)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isMainCity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CityLocalizer.localizedName(city, languageCode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isMainCity) FontWeight.SemiBold else FontWeight.Medium
                )
                Row {
                    Text(
                        text = "%.4f°N".format(city.latitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "  ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "%.4f°E".format(city.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isMainCity) {
                Text(
                    text = stringResource(R.string.province_capital),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

- [ ] **Step 10: Update `ProvinceDetailContent` to display localized names**

Replace the `ProvinceDetailContent` composable:

```kotlin
@Composable
private fun ProvinceDetailContent(
    provinceSelection: LocationSelectionUiState.ProvinceSelection,
    languageCode: String,
    onDistrictClick: (City) -> Unit,
    onMainCityClick: (City) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = CityLocalizer.localizedProvince(provinceSelection.mainCity, languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CityLocalizer.localizedCountry(provinceSelection.mainCity, languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.province_capital),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            ProvinceItem(
                city = provinceSelection.mainCity,
                isMainCity = true,
                languageCode = languageCode,
                onClick = { onMainCityClick(provinceSelection.mainCity) }
            )
        }

        if (provinceSelection.districts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.districts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp)
                )
            }

            items(provinceSelection.districts) { district ->
                ProvinceItem(
                    city = district,
                    isMainCity = false,
                    languageCode = languageCode,
                    onClick = { onDistrictClick(district) }
                )
            }
        }
    }
}
```

- [ ] **Step 11: Verify the module compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`

Expected: COMPILES.

- [ ] **Step 12: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionScreen.kt prayer_feature/settings/src/main/res/values/strings.xml prayer_feature/settings/src/main/res/values-tr/strings.xml
git commit -m "feat(settings): localize preset city display and highlight Turkey"
```

---

## Task 10: Screen — landscape two-pane master/detail

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionScreen.kt`

- [ ] **Step 1: Wrap the tab content in `BoxWithConstraints`**

In `LocationSelectionRoute`, replace the `Scaffold` content lambda body. Find the `Column` that starts after `) { paddingValues ->` and replace it so the tab content is wrapped in `BoxWithConstraints` and the Preset tab renders the two-pane layout in landscape:

```kotlin
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLandscape = maxWidth > maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                // Use My Location Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = { requestLocationAndUseMyLocation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.use_my_location))
                    }
                }

                // Tabs
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Tab Content
                if (isLandscape && selectedTabIndex == 0) {
                    PresetCitiesLandscapeContent(
                        uiState = uiState,
                        languageCode = languageCode,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearch = { viewModel.onEvent(LocationSelectionEvent.SearchCountry(it)) },
                        onCountryClick = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) },
                        onCityClick = { city ->
                            viewModel.onEvent(LocationSelectionEvent.SelectCity(city))
                        },
                        onSelectProvince = { province, mainCity ->
                            viewModel.onEvent(LocationSelectionEvent.SelectProvince(province, mainCity))
                        },
                        onSelectDistrict = { district ->
                            viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))
                        }
                    )
                } else {
                    AnimatedVisibility(
                        visible = selectedTabIndex == 0,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        PresetCitiesContent(
                            uiState = uiState,
                            languageCode = languageCode,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = { viewModel.onEvent(LocationSelectionEvent.SearchCountry(it)) },
                            onCountryClick = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) },
                            onCityClick = { city ->
                                viewModel.onEvent(LocationSelectionEvent.SelectCity(city))
                            },
                            onSelectProvince = { province, mainCity ->
                                viewModel.onEvent(LocationSelectionEvent.SelectProvince(province, mainCity))
                            },
                            onSelectDistrict = { district ->
                                viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))
                            },
                            showCountryFilter = showCountryFilter,
                            onShowCountryFilter = { showCountryFilter = !showCountryFilter },
                            onCountrySelect = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedTabIndex == 1,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        SearchTab(
                            uiState = uiState,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = { viewModel.onEvent(LocationSelectionEvent.Search(it)) },
                            onClearSearch = {
                                searchQuery = ""
                                viewModel.onEvent(LocationSelectionEvent.ClearSearch)
                            },
                            onCityClick = { viewModel.onEvent(LocationSelectionEvent.SelectCity(it)) },
                            onClearHistory = { viewModel.onEvent(LocationSelectionEvent.ClearHistory) }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedTabIndex == 2,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        MapTab(
                            uiState = uiState,
                            onLocationSelected = { lat, lon ->
                                viewModel.onEvent(LocationSelectionEvent.UpdateMapLocation(lat, lon))
                            },
                            onConfirmLocation = { location ->
                                viewModel.onEvent(LocationSelectionEvent.ConfirmMapLocation(location))
                            }
                        )
                    }
                }
            }
        }
    }
```

- [ ] **Step 2: Add the `PresetCitiesLandscapeContent` composable**

Add this new composable after `PresetCitiesContent` (before `CountryList`):

```kotlin
@Composable
private fun PresetCitiesLandscapeContent(
    uiState: LocationSelectionUiState,
    languageCode: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCountryClick: (String) -> Unit,
    onCityClick: (City) -> Unit,
    onSelectProvince: (String, City) -> Unit,
    onSelectDistrict: (City) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val countries = when (uiState) {
        is LocationSelectionUiState.CountrySelection -> uiState.countries
        is LocationSelectionUiState.CitySelection -> uiState.countries
        is LocationSelectionUiState.ProvinceSelection -> uiState.countries
        else -> emptyList()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left pane: master (country search + country list)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_country)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            onSearch("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp)
            )

            CountryList(
                countries = countries,
                onCountryClick = onCountryClick
            )
        }

        VerticalDivider()

        // Right pane: detail (provinces / districts)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (uiState) {
                is LocationSelectionUiState.Loading -> {
                    SkeletonList(itemCount = 10)
                }

                is LocationSelectionUiState.CountrySelection -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.select_country_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LocationSelectionUiState.CitySelection -> {
                    ProvinceListByProvince(
                        citiesByProvince = uiState.citiesByProvince,
                        selectedProvince = uiState.selectedProvince,
                        languageCode = languageCode,
                        onProvinceClick = onSelectProvince
                    )
                }

                is LocationSelectionUiState.ProvinceSelection -> {
                    ProvinceDetailContent(
                        provinceSelection = uiState,
                        languageCode = languageCode,
                        onDistrictClick = onSelectDistrict,
                        onMainCityClick = onCityClick
                    )
                }

                is LocationSelectionUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = {}
                    )
                }

                else -> {}
            }
        }
    }
}
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`

Expected: COMPILES.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionScreen.kt
git commit -m "feat(settings): add landscape two-pane layout to preset cities tab"
```

---

## Task 11: Full verification

- [ ] **Step 1: Run all settings feature tests**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 2: Run all prayer_settings tests**

Run: `./gradlew :prayer_settings:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew unitTests`

Expected: PASS.

- [ ] **Step 4: Build the debug APK**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Verify the git diff scope with GitNexus**

Run: `gitnexus_detect_changes()` (via the GitNexus MCP tool).

Expected: changed symbols are limited to `City`, `CityLocalizer`, `LocationSelectionContract`, `LocationSelectionViewModel`, `LocationSelectionScreen`, and the data/script files. No unexpected execution flows affected.

- [ ] **Step 6: Final commit (if any uncommitted changes remain)**

```bash
git status
git add -A
git commit -m "chore(settings): finalize preset cities tab localization and landscape"
```
