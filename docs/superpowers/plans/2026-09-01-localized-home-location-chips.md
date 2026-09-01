# Localized Home Location Chips Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the home screen location chips render localized city/country names (TR/AR/FA) for **newly saved** locations, using the app's active language.

**Architecture:** Add nullable localized display-name fields (`displayNameTr/Ar/Fa`) to the shared `LocationEntry` model (`prayer:model`, a leaf module). Populate them at save time in the settings `LocationSelectionViewModel.saveLocation()` using the existing `CityLocalizer`. Add a pure `LocationNameLocalizer` object in `prayer:model` that picks the localized field based on a language code. Render chips in the home `LocationChipsRow` through this localizer, injecting the app language via the existing `LanguageProvider` (Koin singleton in `core:designsystem`). Existing already-saved entries keep their stored English `displayName` (backfill deferred — documented limitation). GPS chip keeps its device-locale geocoded name (localized variants null → fallback to `displayName`).

**Scope decision (agreed with user):** New saves only; backfill of existing saved entries deferred to a follow-up (blocked by `prayer_location` → `prayer_settings` dependency direction, which prevents `LocationsMigration` from reading the city catalog).

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (KSP), kotlinx.serialization, DataStore, JUnit 5 + MockK + Truth + Turbine.

---

## File Structure

**Model (new shared localizer + model fields):**
- Modify: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt` — add `displayNameTr`, `displayNameAr`, `displayNameFa` nullable fields with `= null` defaults
- Create: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationNameLocalizer.kt` — pure object with `localized(entry, languageCode)`
- Test: `prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationNameLocalizerTest.kt`

**Settings save (populate localized fields):**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt:323-346` — `saveLocation()` sets `displayNameTr/Ar/Fa`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt`

**Home render (language-aware chip text):**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt:109` — use localizer
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt` — thread `languageCode` (and/or `LanguageProvider`)
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationPagerCountdownTest.kt`

---

## Task 1: Extend `LocationEntry` model with localized display names

**Files:**
- Modify: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt`
- Test: `prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationEntrySerializationTest.kt` (Create)

- [ ] **Step 1: Write the failing serialization test**

- [ ] **Step 0: Add test infrastructure to `prayer:model` (setup)**

`prayer:model` is a pure `java-library` Kotlin JVM module with **no test dependencies**. Add the standard JUnit 5 test deps used across the repo (same block as `prayer/domain/build.gradle.kts`) and enable the JUnit platform:

```kotlin
// inside dependencies { } of prayer/model/build.gradle.kts
testImplementation(platform(libs.junit.bom))
testImplementation(libs.junit.jupiter.api)
testRuntimeOnly(libs.junit.jupiter.engine)

// at the end of the file
tasks.withType<Test> { useJUnitPlatform() }
```

Note: `prayer:model` is NOT an Android module, so its test task is `test`, NOT `testDebugUnitTest`.

- [ ] **Step 1: Write the failing test**

Create `prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationEntrySerializationTest.kt`:

```kotlin
package com.kutluoglu.prayer.model.location

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocationEntrySerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes legacy entry without localized fields as null`() {
        val legacy = """
            {"id":"x","location":{"latitude":1.0,"longitude":2.0,"country":"Turkey",
             "countryCode":"TR","city":"Istanbul","county":null},"isAutoGps":false,
             "displayName":"Istanbul, Turkey"}
        """.trimIndent()
        val entry = json.decodeFromString<LocationEntry>(legacy)
        assertNull(entry.displayNameTr)
        assertNull(entry.displayNameAr)
        assertNull(entry.displayNameFa)
    }

    @Test
    fun `encodes and decodes localized fields round trip`() {
        val entry = LocationEntry(
            id = "x",
            location = LocationData(1.0, 2.0, "Turkey", "TR", "Istanbul", null),
            displayName = "Istanbul, Turkey",
            displayNameTr = "İstanbul, Türkiye",
            displayNameAr = "إسطنبول، تركيا",
            displayNameFa = "استانبول، ترکیه"
        )
        val roundTripped = json.decodeFromString<LocationEntry>(json.encodeToString(LocationEntry.serializer(), entry))
        assertEquals("İstanbul, Türkiye", roundTripped.displayNameTr)
        assertEquals("إسطنبول، تركيا", roundTripped.displayNameAr)
        assertEquals("استانبول، ترکیه", roundTripped.displayNameFa)
    }
}
```

Note: this module uses JUnit 5 (assertJ/truth available per module build config, but keep it to `org.junit.jupiter` + `assertEquals` for a leaf module; if Truth is configured in `prayer/model/build.gradle.kts`, prefer `assertThat`).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:model:test --tests="com.kutluoglu.prayer.model.location.LocationEntrySerializationTest"`
Expected: FAIL to compile — `displayNameTr` does not exist on `LocationEntry`.

