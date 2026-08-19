# Fix Calculation Method Recalculation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make changing the "Calculation Method" in Settings actually recalculate prayer times on the Home screen (and the monthly Prayer Times screen).

**Architecture:** The calculation method is currently saved to settings but never read by the prayer-time pipeline — `PrayerDataStoreImp` hardcodes `CalculationMethod.TURKEY_DIYANET`, the cache key excludes the method, and neither `HomeViewModel` nor `PrayerTimesViewModel` observes settings changes. The fix threads `calculationMethod` as an explicit parameter from the feature layer (which reads settings via `prayer_settings`) down through the use cases, repository, and data store; includes the method in every cache key; and adds a settings observer to both ViewModels that clears the in-memory cache and reloads when the method changes.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin, kotlinx-datetime, adhan2 (0.0.6), JUnit 5 + MockK + Turbine + Truth.

---

## Root Cause (from investigation)

1. **Home never reloads on settings change** — `HomeViewModel` only observes location state, prayer-passed, and day-changed signals (`HomeViewModel.kt:60-76`). No settings observer.
2. **Method is hardcoded** — `PrayerDataStoreImp.getPrayerTimes` uses `CalculationMethod.TURKEY_DIYANET` (`PrayerDataStoreImp.kt:54`); the method never travels through `GetPrayerTimesUseCase` / `IPrayerRepository` / `PrayerDataStore`.
3. **Cache key excludes the method** — `buildCacheKey` = `"${date.date}|$latitude|$longitude|${zoneId.id}"` (`PrayerDataStoreImp.kt:120`), so even a reload returns the old method's cached times.

The monthly Prayer Times screen has the same root causes (2 & 3).

## Design Decisions

- **Thread the method as a parameter** (not read inside the data layer): `prayer:data` cannot depend on `prayer_settings` (circular — `prayer_settings` already depends on `prayer:data`). The feature layer reads settings via `GetSettingsUseCase`/`SettingsRepository` and passes the method down.
- **Default value `CalculationMethod.TURKEY_DIYANET`** on every new parameter keeps each phase buildable (callers not yet updated keep working). Both callers are updated in later phases to pass the real method.
- **Cache keys include the method**: `"${date.date}|$latitude|$longitude|${zoneId.id}|$calculationMethod"` (daily) and `"$month|$latitude|$longitude|${zoneId.id}|$calculationMethod"` (monthly). Old cached entries (no method suffix) simply miss and get recalculated once.
- **JAFARI removed**: adhan2 0.0.6 (the pinned version in `libs.versions.toml`) has **no JAFARI method** — verified from the compiled `adhan2-jvm-0.0.6.jar`: its `CalculationMethod` enum is `MUSLIM_WORLD_LEAGUE, EGYPTIAN, KARACHI, UMM_AL_QURA, DUBAI, MOON_SIGHTING_COMMITTEE, NORTH_AMERICA, KUWAIT, QATAR, SINGAPORE, TURKEY, OTHER`. JAFARI is removed from the app's settings method list (Task 1.0) and from the `prayer/model` enum (Task 1.1) so every offered method maps to a real adhan2 method.
- **adhan2 alignment**: every app method maps to a supported adhan2 method — `TURKEY_DIYANET`→`TURKEY`, `MWL`→`MUSLIM_WORLD_LEAGUE`, `ISNA`→`NORTH_AMERICA`, `EGYPT`→`EGYPTIAN`, `MAKKAH`→`UMM_AL_QURA`, `KARACHI`→`KARACHI`, `TEHRAN`→`TEHRAN`. adhan2 also supports `DUBAI`, `MOON_SIGHTING_COMMITTEE`, `KUWAIT`, `QATAR`, `SINGAPORE`, `OTHER` which the app does not offer — acceptable, the app's list is a subset.
- **Expand `prayer/model` `CalculationMethod` enum** to match the 7 settings IDs exactly (`TURKEY_DIYANET`, `MWL`, `ISNA`, `EGYPT`, `MAKKAH`, `KARACHI`, `TEHRAN`) so `fromSettingsId(id)` is a direct `valueOf`-style lookup. `MUSLIM_WORLD_LEAGUE` is renamed to `MWL` (single reference in `PrayerTimeEngine`).
- **Settings observation**: both ViewModels observe `SettingsRepository.observeSettings()`, map to `calculationMethod`, `distinctUntilChanged()`, `drop(1)` (skip the initial emission — the initial load already used the current method), then clear the in-memory cache and reload.

---

