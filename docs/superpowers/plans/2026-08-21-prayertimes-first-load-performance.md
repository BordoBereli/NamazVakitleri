# PrayerTimes First-Load Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the cold first load of the PrayerTimes tab fast: paint before persisting, one disk write per month load, progressive rendering with today first, and no error flash while location resolves.

**Architecture:** Adds a `persistDailyCache` flag through the prayer-times read chain so bulk month loads skip per-day DataStore writes; moves month persistence off the critical path onto an injected app-scoped coroutine scope; streams partial `Success` emissions (today computed first) from `PrayerTimesViewModel`; replaces the immediate null-location `Error` with a cancellable grace timeout.

**Tech Stack:** Kotlin 2.2, Coroutines (Mutex/async/StateFlow), Jetpack Compose (no UI changes), Koin annotations, JUnit 5 + MockK + Turbine + Truth.

**Spec:** `docs/superpowers/specs/2026-08-21-prayertimes-first-load-performance-design.md`

---

### Task 1: `persistDailyCache` flag through the data layer

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt:19-25`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt:17-29`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt:22-28`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt:35-62`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these two tests to `PrayerDataStoreImpTest.kt` (after `getPrayerTimes calculates and caches when cache miss`):

```kotlin
@Test
fun `getPrayerTimes in bulk mode calculates without caching or pre-caching`() = runTest {
    val testDate = LocalDateTime.createBy(2024, 1, 1)
    val zoneId = ZoneId.of("Europe/Istanbul")
    val calculatedPrayers = listOf(
        Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
    )
    coEvery { prayerTimesCache.get(any()) } returns null
    coEvery {
        prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
    } returns calculatedPrayers

    val result = dataStore.getPrayerTimes(
        testDate, 41.0, 29.0, zoneId,
        CalculationMethod.TURKEY_DIYANET, persistDailyCache = false
    )

    assertThat(result).isEqualTo(calculatedPrayers)
    coVerify(exactly = 1) {
        prayerCalculationService.calculateDailyPrayerTimes(
            41.0, 29.0, zoneId, testDate,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
        )
    }
    coVerify(exactly = 0) { prayerTimesCache.put(any(), any()) }
}

@Test
fun `getPrayerTimes in bulk mode skips pre-caching on cache hit`() = runTest {
    val testDate = LocalDateTime.createBy(2024, 1, 1)
    val zoneId = ZoneId.of("Europe/Istanbul")
    val cachedToday = listOf(
        Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
    )
    coEvery {
        prayerTimesCache.get("2024-01-01|41.0|29.0|Europe/Istanbul|TURKEY_DIYANET")
    } returns cachedToday

    val result = dataStore.getPrayerTimes(
        testDate, 41.0, 29.0, zoneId,
        CalculationMethod.TURKEY_DIYANET, persistDailyCache = false
    )

    assertThat(result).isEqualTo(cachedToday)
    coVerify(exactly = 0) { prayerTimesCache.put(any(), any()) }
    coVerify(exactly = 0) {
        prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*PrayerDataStoreImpTest*"`
Expected: COMPILE ERROR — `Too many arguments` / unresolved `persistDailyCache`.

- [ ] **Step 3: Add the flag to the interfaces**

`IPrayerRepository.getPrayerTimes` becomes:

```kotlin
suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        persistDailyCache: Boolean = true,
): List<Prayer>
```

`PrayerDataStore.getPrayerTimes` gains the identical trailing parameter
(`persistDailyCache: Boolean = true`).

- [ ] **Step 4: Pass through `PrayerRepository`**

```kotlin
override suspend fun getPrayerTimes(
    date: LocalDateTime,
    latitude: Double,
    longitude: Double,
    zoneId: ZoneId,
    calculationMethod: CalculationMethod,
    persistDailyCache: Boolean,
): List<Prayer> = prayerDataStore.getPrayerTimes(
    date = date,
    latitude = latitude,
    longitude = longitude,
    zoneId = zoneId,
    calculationMethod = calculationMethod,
    persistDailyCache = persistDailyCache
)
```