- [ ] **Step 3: Add the fields to `LocationEntry`**

In `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt`, change:

```kotlin
@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String
)
```

to:

```kotlin
@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String,
    val displayNameTr: String? = null,
    val displayNameAr: String? = null,
    val displayNameFa: String? = null
)
```

The `= null` defaults keep decoding of legacy persisted JSON working (missing fields fall back to null). `LocationDataStore`'s `Json { ignoreUnknownKeys = true }` already tolerates new keys when reading old data and null defaults when writing.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer:model:test --tests="com.kutluoglu.prayer.model.location.LocationEntrySerializationTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationEntry.kt \
        prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationEntrySerializationTest.kt
git commit -m "feat(model): add localized display names to LocationEntry"
```

---

## Task 2: `LocationNameLocalizer` pure helper

**Files:**
- Create: `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationNameLocalizer.kt`
- Test: `prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationNameLocalizerTest.kt` (Create)

- [ ] **Step 1: Write the failing test**

Create `prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationNameLocalizerTest.kt`:

```kotlin
package com.kutluoglu.prayer.model.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocationNameLocalizerTest {

    private val entry = LocationEntry(
        id = "x",
        location = LocationData(1.0, 2.0, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey",
        displayNameTr = "İstanbul, Türkiye",
        displayNameAr = "إسطنبول، تركيا",
        displayNameFa = "استانبول، ترکیه"
    )

    @Test
    fun `returns english displayName when language is not localized`() {
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "en"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "de"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "fr"))
    }

    @Test
    fun `returns turkish displayName for tr`() {
        assertEquals("İstanbul, Türkiye", LocationNameLocalizer.localized(entry, "tr"))
    }

    @Test
    fun `returns arabic displayName for ar`() {
        assertEquals("إسطنبول، تركيا", LocationNameLocalizer.localized(entry, "ar"))
    }

    @Test
    fun `returns farsi displayName for fa`() {
        assertEquals("استانبول، ترکیه", LocationNameLocalizer.localized(entry, "fa"))
    }

    @Test
    fun `falls back to english when localized field missing`() {
        val noLocalized = entry.copy(displayNameTr = null, displayNameAr = null, displayNameFa = null)
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "tr"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "ar"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "fa"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:model:test --tests="com.kutluoglu.prayer.model.location.LocationNameLocalizerTest"`
Expected: FAIL to compile — `LocationNameLocalizer` is not defined.

- [ ] **Step 3: Implement `LocationNameLocalizer`**

Create `prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationNameLocalizer.kt`:

```kotlin
package com.kutluoglu.prayer.model.location

/**
 * Resolves the display name for a [LocationEntry] in the active app language.
 * Falling back to the stored English [LocationEntry.displayName] when no
 * localized variant is available for the requested [languageCode].
 */
object LocationNameLocalizer {

    fun localized(entry: LocationEntry, languageCode: String): String = when (languageCode) {
        "tr" -> entry.displayNameTr ?: entry.displayName
        "ar" -> entry.displayNameAr ?: entry.displayName
        "fa" -> entry.displayNameFa ?: entry.displayName
        else -> entry.displayName
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer:model:test --tests="com.kutluoglu.prayer.model.location.LocationNameLocalizerTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/location/LocationNameLocalizer.kt \
        prayer/model/src/test/java/com/kutluoglu/prayer/model/location/LocationNameLocalizerTest.kt
git commit -m "feat(model): add LocationNameLocalizer for localized location names"
```

---

## Task 3: Populate localized names on save (settings)

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt:323-346`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt`

Context: `saveLocation(city: City)` currently builds a `LocationData` then a `LocationEntry` with only the English `displayName`. We add a private helper `localizedDisplayName(city, lang)` that reuses `CityLocalizer` for province + country and keeps the (non-localized) county as-is.

- [ ] **Step 1: Write the failing test**

In `LocationSelectionViewModelTest.kt`, add a test that captures the `LocationEntry` passed to `locationsCoordinator.addLocation(...)` and asserts the localized fields are populated. The test file already stubs `locationsCoordinator.addLocation`. Add:

```kotlin
@Test
fun `saveLocation populates localized display names`() = runTest {
    val captured = mutableListOf<LocationEntry>()
    coEvery { locationsCoordinator.addLocation(capture(captured)) } returns Unit

    val district = City(
        name = "Fatih",
        country = "Turkey",
        latitude = 41.0364,
        longitude = 28.9603,
        timezone = "Europe/Istanbul",
        city = "Istanbul",
        county = "Fatih",
        nameTr = "Fatih",
        nameAr = "فاتح",
        nameFa = "فاتح",
        countryTr = "Türkiye",
        countryAr = "تركيا",
        countryFa = "ترکیه",
        cityTr = "İstanbul",
        cityAr = "إسطنبول",
        cityFa = "استانبول"
    )
    viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))

    val entry = captured.first()
    assertThat(entry.displayNameTr).isEqualTo("Fatih, İstanbul, Türkiye")
    assertThat(entry.displayNameAr).isEqualTo("Fatih, إسطنبول, تركيا")
    assertThat(entry.displayNameFa).isEqualTo("Fatih, استانبول, ترکیه")
    assertThat(entry.displayName).isEqualTo("Fatih, Istanbul, Turkey")
}
```

Add the missing imports if not present:
```kotlin
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.LocationEntry
import io.mockk.capture
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.location.LocationSelectionViewModelTest"`
Expected: FAIL — `entry.displayNameTr` is null (assertion fails).

- [ ] **Step 3: Implement the localized save**

In `LocationSelectionViewModel.kt`, add a private helper and update `saveLocation`. Add the helper (it mirrors exactly the county/part logic of the existing English `displayName` builder so localized output is structurally identical, but localizes province + country):

```kotlin
private fun localizedDisplayName(city: City, languageCode: String): String {
    val county = city.county?.takeIf { it.isNotBlank() }
        ?: city.name.takeIf { it != city.province }
    val parts = listOfNotNull(
        county,
        CityLocalizer.localizedProvince(city, languageCode),
        CityLocalizer.localizedCountry(city, languageCode)
    )
    return parts.joinToString(", ").ifBlank { "My Location" }
}
```

Update `saveLocation(city: City)` to:

```kotlin
private suspend fun saveLocation(city: City) {
    val countryCode = getCountryCode(city.country)
    val location = LocationData(
        latitude = city.latitude,
        longitude = city.longitude,
        country = city.country,
        countryCode = countryCode,
        city = city.province,
        county = city.county?.takeIf { it.isNotBlank() }
            ?: city.name.takeIf { it != city.province }
    )
    locationsCoordinator.addLocation(
        LocationEntry(
            id = UUID.randomUUID().toString(),
            location = location,
            displayName = listOfNotNull(location.county, location.city, location.country)
                .joinToString(", ").ifBlank { "My Location" },
            displayNameTr = localizedDisplayName(city, "tr"),
            displayNameAr = localizedDisplayName(city, "ar"),
            displayNameFa = localizedDisplayName(city, "fa")
        )
    )
    analyticsTracker.logEvent(
        AnalyticsEvents.LOCATION_ADDED,
        mapOf(AnalyticsParams.SOURCE to "search")
    )
}
```

Note: `CityLocalizer` is in the same package (`com.kutluoglu.prayer_feature.settings.location`), so no import change is required. `buildList` requires Kotlin stdlib (already used). Verify the `LocationSelectionViewModel` already imports `City` (it does — used in `selectDistrict`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.location.LocationSelectionViewModelTest"`
Expected: PASS. Then run the whole module test to ensure no regressions:
`./gradlew :prayer_feature:settings:testDebugUnitTest`

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModel.kt \
        prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/LocationSelectionViewModelTest.kt
git commit -m "feat(settings): persist localized display names when saving locations"
```

---

## Task 4: Render localized chip text in home

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt:109`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationPagerCountdownTest.kt` (add `languageCode`)
- Create: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationChipsLocalizationTest.kt`

Context: `LocationChipsRow` renders `text = entry.displayName`. We thread the active language code as an explicit `languageCode: String` parameter from the top: `HomeRoute` resolves the existing `LanguageProvider` (Koin singleton in `core:designsystem`) via the module's established `org.koin.compose.koinInject` pattern and passes `languageCode` down through `HomeScreen → LocationPager → LocationChipsRow`. Every composable in that chain stays pure/testable (takes `languageCode` as a plain required param). This follows the existing home convention (`HomeRoute` already uses `koinInject` for `QuranVerseFormatter`, `PrayerLogicEngine`, `PrayerFormatter`) and keeps `LocationPagerCountdownTest`/Robolectric tests DI-free.

**Decision (fixed):** resolve `LanguageProvider.getLanguageCode()` in `HomeRoute` ONLY via `koinInject`; do NOT add language state to `HomeViewModel` (larger blast radius, unnecessary since language changes trigger activity recreation per the language-agnostic plan). Pass `languageCode` as required (non-defaulted) params through `HomeScreen`, `LocationPager`, and `LocationChipsRow` so callers are forced to be explicit.

- [ ] **Step 1: Write the failing localization test (before wiring)**

Create `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationChipsLocalizationTest.kt`, mirroring `LocationPagerCountdownTest`'s Robolectric harness (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [35])`, `createAndroidComposeRule<ComponentActivity>()`):

```kotlin
@Test
fun `chip renders localized name for turkish`() {
    val entry = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey",
        displayNameTr = "İstanbul, Türkiye",
        displayNameAr = "إسطنبول، تركيا"
    )
    composeTestRule.setContent {
        LocationChipsRow(
            entries = listOf(entry),
            selectedId = null,
            pagerState = PagerState(initialPage = 0, initialPageOffsetFraction = 0f),
            onLocationSelected = {},
            onAddLocation = {},
            languageCode = "tr"
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("İstanbul, Türkiye").assertIsDisplayed()
}
```

Also add a test asserting the ENGLISH fallback when `languageCode = "en"` shows `"Istanbul, Turkey"` (guards the else branch).
This test will NOT compile yet (no `languageCode` param) — that is the expected RED.

- [ ] **Step 2: Apply the render wiring**

`LocationChipsRow.kt` — add required param + use the localizer:
- Signature: add `languageCode: String,` before `modifier`
- Line 109: `text = LocationNameLocalizer.localized(entry, languageCode)`
- Import: `import com.kutluoglu.prayer.model.location.LocationNameLocalizer`

`LocationPager.kt` — add required `languageCode: String` param; pass to `LocationChipsRow` at line 112 and to `LocationPlaceholder(entry, languageCode)` at line 143. Update `LocationPlaceholder` (private composable line 149-157) to take `languageCode` and render `LocationNameLocalizer.localized(entry, languageCode)`. Import the localizer.

`HomeScreen.kt` — add required `languageCode: String` param; pass to `LocationPager` at line 99.

`HomeRoute.kt` — resolve and pass:
```kotlin
val languageProvider = koinInject<LanguageProvider>()
val languageCode = languageProvider.getLanguageCode()
```
Add `languageCode = languageCode` to the `HomeScreen(...)` call. Import `com.kutluoglu.core.designsystem.utils.LanguageProvider` (already a home dependency via `core:designsystem` — it's already used in `SavedVersesViewModel`/`QuranVerseLoader`).

`LocationPagerCountdownTest.kt` — add `languageCode = "en"` to the existing `LocationPager(...)` call (line 71).

- [ ] **Step 3: Run tests to verify GREEN**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.LocationChipsLocalizationTest"`
Expected: PASS (both tests).
Then run the full home module test suite:
`./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS (no regressions).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt \
        prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt \
        prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt \
        prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt \
        prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationChipsLocalizationTest.kt \
        prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationPagerCountdownTest.kt
git commit -m "feat(home): render localized location chip names"
```

---

## Task 5: Full regression + impact verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full build and test suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run GitNexus change detection**

Run: `gitnexus_detect_changes()` (per AGENTS.md).
Expected: changed symbols limited to `LocationEntry`, `LocationNameLocalizer`, `LocationSelectionViewModel.saveLocation`, `LocationPager`, `LocationChipsRow`. No unexpected execution flows affected.

- [ ] **Step 3: Optional — run module-level tests**

Run the affected modules explicitly if the full run is slow:
```bash
./gradlew :prayer:model:test \
          :prayer_feature:settings:testDebugUnitTest \
          :prayer_feature:home:testDebugUnitTest
```
Expected: PASS.

---

## Self-Review Notes

**Spec coverage:** Every requirement maps to a task — model fields (T1), localizer (T2), save population (T3), home render (T4), regression (T5). Backfill of existing saves is explicitly out of scope (deferred per user decision).

**Documented limitations (accepted this iteration):**
- Existing already-saved locations keep their stored English `displayName` until re-saved (backfill deferred).
- GPS chip display name is device-locale geocoded and not translated per in-app language for tr/ar/fa (its localized fields stay null → falls back to `displayName`).
- The `county` part of a saved location name is not localized (the `City` model has no localized `county` field); only province + country are localized in `displayNameTr/Ar/Fa`.
- Only TR/AR/FA localized variants are stored, matching the existing `CityLocalizer`/`City` localized-field coverage.

**Type consistency:** `LocationNameLocalizer.localized(entry, languageCode)` is defined in Task 2 and used in Tasks 3 (via `CityLocalizer`, not `LocationNameLocalizer`) and 4 (via `LocationNameLocalizer`). `LocationEntry` localized fields are named `displayNameTr/displayNameAr/displayNameFa` consistently across all tasks.