## Phase 1: Domain & Data Layer — thread the calculation method

### Task 1.0: Remove JAFARI from the app's settings method list

adhan2 0.0.6 (the pinned version) has no JAFARI method, so the app must not offer it.

**Files:**
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/CalculationMethod.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

In `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodViewModelTest.kt`:

Change the three `hasSize(8)` assertions (lines 70, 126, 134) to `hasSize(7)`:
```kotlin
        assertThat(loadedState.methods).hasSize(7)
```

Remove the JAFARI assertion (line 87):
```kotlin
        assertThat(methodIds).contains("JAFARI")
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CalculationMethodViewModelTest"`
Expected: FAIL — the list still has 8 methods (JAFARI not yet removed).

- [ ] **Step 3: Remove JAFARI from the settings list**

In `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/CalculationMethod.kt`, remove the JAFARI entry (line 17):
```kotlin
            CalculationMethod("JAFARI", "Jaafari (Imami Shiah)", "Jaafari method")
```
The `methods` list now contains exactly the 7 adhan2-supported methods.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CalculationMethodViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/CalculationMethod.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodViewModelTest.kt
git commit -m "feat: remove JAFARI calculation method (unsupported by adhan2)"
```

### Task 1.1: Expand the `prayer/model` `CalculationMethod` enum

**Files:**
- Modify: `prayer/model/src/main/java/com/kutluoglu/prayer/model/prayer/CalculationMethod.kt`
- Test: `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/CalculationMethodTest.kt` (new)

- [ ] **Step 1: Write the failing test**

Create `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/CalculationMethodTest.kt`:

```kotlin
package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import org.junit.jupiter.api.Test

class CalculationMethodTest {

    @Test
    fun `fromSettingsId maps every settings id to the matching enum`() {
        assertThat(CalculationMethod.fromSettingsId("TURKEY_DIYANET")).isEqualTo(CalculationMethod.TURKEY_DIYANET)
        assertThat(CalculationMethod.fromSettingsId("MWL")).isEqualTo(CalculationMethod.MWL)
        assertThat(CalculationMethod.fromSettingsId("ISNA")).isEqualTo(CalculationMethod.ISNA)
        assertThat(CalculationMethod.fromSettingsId("EGYPT")).isEqualTo(CalculationMethod.EGYPT)
        assertThat(CalculationMethod.fromSettingsId("MAKKAH")).isEqualTo(CalculationMethod.MAKKAH)
        assertThat(CalculationMethod.fromSettingsId("KARACHI")).isEqualTo(CalculationMethod.KARACHI)
        assertThat(CalculationMethod.fromSettingsId("TEHRAN")).isEqualTo(CalculationMethod.TEHRAN)
    }