- [ ] **Step 5: Honor the flag in `PrayerDataStoreImp`**

```kotlin
override suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        persistDailyCache: Boolean,
): List<Prayer> {
    val cacheKey = buildCacheKey(date, latitude, longitude, zoneId, calculationMethod)
    val cached = prayerTimesCache.get(cacheKey)
    if (cached != null) {
        if (persistDailyCache) preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod)
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
    if (persistDailyCache) {
        prayerTimesCache.put(cacheKey, calculated)
        preCacheTomorrow(date, latitude, longitude, zoneId, calculationMethod)
    }
    return calculated
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*PrayerDataStoreImpTest*"`
Expected: PASS (all pre-existing tests included).

- [ ] **Step 7: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IPrayerRepository.kt \
  prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt \
  prayer/data/src/main/java/com/kutluoglu/prayer/data/repository/prayer/PrayerDataStore.kt \
  prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt \
  prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt
git commit -m "feat(prayer): add persistDailyCache flag to daily read path"
```

---

### Task 2: Forward the flag through `GetPrayerTimesUseCase`

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetPrayerTimesUseCase.kt`
- Test: `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/GetPrayerTimesUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `GetPrayerTimesUseCaseTest.kt` (add import `com.kutluoglu.prayer.model.prayer.CalculationMethod`):

```kotlin
@Test
fun `invoke forwards persistDailyCache=false to repository`() = runTest {
    coEvery {
        prayerRepository.getPrayerTimes(any(), any(), any(), zoneId, any(), any())
    } returns emptyList()

    useCase(testDate, 41.0, 29.0, zoneId, persistDailyCache = false)

    coVerify(exactly = 1) {
        prayerRepository.getPrayerTimes(
            testDate, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, false
        )
    }
}