    @Test
    fun `fromSettingsId falls back to TURKEY_DIYANET for unknown ids`() {
        assertThat(CalculationMethod.fromSettingsId("UNKNOWN")).isEqualTo(CalculationMethod.TURKEY_DIYANET)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*CalculationMethodTest"`
Expected: FAIL — `fromSettingsId` does not exist (compile error).

- [ ] **Step 3: Replace the enum**

Replace the entire contents of `prayer/model/src/main/java/com/kutluoglu/prayer/model/prayer/CalculationMethod.kt`:

```kotlin
package com.kutluoglu.prayer.model.prayer

enum class CalculationMethod {
    TURKEY_DIYANET,
    MWL,
    ISNA,
    EGYPT,
    MAKKAH,
    KARACHI,
    TEHRAN;

    companion object {
        fun fromSettingsId(id: String): CalculationMethod =
            entries.firstOrNull { it.name == id } ?: TURKEY_DIYANET
    }
}
```

- [ ] **Step 4: Fix the renamed enum reference in `PrayerTimeEngine`**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerTimeEngine.kt`, line 84, change:
```kotlin
CalculationMethod.MUSLIM_WORLD_LEAGUE -> com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE
```
to:
```kotlin
CalculationMethod.MWL -> com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*CalculationMethodTest" --tests="*PrayerCalculationServiceTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/prayer/CalculationMethod.kt prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerTimeEngine.kt prayer/domain/src/test/java/com/kutluoglu/prayer/domain/CalculationMethodTest.kt
git commit -m "feat: expand CalculationMethod enum to all settings methods"
```

### Task 1.2: Map all 7 methods in `PrayerTimeEngine`

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerTimeEngine.kt`
- Test: `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/PrayerCalculationServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/PrayerCalculationServiceTest.kt`:

```kotlin
    @Test
    fun `different calculation methods produce different fajr times`() {
        val service = PrayerTimeEngine()
        val date = LocalDateTime.createBy(2025, 9, 15)
        val latitude = 41.03648429460445
        val longitude = 28.79004556525033
        val zoneId = ZoneId.systemDefault()

        val turkey = service.calculateDailyPrayerTimes(
            latitude, longitude, zoneId, date,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
        )
        val mwl = service.calculateDailyPrayerTimes(
            latitude, longitude, zoneId, date,
            CalculationMethod.MWL, JuristicMethod.STANDARD
        )

        val turkeyFajr = turkey.first { it.name == "Fajr" }.time
        val mwlFajr = mwl.first { it.name == "Fajr" }.time
        assertThat(turkeyFajr).isNotEqualTo(mwlFajr)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*PrayerCalculationServiceTest"`
Expected: FAIL — `CalculationMethod.MWL` does not exist yet (compile error from Task 1.1 not yet applied) OR the test fails because both methods currently map to the same params. (If Task 1.1 is already committed, the enum exists but `MWL` is not yet mapped in the engine, so it fails the `when` exhaustiveness — see Step 3.)

- [ ] **Step 3: Rewrite `getCalculationParameters`**

Replace the `getCalculationParameters` function in `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerTimeEngine.kt` (currently lines 77-92) with:

```kotlin
    private fun getCalculationParameters(
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod
    ): CalculationParameters {
        val params = when (calculationMethod) {
            CalculationMethod.TURKEY_DIYANET -> com.batoulapps.adhan2.CalculationMethod.TURKEY.parameters
            CalculationMethod.MWL -> com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            CalculationMethod.ISNA -> com.batoulapps.adhan2.CalculationMethod.NORTH_AMERICA.parameters
            CalculationMethod.EGYPT -> com.batoulapps.adhan2.CalculationMethod.EGYPTIAN.parameters
            CalculationMethod.MAKKAH -> com.batoulapps.adhan2.CalculationMethod.UMM_AL_QURA.parameters
            CalculationMethod.KARACHI -> com.batoulapps.adhan2.CalculationMethod.KARACHI.parameters
            CalculationMethod.TEHRAN -> com.batoulapps.adhan2.CalculationMethod.TEHRAN.parameters
        }
        return when (juristicMethod) {
            JuristicMethod.STANDARD -> params.copy(madhab = com.batoulapps.adhan2.Madhab.SHAFI)
            JuristicMethod.HANAFI -> params.copy(madhab = com.batoulapps.adhan2.Madhab.HANAFI)
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*PrayerCalculationServiceTest"`
Expected: PASS (both the original `calculateDailyPrayerTimes successful` and the new method-difference test).

- [ ] **Step 5: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerTimeEngine.kt prayer/domain/src/test/java/com/kutluoglu/prayer/domain/PrayerCalculationServiceTest.kt
git commit -m "feat: map all calculation methods in PrayerTimeEngine"
```

### Task 1.3: Thread `calculationMethod` through the daily prayer-times pipeline

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetPrayerTimesUseCase.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`
- Test: `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/GetPrayerTimesUseCaseTest.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`

- [ ] **Step 1: Add the parameter to `GetPrayerTimesUseCase`**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetPrayerTimesUseCase.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change the `invoke` signature and body:

```kotlin
    suspend operator fun invoke(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): Result<List<Prayer>> {
        return try {
            val prayerTimes = prayerRepository.getPrayerTimes(
                date, latitude, longitude, zoneId, calculationMethod
            )
            Result.success(prayerTimes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

- [ ] **Step 2: Add the parameter to `IPrayerRepository`**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `getPrayerTimes`:

```kotlin
    suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<Prayer>
```

- [ ] **Step 3: Add the parameter to `PrayerRepository`**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `getPrayerTimes`:

```kotlin
    override suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<Prayer> = prayerDataStore.getPrayerTimes(
        date = date,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod
    )
```

- [ ] **Step 4: Add the parameter to `PrayerDataStore`**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `getPrayerTimes`:

```kotlin
    suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<Prayer>
```

- [ ] **Step 5: Use the parameter in `PrayerDataStoreImp` (daily + cache key)**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`:

Change `getPrayerTimes` (lines 35-61) to:

```kotlin
    override suspend fun getPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<Prayer> {
        val cacheKey = buildCacheKey(date, latitude, longitude, zoneId, calculationMethod)
        val cached = prayerTimesCache.get(cacheKey)
        if (cached != null) {
            preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod)
            return cached
        }

        val calculated = withContext(Dispatchers.Default) {
            prayerCalculationService.calculateDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                zoneId = zoneId,
                date = date,
                calculationMethod = calculationMethod,
                juristicMethod = JuristicMethod.STANDARD
            )
        }
        prayerTimesCache.put(cacheKey, calculated)
        preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod)
        return calculated
    }
```

Change `preCacheTomorrow` (lines 63-88) to accept and use the method:

```kotlin
    private fun preCacheTomorrow(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod
    ) {
        preCacheScope.launch {
            runCatching {
                val tomorrow = date.date.plus(1, DateTimeUnit.DAY)
                    .atTime(date.hour, date.minute, date.second, date.nanosecond)
                val tomorrowKey = buildCacheKey(tomorrow, latitude, longitude, zoneId, calculationMethod)
                if (prayerTimesCache.get(tomorrowKey) != null) return@runCatching
                val tomorrowPrayers = prayerCalculationService.calculateDailyPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    zoneId = zoneId,
                    date = tomorrow,
                    calculationMethod = calculationMethod,
                    juristicMethod = JuristicMethod.STANDARD
                )
                prayerTimesCache.put(tomorrowKey, tomorrowPrayers)
            }.onFailure { error ->
                Log.e("PrayerDataStoreImp", "Failed to pre-cache tomorrow: ${error.message}")
            }
        }
    }
```

Change `buildCacheKey` (lines 115-120) to include the method:

```kotlin
    private fun buildCacheKey(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod
    ): String = "${date.date}|$latitude|$longitude|${zoneId.id}|$calculationMethod"
```

- [ ] **Step 6: Update `GetPrayerTimesUseCaseTest`**

In `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/GetPrayerTimesUseCaseTest.kt`, update both stubs (lines 44 and 58) to add a 5th `any()` for the method:

```kotlin
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any(), zoneId, any()) } returns mockPrayerList
```
and
```kotlin
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any(), zoneId, any()) } throws exception
```

- [ ] **Step 7: Update `PrayerRepositoryTest`**

In `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and update the `getPrayerTimes` test (lines 55-62):

```kotlin
        coEvery { prayerDataStore.getPrayerTimes(any(), any(), any(), any(), any()) } returns mockPrayerList

        // Act (When)
        val result = repository.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId, CalculationMethod.TURKEY_DIYANET)

        // Assert (Then)
        coVerify(exactly = 1) {
            prayerDataStore.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId, CalculationMethod.TURKEY_DIYANET)
        }
```

- [ ] **Step 8: Update `PrayerDataStoreImpTest` cache-key assertions**

In `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`, append `|TURKEY_DIYANET` to **every** cache-key string literal. The affected lines are 64, 95, 154, 176, 213, 216, 232, 233, 254, 257, 277, 293, 294, 315. Examples:

```kotlin
        coVerify(exactly = 1) { prayerTimesCache.get("2024-01-01|41.0|29.0|Europe/Istanbul|TURKEY_DIYANET") }
```
```kotlin
        prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul|TURKEY_DIYANET", calculatedPrayers)
```
```kotlin
        prayerTimesCache.getMonth("2024-01|41.0|29.0|Europe/Istanbul|TURKEY_DIYANET")
```
```kotlin
        prayerTimesCache.putMonth("2024-01|41.0|29.0|Europe/Istanbul|TURKEY_DIYANET", monthToSave)
```

- [ ] **Step 9: Add a test that the method is used and keys the cache**

Append to `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`:

```kotlin
    @Test
    fun `getPrayerTimes uses the provided calculation method and keys cache by method`() = runTest {
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val calculatedPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        coEvery { prayerTimesCache.get(any()) } returns null
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returns calculatedPrayers

        dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId, CalculationMethod.MWL)

        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, testDate,
                CalculationMethod.MWL, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 1) {
            prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul|MWL", calculatedPrayers)
        }
    }
```

- [ ] **Step 10: Run the affected module tests**

Run:
```bash
./gradlew :prayer:domain:testDebugUnitTest
./gradlew :prayer:data:testDebugUnitTest
```
Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetPrayerTimesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt prayer/domain/src/test/java/com/kutluoglu/prayer/domain/GetPrayerTimesUseCaseTest.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt
git commit -m "feat: thread calculation method through daily prayer-times pipeline"
```