@Test
fun `invoke defaults persistDailyCache=true to repository`() = runTest {
    coEvery {
        prayerRepository.getPrayerTimes(any(), any(), any(), zoneId, any(), any())
    } returns emptyList()

    useCase(testDate, 41.0, 29.0, zoneId)

    coVerify(exactly = 1) {
        prayerRepository.getPrayerTimes(
            testDate, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, true
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*GetPrayerTimesUseCaseTest*"`
Expected: FAIL/COMPILE ERROR — no `persistDailyCache` parameter.

- [ ] **Step 3: Implement**

```kotlin
suspend operator fun invoke(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
        persistDailyCache: Boolean = true,
): Result<List<Prayer>> {
    return try {
        val prayerTimes = prayerRepository.getPrayerTimes(
            date, latitude, longitude, zoneId, calculationMethod, persistDailyCache
        )
        Result.success(prayerTimes)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer:domain:testDebugUnitTest --tests="*GetPrayerTimesUseCaseTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/GetPrayerTimesUseCase.kt \
  prayer/domain/src/test/java/com/kutluoglu/prayer/domain/GetPrayerTimesUseCaseTest.kt
git commit -m "feat(domain): forward persistDailyCache through GetPrayerTimesUseCase"
```

---

### Task 3: Provide the app-scoped save scope (DI)

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/di/PrayerFeaturePrayerTimesModule.kt`

- [ ] **Step 1: Add the named scope provider**

```kotlin
package com.kutluoglu.prayer_feature.prayertimes.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 24.12.2025.
 */

@Module
@ComponentScan("com.kutluoglu.prayer_feature.prayertimes**")
@Configuration
object PrayerFeaturePrayerTimesModule {
    @Single
    @Named("prayerSaveScope")
    fun providePrayerSaveScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

Named to avoid ambiguity with the other `CoroutineScope` singles
(`providePreCacheScope`, `provideLocationRefreshScope`) during injection.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/di/PrayerFeaturePrayerTimesModule.kt
git commit -m "feat(prayertimes): provide app-scoped prayerSaveScope for background saves"
```

---

### Task 4: Bulk mode wired into `PrayerTimesViewModel`

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt:279-285`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt:106`

- [ ] **Step 1: Update the mock stub and write the failing test**

In `PrayerTimesViewModelTest.setUp()` change the stub to six matchers:

```kotlin
coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns success(mockPrayerList)
```

Add the test (imports already present except none new required):

```kotlin
@Test
fun `month load requests daily times without persisting per-day cache`() = runTest {
    viewModel.loadMonthlyPrayerTimes()

    coVerify(atLeast = currentMonthDays()) {
        getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), false)
    }
}

private fun currentMonthDays(): Int {
    val zoneId = getZoneIdFromLocation("TR")
    return LocalDateTime.now(zoneId).date.yearMonth.numberOfDays
}
```

Note: `numberOfDays` extension on `YearMonth` is already imported in the test file
(`kotlinx.datetime.yearMonth` + member). If `numberOfDays` is not resolvable on `YearMonth`
directly, use `LocalDateTime.now(zoneId).date.yearMonth.numberOfDays` exactly as the existing
test does at line 153 — it compiles there today.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: FAIL — MockK records the production call with `persistDailyCache = true` (default),
so `false` matcher finds no answer / verification fails.

- [ ] **Step 3: Wire the flag in `computeDailyPrayer`**

```kotlin
val prayerTimes = getPrayerTimesUseCase(
    date = date.atTime(0, 0),
    latitude = location.latitude,
    longitude = location.longitude,
    zoneId = resolvedZoneId,
    calculationMethod = calculationMethod,
    persistDailyCache = false
).getOrElse { throw it }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt \
  prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "feat(prayertimes): suppress per-day cache writes during bulk month loads"
```

---

### Task 5: Emit success before persisting (background save)

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt:51-63,248-257`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add imports: `kotlinx.coroutines.CoroutineScope`, `kotlinx.coroutines.SupervisorJob`,
`kotlinx.coroutines.test.advanceUntilIdle`,
`com.kutluoglu.core.common.analytics.AnalyticsEvents`,
`com.kutluoglu.core.common.analytics.AnalyticsParams`.
Add field + wire into `setUp()`:

```kotlin
private val backgroundSaveScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
```

Constructor call gains the scope before `computationDispatcher`:

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
    analyticsTracker,
    backgroundSaveScope,
    UnconfinedTestDispatcher()
)
```

Tests:

```kotlin
@Test
fun `monthly save runs in background after success is emitted`() = runTest {
    val saveGate = CompletableDeferred<Unit>()
    coEvery {
        saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
    } coAnswers { saveGate.await(); Unit }

    viewModel.loadMonthlyPrayerTimes()

    viewModel.uiState.test {
        var state = withTimeout(5_000) { awaitItem() }
        while (state is PrayerTimesUiState.Loading) {
            state = withTimeout(5_000) { awaitItem() }
        }
        assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)

        assertThat(saveGate.isCompleted).isFalse()
        cancelAndIgnoreRemainingEvents()
    }

    saveGate.complete(Unit)
    advanceUntilIdle()
    coVerify(exactly = 1) {
        saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
    }
}