### Task 1.4: Thread `calculationMethod` through the monthly pipeline

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetMonthlyPrayerTimesUseCase.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/SaveMonthlyPrayerTimesUseCase.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt`

- [ ] **Step 1: Add the parameter to `GetMonthlyPrayerTimesUseCase`**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetMonthlyPrayerTimesUseCase.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `invoke`:

```kotlin
    suspend operator fun invoke(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>? = prayerRepository.getMonthlyPrayerTimes(
        month = month,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod
    )
```

- [ ] **Step 2: Add the parameter to `SaveMonthlyPrayerTimesUseCase`**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/SaveMonthlyPrayerTimesUseCase.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `invoke`:

```kotlin
    suspend operator fun invoke(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        prayers: List<DailyPrayer>,
    ) {
        prayerRepository.saveMonthlyPrayerTimes(
            month = month,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            prayers = prayers
        )
    }
```

- [ ] **Step 3: Add the parameter to `IPrayerRepository` monthly methods**

In `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt`, change the two monthly methods:

```kotlin
    suspend fun getMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>?

    suspend fun saveMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
            prayers: List<DailyPrayer>,
    )
```

- [ ] **Step 4: Add the parameter to `PrayerRepository` monthly methods**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt`, change the two monthly methods:

```kotlin
    override suspend fun getMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>? = prayerDataStore.getMonthlyPrayerTimes(
        month = month,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        calculationMethod = calculationMethod
    )

    override suspend fun saveMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        prayers: List<DailyPrayer>,
    ) {
        prayerDataStore.saveMonthlyPrayerTimes(
            month = month,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            prayers = prayers
        )
    }
```

- [ ] **Step 5: Add the parameter to `PrayerDataStore` monthly methods**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt`, change the two monthly methods:

```kotlin
    suspend fun getMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>?

    suspend fun saveMonthlyPrayerTimes(
        month: YearMonth,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        prayers: List<DailyPrayer>,
    )
```

- [ ] **Step 6: Use the parameter in `PrayerDataStoreImp` monthly methods + month cache key**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`, change the monthly methods (lines 90-109):

```kotlin
    override suspend fun getMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
    ): List<DailyPrayer>? {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId, calculationMethod)
        return prayerTimesCache.getMonth(cacheKey)
    }

    override suspend fun saveMonthlyPrayerTimes(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
            prayers: List<DailyPrayer>
    ) {
        val cacheKey = buildMonthCacheKey(month, latitude, longitude, zoneId, calculationMethod)
        prayerTimesCache.putMonth(cacheKey, prayers)
    }
```

And change `buildMonthCacheKey` (lines 122-127):

```kotlin
    private fun buildMonthCacheKey(
            month: YearMonth,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod
    ): String = "$month|$latitude|$longitude|${zoneId.id}|$calculationMethod"
```

- [ ] **Step 7: Update `PrayerRepositoryTest` monthly tests**

In `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt`, update the monthly tests (lines 93-123):

```kotlin
        coEvery { prayerDataStore.getMonthlyPrayerTimes(any(), any(), any(), any(), any()) } returns mockMonth

        val result = repository.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET)

        coVerify(exactly = 1) {
            prayerDataStore.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET)
        }
        assertThat(result).isEqualTo(mockMonth)
```
and
```kotlin
        coEvery { prayerDataStore.saveMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, monthToSave)

        coVerify(exactly = 1) {
            prayerDataStore.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, monthToSave)
        }
```

- [ ] **Step 8: Run the affected module tests**

Run: `./gradlew :prayer:domain:testDebugUnitTest :prayer:data:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetMonthlyPrayerTimesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/SaveMonthlyPrayerTimesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerRepositoryTest.kt
git commit -m "feat: thread calculation method through monthly prayer-times pipeline"
```

---

## Phase 2: Home screen — read settings, observe changes, reload

### Task 2.1: `PrayerTimesLoader` accepts the calculation method

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoader.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoaderTest.kt`

- [ ] **Step 1: Write the failing test**

In `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoaderTest.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod`, update the existing `load` calls (lines 47 and 63) to pass a method, and update the `getPrayerTimesUseCase` stubs (lines 40 and 60) to 5 args:

```kotlin
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns success(listOf(fajr, dhuhr))
        ...
        val result = loader.load(location, CalculationMethod.TURKEY_DIYANET)
```
and
```kotlin
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("fetch failed"))
        ...
        val result = loader.load(location, CalculationMethod.TURKEY_DIYANET)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerTimesLoaderTest"`
Expected: FAIL — `load` does not accept a second argument (compile error).

- [ ] **Step 3: Update `PrayerTimesLoader.load`**

In `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoader.kt`, add the import `com.kutluoglu.prayer.model.prayer.CalculationMethod` and change `load`:

```kotlin
    suspend fun load(location: LocationData, calculationMethod: CalculationMethod): Result<LoadedPrayerData> {
        val zoneId = getZoneIdFromLocation(location.countryCode)
        val locationDateTime = LocalDateTime.now(zoneId)
        return getPrayerTimesUseCase(
            date = locationDateTime,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod
        ).map { prayerTimes ->
            val localized = formatter.withLocalizedNames(prayerTimes)
            LoadedPrayerData(
                prayerState = computePrayerState(localized, zoneId),
                timeState = formatter.getInitialTimeInfo(zoneId),
                locationState = LocationUiState(
                    locationData = location,
                    locationInfoText = formatter.locationInfo(location)
                ),
                zoneId = zoneId
            )
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerTimesLoaderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoader.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/PrayerTimesLoaderTest.kt
git commit -m "feat: pass calculation method through PrayerTimesLoader"
```

### Task 2.2: `HomeViewModel` reads settings, observes changes, reloads

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

In `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`:

Add imports:
```kotlin
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
```

Add mock fields:
```kotlin
    private val getSettingsUseCase: GetSettingsUseCase = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
```

Update the `viewModel()` factory (lines 71-76):
```kotlin
    private fun viewModel() = HomeViewModel(
        locationsCoordinator,
        prayerTimesLoader,
        countdownEngine,
        quranVerseLoader,
        getSettingsUseCase,
        settingsRepository
    )
```

In `setUp()`, add default stubs:
```kotlin
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        every { settingsRepository.observeSettings() } returns flowOf(Settings())
```

Update every `coEvery { prayerTimesLoader.load(location) }` stub to `coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET) }` (lines 98, 110, 123, 136, 152, 176, 195, 218, 236, 254, 274, 296).

Add the new test:
```kotlin
    @Test
    fun `calculation method change clears cache and reloads with new method`() = runTest {
        val settingsFlow = MutableStateFlow(Settings(calculationMethod = "TURKEY_DIYANET"))
        coEvery { locationsCoordinator.observeState() } returns flowOf(
            LocationsState(entries = listOf(entry), selectedId = "loc-1")
        )
        coEvery { locationsCoordinator.resolveInitial() } returns location
        coEvery { locationsCoordinator.resolveSelected() } returns location
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        every { settingsRepository.observeSettings() } returns settingsFlow
        coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET) } returns success(loadedData())

        val vm = viewModel()
        coVerify(exactly = 1) { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET) }

        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "MWL")
        coEvery { prayerTimesLoader.load(location, CalculationMethod.MWL) } returns success(loadedData())
        settingsFlow.value = Settings(calculationMethod = "MWL")

        coVerify { prayerTimesLoader.load(location, CalculationMethod.MWL) }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: FAIL — `HomeViewModel` constructor does not accept the new arguments (compile error).

- [ ] **Step 3: Update `HomeViewModel`**

In `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`:

Add imports:
```kotlin
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
```

Add the two constructor params:
```kotlin
class HomeViewModel(
    private val locationsCoordinator: LocationsCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
```

Add a job field:
```kotlin
    private var settingsObserverJob: Job? = null
```

Add the observer in `init` (after the `dayChangedObserverJob` block):
```kotlin
        settingsObserverJob = viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.calculationMethod }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    _prayerDataByLocation.value = emptyMap()
                    loadPrayerTimesForCurrentLocation()
                }
        }
```

Add a helper to read the current method:
```kotlin
    private suspend fun currentCalculationMethod(): CalculationMethod =
        CalculationMethod.fromSettingsId(getSettingsUseCase().calculationMethod)
```

Update the three `prayerTimesLoader.load(...)` call sites to pass the method:
- Line 111 in `loadPrayerTimesForCurrentLocation`:
```kotlin
                    val result = prayerTimesLoader.load(location, currentCalculationMethod())
```
- Line 169 in `loadActiveLocation`:
```kotlin
        return prayerTimesLoader.load(entry.location, currentCalculationMethod())
```
- Line 189 in `preloadOtherLocations`:
```kotlin
                            prayerTimesLoader.load(entry.location, currentCalculationMethod())
```

Cancel the new job in `onCleared`:
```kotlin
        settingsObserverJob?.cancel()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt
git commit -m "feat: home reloads prayer times when calculation method changes"
```

---

## Phase 3: Monthly Prayer Times screen — read settings, observe changes, reload

### Task 3.1: Add `:prayer_settings` dependency to `prayer_feature:prayertimes`

**Files:**
- Modify: `prayer_feature/prayertimes/build.gradle.kts`

- [ ] **Step 1: Add the dependency**

In `prayer_feature/prayertimes/build.gradle.kts`, in the `dependencies` block, after `implementation(project(":prayer_location"))` (line 51), add:

```kotlin
    implementation(project(":prayer_settings"))
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/build.gradle.kts
git commit -m "build: add prayer_settings dependency to prayertimes feature"
```

### Task 3.2: `PrayerTimesViewModel` reads settings, observes changes, reloads

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

In `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`:

Add imports:
```kotlin
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
```

Add mock fields:
```kotlin
    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var settingsRepository: SettingsRepository
```

In `setUp()`, initialize and stub them:
```kotlin
        getSettingsUseCase = mockk()
        settingsRepository = mockk()
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        every { settingsRepository.observeSettings() } returns flowOf(Settings())
```

Update the constructor call (lines 101-109) to pass the new dependencies:
```kotlin
        viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            getMonthlyPrayerTimesUseCase,
            saveMonthlyPrayerTimesUseCase,
            activeLocationProvider,
            calculator,
            formatter,
            getSettingsUseCase,
            settingsRepository,
            UnconfinedTestDispatcher()
        )