@Test
fun `failing monthly save keeps success state and logs analytics`() = runTest {
    coEvery {
        saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
    } throws RuntimeException("disk full")

    viewModel.loadMonthlyPrayerTimes()

    viewModel.uiState.test {
        var state = withTimeout(5_000) { awaitItem() }
        while (state is PrayerTimesUiState.Loading) {
            state = withTimeout(5_000) { awaitItem() }
        }
        assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
        cancelAndIgnoreRemainingEvents()
    }
    advanceUntilIdle()
    coVerify(exactly = 1) {
        analyticsTracker.logEvent(
            AnalyticsEvents.PRAYER_TIMES_ERROR,
            mapOf(AnalyticsParams.REASON to "monthly_save_failed")
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: COMPILE ERROR — no `backgroundSaveScope` constructor parameter.

- [ ] **Step 3: Implement**

Constructor gains (after `analyticsTracker`, keeping `computationDispatcher` last):

```kotlin
@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getMonthlyPrayerTimesUseCase: GetMonthlyPrayerTimesUseCase,
        private val saveMonthlyPrayerTimesUseCase: SaveMonthlyPrayerTimesUseCase,
        private val activeLocationProvider: ActiveLocationProvider,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter,
        private val getSettingsUseCase: GetSettingsUseCase,
        private val settingsRepository: SettingsRepository,
        private val analyticsTracker: AnalyticsTracker,
        @Named("prayerSaveScope") private val backgroundSaveScope: CoroutineScope,
        private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
```

New import: `org.koin.core.annotation.Named`.

Cold path tail of `loadMonth` becomes (replacing lines that awaited the save before
`emitSuccess`):

```kotlin
                locationCache[month] = monthlyPrayers
                emitSuccess(month, monthlyPrayers, locationId, location, resolvedZoneId, hijriAdjustment)
                backgroundSaveScope.launch {
                    runCatching {
                        saveMonthlyPrayerTimesUseCase(
                            month = month,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            zoneId = resolvedZoneId,
                            calculationMethod = calculationMethod,
                            prayers = monthlyPrayers
                        )
                    }.onFailure {
                        analyticsTracker.logEvent(
                            AnalyticsEvents.PRAYER_TIMES_ERROR,
                            mapOf(AnalyticsParams.REASON to "monthly_save_failed")
                        )
                    }
                }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt \
  prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "feat(prayertimes): emit success before backgrounding monthly persistence"
```

---

### Task 6: Graceful null-location handling (no error flash)

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt:81-110`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

The timeout must be injectable because `MainCoroutineRule` uses an isolated
`UnconfinedTestDispatcher` scheduler that `runTest` cannot advance. Constructor gains
`locationResolutionTimeoutMs: Long = LOCATION_RESOLUTION_TIMEOUT_MS`. In tests construct the
VM with a small real-time timeout.

```kotlin
@Test
fun `null location stays Loading and errors only after timeout`() = runTest {
    activeLocationProvider.set(null)
    buildViewModel(locationResolutionTimeoutMs = 100)

    viewModel.loadMonthlyPrayerTimes()

    viewModel.uiState.test {
        assertThat(awaitItem()).isEqualTo(PrayerTimesUiState.Loading)
        val state = withTimeout(5_000) { awaitItem() }
        assertThat(state).isInstanceOf(PrayerTimesUiState.Error::class.java)
        cancelAndIgnoreRemainingEvents()
    }
    coVerify(exactly = 1) {
        analyticsTracker.logEvent(
            AnalyticsEvents.PRAYER_TIMES_ERROR,
            mapOf(AnalyticsParams.REASON to "no_active_location")
        )
    }
}

@Test
fun `location arriving before timeout cancels the pending error`() = runTest {
    activeLocationProvider.set(null)
    buildViewModel(locationResolutionTimeoutMs = 10_000)

    viewModel.loadMonthlyPrayerTimes()
    activeLocationProvider.set(mockLocation)

    viewModel.uiState.test {
        var state = withTimeout(5_000) { awaitItem() }
        while (state is PrayerTimesUiState.Loading) {
            state = withTimeout(5_000) { awaitItem() }
        }
        assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
        delay(200)
        assertThat(viewModel.uiState.value).isInstanceOf(PrayerTimesUiState.Success::class.java)
        cancelAndIgnoreRemainingEvents()
    }
    coVerify(exactly = 0) {
        analyticsTracker.logEvent(
            AnalyticsEvents.PRAYER_TIMES_ERROR,
            mapOf(AnalyticsParams.REASON to "no_active_location")
        )
    }
}
```

Extract the `setUp()` construction into a helper so tests can override the timeout:

```kotlin
private fun buildViewModel(locationResolutionTimeoutMs: Long = 15_000L) {
    viewModel = PrayerTimesViewModel(
        getPrayerTimesUseCase,
        getMonthlyPrayerTimesUseCase,
        saveMonthlyPrayerTimesUseCase,
        activeLocationProvider,
        calculator,
        formatter,
        getSettingsUseCase,
        settingsRepository,
        analyticsTracker,
        backgroundSaveScope,
        UnconfinedTestDispatcher(),
        locationResolutionTimeoutMs
    )
}
```

`setUp()` calls `buildViewModel()` instead of constructing inline.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: COMPILE ERROR — no timeout parameter / behavior mismatch.

- [ ] **Step 3: Implement**

Add constant + field near the other fields:

```kotlin
private var locationTimeoutJob: Job? = null
```

Companion-less top of class or bottom:

```kotlin
private companion object {
    const val LOCATION_RESOLUTION_TIMEOUT_MS = 15_000L
}
```

Constructor gains trailing parameter (before the closing brace, after
`computationDispatcher` — keep it last so the default doesn't disturb Koin):

```kotlin
        private val locationResolutionTimeoutMs: Long = LOCATION_RESOLUTION_TIMEOUT_MS
```

Wait — companion constants are not visible as constructor defaults. Instead declare a
top-level private const above the class:

```kotlin
private const val LOCATION_RESOLUTION_TIMEOUT_MS = 15_000L
```

and use it as the constructor default. Remove the companion.

Rewrite the collector in `loadMonthlyPrayerTimes()`:

```kotlin
        locationObservationJob = viewModelScope.launch {
            activeLocationProvider.location
                .collect { location ->
                    if (location == null) {
                        _uiState.value = PrayerTimesUiState.Loading
                        locationTimeoutJob?.cancel()
                        locationTimeoutJob = viewModelScope.launch {
                            delay(locationResolutionTimeoutMs)
                            _uiState.value =
                                PrayerTimesUiState.Error("Failed to get active location.")
                            analyticsTracker.logEvent(
                                AnalyticsEvents.PRAYER_TIMES_ERROR,
                                mapOf(AnalyticsParams.REASON to "no_active_location")
                            )
                        }
                    } else {
                        locationTimeoutJob?.cancel()
                        loadForLocation(location)
                    }
                }
        }
```

`delay` import (`kotlinx.coroutines.delay`) is added.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt \
  prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "fix(prayertimes): replace instant null-location error with cancellable timeout"
```

---

### Task 7: Progressive streaming emission (today first)

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt:226-257,310-331`
- Test: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `cold load streams today first then completes full sorted month`() = runTest {
    val zoneId = getZoneIdFromLocation("TR")
    val today = LocalDateTime.now(zoneId).date.dayOfMonth
    val days = currentMonthDays()
    val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()
    coEvery {
        getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
    } coAnswers {
        val date = firstArg<LocalDateTime>()
        gates.getOrPut(date.date.dayOfMonth) { CompletableDeferred() }.await()
        success(mockPrayerList)
    }

    viewModel.loadMonthlyPrayerTimes()

    viewModel.uiState.test {
        var state = withTimeout(5_000) { awaitItem() }
        while (state !is PrayerTimesUiState.Success) {
            state = withTimeout(5_000) { awaitItem() }
        }
        assertThat(state.monthlyPrayers.map { it.dayOfMonth }).containsExactly(today)

        gates.values.forEach { it.complete(Unit) }

        var final = withTimeout(5_000) { awaitItem() }
        while ((final as? PrayerTimesUiState.Success)?.monthlyPrayers?.size != days) {
            final = withTimeout(5_000) { awaitItem() }
        }
        assertThat(final.monthlyPrayers.map { it.dayOfMonth })
            .isEqualTo((1..days).toList())
        cancelAndIgnoreRemainingEvents()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest*"`
Expected: FAIL — current implementation emits once with the full month, so the first
`Success` contains every day, not `[today]`.

- [ ] **Step 3: Implement streaming in the cold path**

Replace the cold-path compute + emit block of `loadMonth` (the `coroutineScope { ... }` and
everything up to the background save) with:

```kotlin
                val payload = buildPayload(locationId, location, resolvedZoneId, hijriAdjustment)
                val monthlyPrayers = try {
                    coroutineScope {
                        val results = arrayOfNulls<DailyPrayer>(month.numberOfDays)
                        val mutex = Mutex()
                        val todayDayOfMonth = today.dayOfMonth
                        val orderedDays = listOf(todayDayOfMonth) +
                            (1..month.numberOfDays).filter { it != todayDayOfMonth }
                        orderedDays.map { day ->
                            async(computationDispatcher) {
                                val computed = computeDailyPrayer(
                                    day, month, location, resolvedZoneId, today, calculationMethod, hijriAdjustment
                                )
                                mutex.withLock {
                                    results[day - 1] = computed
                                    emitPartial(month, results.filterNotNull().sortedBy { it.dayOfMonth }, payload)
                                }
                            }
                        }.awaitAll()
                        results.filterNotNull().sortedBy { it.dayOfMonth }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = PrayerTimesUiState.Error(
                        e.message ?: "Failed to load prayer times."
                    )
                    analyticsTracker.logEvent(
                        AnalyticsEvents.PRAYER_TIMES_ERROR,
                        mapOf(AnalyticsParams.REASON to (e.message ?: "unknown"))
                    )
                    return@launch
                }
                locationCache[month] = monthlyPrayers
                emitPartial(month, monthlyPrayers, payload)
                backgroundSaveScope.launch {
                    runCatching {
                        saveMonthlyPrayerTimesUseCase(
                            month = month,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            zoneId = resolvedZoneId,
                            calculationMethod = calculationMethod,
                            prayers = monthlyPrayers
                        )
                    }.onFailure {
                        analyticsTracker.logEvent(
                            AnalyticsEvents.PRAYER_TIMES_ERROR,
                            mapOf(AnalyticsParams.REASON to "monthly_save_failed")
                        )
                    }
                }
```

Refactor `emitSuccess` into a reusable payload builder + emitters:

```kotlin
    private data class SuccessPayload(
        val locationId: String,
        val timeState: TimeUiState,
        val locationState: LocationUiState
    )

    private suspend fun buildPayload(
        locationId: String,
        location: LocationData,
        resolvedZoneId: ZoneId,
        hijriAdjustment: Int
    ): SuccessPayload = SuccessPayload(
        locationId = locationId,
        timeState = formatter.getInitialTimeInfo(resolvedZoneId, hijriAdjustment = hijriAdjustment),
        locationState = LocationUiState(
            locationData = location,
            locationInfoText = formatter.locationInfo(location)
        )
    )

    private fun emitPartial(
        month: YearMonth,
        monthlyPrayers: List<DailyPrayer>,
        payload: SuccessPayload
    ) {
        if (month != selectedMonth() || payload.locationId != activeLocationId) return
        val today = LocalDateTime.now(zoneId ?: ZoneId.systemDefault())
        _uiState.value = PrayerTimesUiState.Success(
            monthlyPrayers = monthlyPrayers,
            currentDayOfMonth = today.day,
            selectedMonth = month,
            isCurrentMonth = month == today.date.yearMonth,
            timeState = payload.timeState,
            locationState = payload.locationState
        )
    }
```

Delete the now-unused `emitSuccess`; the persisted/cached branches switch to
`buildPayload(...)` + `emitPartial(month, adjusted-or-cached, payload)`.

New imports: `kotlinx.coroutines.sync.Mutex`, `kotlinx.coroutines.sync.withLock`,
`com.kutluoglu.prayer_feature.common.states.TimeUiState`.

- [ ] **Step 4: Run the whole feature test suite**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest`
Expected: PASS — including the original `initial load computes the current month` (final
emission is conflated; turbine sees the full month).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt \
  prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git commit -m "feat(prayertimes): stream month results progressively with today first"
```

---

### Task 8: Full verification

- [ ] **Step 1: Run the complete unit-test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all modules green.

- [ ] **Step 2: Assemble the app**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (validates Koin graph compilation).

- [ ] **Step 3: Review changed scope**

Run: `git diff --stat` and inspect all touched symbols.
Expected: only the files listed across Tasks 1-7.