```

Update every `getPrayerTimesUseCase.invoke(...)` stub (lines 89, 194, 224, 270, 300, 326, 356, 383, 414, 445) from 4 `any()` to 5 `any()`:
```kotlin
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns success(mockPrayerList)
```

Update the `getMonthlyPrayerTimesUseCase.invoke(...)` stubs (lines 90, 222, 246) from 4 `any()` to 5 `any()`:
```kotlin
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns null
```

Update the `saveMonthlyPrayerTimesUseCase.invoke(...)` stub (line 91) from 5 `any()` to 6 `any()`:
```kotlin
        coEvery { saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns Unit
```

Update the `coVerify` in `saves month to persistent cache after per-day computation` (lines 255-263) to include the method:
```kotlin
        coVerify(exactly = 1) {
            saveMonthlyPrayerTimesUseCase.invoke(
                currentMonth,
                mockLocation.latitude,
                mockLocation.longitude,
                zoneId,
                CalculationMethod.TURKEY_DIYANET,
                any()
            )
        }
```

Add the new test:
```kotlin
    @Test
    fun `calculation method change clears month cache and reloads`() = runTest {
        val settingsFlow = MutableStateFlow(Settings(calculationMethod = "TURKEY_DIYANET"))
        every { settingsRepository.observeSettings() } returns settingsFlow
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        val callsAfterInitial = callCount

        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "MWL")
        settingsFlow.value = Settings(calculationMethod = "MWL")

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(callCount).isGreaterThan(callsAfterInitial)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: FAIL — `PrayerTimesViewModel` constructor does not accept the new arguments (compile error).

- [ ] **Step 3: Update `PrayerTimesViewModel`**

In `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`:

Add imports:
```kotlin
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
```

Add the two constructor params (before `computationDispatcher`):
```kotlin
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getMonthlyPrayerTimesUseCase: GetMonthlyPrayerTimesUseCase,
        private val saveMonthlyPrayerTimesUseCase: SaveMonthlyPrayerTimesUseCase,
        private val activeLocationProvider: ActiveLocationProvider,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter,
        private val getSettingsUseCase: GetSettingsUseCase,
        private val settingsRepository: SettingsRepository,
        private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
```

Add a job field:
```kotlin
    private var settingsObserverJob: Job? = null
```

In `loadMonthlyPrayerTimes()`, add the settings observer after the location observer:
```kotlin
    fun loadMonthlyPrayerTimes() {
        if (locationObservationJob?.isActive == true) return
        locationObservationJob = viewModelScope.launch {
            activeLocationProvider.location
                .collect { location ->
                    if (location == null) {
                        _uiState.value = PrayerTimesUiState.Error("Failed to get active location.")
                    } else {
                        loadForLocation(location)
                    }
                }
        }
        settingsObserverJob = viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.calculationMethod }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    monthCache.clear()
                    val location = activeLocationProvider.location.value
                    if (location != null) {
                        loadForLocation(location)
                    }
                }
        }
    }
```

In `loadMonth`, read the method once and pass it through. Change the start of the `viewModelScope.launch` block (after the `resolvedZoneId` null-check, before `try`):
```kotlin
            val calculationMethod = CalculationMethod.fromSettingsId(getSettingsUseCase().calculationMethod)
            try {
                val locationCache = monthCache.getOrPut(locationId) { mutableMapOf() }
                val cached = locationCache[month]
                if (cached != null) {
                    emitSuccess(month, cached, locationId, location, resolvedZoneId)
                    return@launch
                }
                val persistedMonth = getMonthlyPrayerTimesUseCase(
                    month = month,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoneId = resolvedZoneId,
                    calculationMethod = calculationMethod
                )
                if (persistedMonth != null) {
                    val today = LocalDateTime.now(resolvedZoneId).date
                    val refreshed = refreshCurrentPrayerFlags(persistedMonth, today, resolvedZoneId)
                    locationCache[month] = refreshed
                    emitSuccess(month, refreshed, locationId, location, resolvedZoneId)
                    return@launch
                }
                val today = LocalDateTime.now(resolvedZoneId)
                val monthlyPrayers = try {
                    coroutineScope {
                        (1..month.numberOfDays).map { day ->
                            async(computationDispatcher) {
                                computeDailyPrayer(day, month, location, resolvedZoneId, today, calculationMethod)
                            }
                        }.awaitAll()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = PrayerTimesUiState.Error(
                        e.message ?: "Failed to load prayer times."
                    )
                    return@launch
                }
                locationCache[month] = monthlyPrayers
                saveMonthlyPrayerTimesUseCase(
                    month = month,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoneId = resolvedZoneId,
                    calculationMethod = calculationMethod,
                    prayers = monthlyPrayers
                )
                emitSuccess(month, monthlyPrayers, locationId, location, resolvedZoneId)
            } finally {
```

Change `computeDailyPrayer` to accept and pass the method:
```kotlin
    private suspend fun computeDailyPrayer(
        day: Int,
        month: YearMonth,
        location: LocationData,
        resolvedZoneId: ZoneId,
        today: LocalDateTime,
        calculationMethod: CalculationMethod
    ): DailyPrayer {
        val date = month.onDay(day)
        val prayerTimes = getPrayerTimesUseCase(
            date = date.atTime(0, 0),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = resolvedZoneId,
            calculationMethod = calculationMethod
        ).getOrElse { throw it }
        ...
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "feat: monthly view reloads prayer times when calculation method changes"
```

---

## Phase 4: Verification

### Task 4.1: Run the full test suite and build

**Files:** none (verification only)

- [ ] **Step 1: Run all unit tests**

Run:
```bash
./gradlew testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run the debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke test (device/emulator)**

1. Launch the app, note the Fajr time on Home.
2. Settings → Calculation Method → select "Muslim World League (MWL)".
3. Navigate back to Home. Verify the prayer times changed (Fajr differs from the Turkey method).
4. Open the monthly Prayer Times screen. Verify the month shows MWL-based times.
5. Change the method again and confirm both screens update.

- [ ] **Step 4: Run `gitnexus_detect_changes` before committing**

Run: `gitnexus_detect_changes({scope: "all", repo: "NamazVakitleri"})`
Expected: changed symbols are limited to the pipeline/ViewModel files touched in this plan; no unexpected execution flows affected.

- [ ] **Step 5: Commit any remaining verification fixes**

```bash
git add -A
git commit -m "chore: verify calculation method recalculation fix"
```

---

## Self-Review Notes

- **Spec coverage:** All three root causes are addressed: (1) settings observer in `HomeViewModel` (Task 2.2) and `PrayerTimesViewModel` (Task 3.2); (2) method threaded through the pipeline and used in `PrayerDataStoreImp` (Tasks 1.3/1.4); (3) method added to daily + monthly cache keys (Tasks 1.3/1.4).
- **Type consistency:** `CalculationMethod.fromSettingsId` (Task 1.1) is used by both ViewModels (Tasks 2.2/3.2). The `calculationMethod` parameter is consistently named and typed `CalculationMethod` across all layers. `saveMonthlyPrayerTimes` places `calculationMethod` before `prayers` everywhere.
- **adhan2 alignment:** JAFARI removed from the settings list (Task 1.0) and the `prayer/model` enum (Task 1.1) because adhan2 0.0.6 has no JAFARI. The remaining 7 app methods each map to a real adhan2 method (Task 1.2). No approximation code remains.
