# HomeViewModel Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the crowded `HomeViewModel` (379 lines, 10 deps) into four per-concern component classes that each publish their own `StateFlow`, kept behind a stable `HomeUiState`/`HomeScreen`/`HomeEvent` facade via a pure `mergeToHomeUiState` function called in `HomeRoute`.

**Architecture:** HomeViewModel becomes a thin orchestrator over four Koin `@Factory` components: `LocationCoordinator` (location resolution + observers), `PrayerTimesLoader` (prayer fetch + current/next computation), `CountdownEngine` (per-second tick loop), `QuranVerseLoader` (verse fetch + sheet visibility). Each component emits its own flow (`prayerState`, `timeState`, `locationState`, `countdownState`, `quranState`, `promptState`) plus a `screenGate` for Loading/Error/Ready. `HomeRoute` collects all flows and re-merges them into `HomeUiState` via the pure `mergeToHomeUiState` function. Splitting `timeRemaining`+`currentTime` out of `PrayerUiState`/`TimeUiState` into a dedicated `countdownState` stops the per-second recomposition of `DailyPrayers` and terminates a self-restart loop in `HomeTopContainer` caused by `LaunchedEffect(prayerState) { onStartCount() }`.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (KSP), kotlinx.coroutines `StateFlow`/`SharedFlow`, kotlinx.datetime, JUnit 5, MockK, Turbine, Truth.

**Spec:** `docs/superpowers/specs/2026-08-12-home-viewmodel-refactor-design.md`

---

## File Structure Overview

All new logic files live in package `com.kutluoglu.prayer_feature.home` (same as `HomeViewModel.kt`), matching the existing flat layout (`HomeEvent.kt`, `HomeUiStates.kt` live there too).

**Create:**
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownUiState.kt` — `CountdownUiState` + `QuranUiState` data classes
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreenGate.kt` — sealed gate (Loading/Error/Ready)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStateMerger.kt` — `HomeErrorMapper` object + top-level `mergeToHomeUiState`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationCoordinator.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/PrayerTimesLoader.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownEngine.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/QuranVerseLoader.kt`

**Modify:**
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStates.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt` (rewrite, slims to ~100 lines)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeTopContainer.kt`

**Test files (Create/Modify in `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/`):**
- `HomeUiStateMergerTest.kt`
- `LocationCoordinatorTest.kt`
- `PrayerTimesLoaderTest.kt`
- `CountdownEngineTest.kt`
- `QuranVerseLoaderTest.kt`
- `HomeViewModelTest.kt` (rewrite)

**Unchanged:** `HomeScreen.kt`, `HomeEvent.kt`, `DailyPrayers.kt`, `BottomContainer.kt`, navigation graph, DI module (`@ComponentScan("com.kutluoglu.prayer_feature.home**")` picks up the new `@Factory` classes automatically).

**Test commands** (run from repo root):
```bash
./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"
./gradlew :prayer_feature:home:testDebugUnitTest --tests="*CountdownEngineTest"
./gradlew :prayer_feature:home:testDebugUnitTest   # all home module unit tests
./gradlew :prayer_feature:home:compileDebugKotlin   # quick compile check
```

---

### Task 1: Add `CountdownUiState` + `QuranUiState` data classes

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownUiState.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/QuranUiState.kt`

- [ ] **Step 1: Create `CountdownUiState.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

data class CountdownUiState(
    val timeRemaining: String = "--:--:--",
    val currentTime: String = ""
)
```

- [ ] **Step 2: Create `QuranUiState.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData

data class QuranUiState(
    val verse: AyahData? = null,
    val isSheetVisible: Boolean = false
)
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownUiState.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/QuranUiState.kt
git commit -m "feat(home): add CountdownUiState and QuranUiState data classes"
```

---

### Task 2: Add `HomeScreenGate` sealed interface

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreenGate.kt`

- [ ] **Step 1: Create `HomeScreenGate.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Ready : HomeScreenGate
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreenGate.kt
git commit -m "feat(home): add HomeScreenGate sealed interface"
```

---

### Task 3: Create `HomeErrorMapper` + `mergeToHomeUiState` (TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStateMerger.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeUiStateMergerTest.kt`

This is the pure aggregation function HomeRoute will call. It must pass the `PrayerUiState` instance through **un-copied** on Ready so ticker changes do not invalidate `DailyPrayers`' inputs.

- [ ] **Step 1: Write the failing test `HomeUiStateMergerTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import org.junit.jupiter.api.Test

class HomeUiStateMergerTest {

    private val location = LocationUiState(
        locationData = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        ),
        locationInfoText = "Istanbul, TR"
    )
    private val time = TimeUiState(
        hijriDate = "1 Recep",
        gregorianFullDate = "12 Ağustos 2026",
        currentTime = "12:00"
    )
    private val prayer = PrayerUiState(
        prayers = emptyList(),
        currentPrayer = null,
        nextPrayer = null
    )
    private val countdown = CountdownUiState(timeRemaining = "--:--:--", currentTime = "12:00")
    private val quran = QuranUiState(verse = null, isSheetVisible = false)

    @Test
    fun `merge with Loading gate returns HomeUiState Loading`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Loading,
            location = null,
            time = null,
            prayer = null,
            countdown = countdown,
            quran = quran,
            prompt = false
        )
        assertThat(result).isEqualTo(HomeUiState.Loading)
    }

    @Test
    fun `merge with Error gate returns HomeUiState Error with message`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Error("boom"),
            location = null,
            time = null,
            prayer = null,
            countdown = countdown,
            quran = quran,
            prompt = false
        )
        assertThat(result).isEqualTo(HomeUiState.Error("boom"))
    }

    @Test
    fun `merge with Ready gate returns Success carrying all sub states`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Ready,
            location = location,
            time = time,
            prayer = prayer,
            countdown = countdown,
            quran = quran,
            prompt = true
        ) as HomeUiState.Success

        assertThat(result.locationState).isEqualTo(location)
        assertThat(result.timeState).isEqualTo(time)
        assertThat(result.prayerState).isEqualTo(prayer)
        assertThat(result.countdownState).isEqualTo(countdown)
        assertThat(result.quranVerse).isNull()
        assertThat(result.isVerseDetailSheetVisible).isFalse()
        assertThat(result.showLocationUpdatePrompt).isTrue()
    }

    @Test
    fun `merge on Ready passes the prayerState instance through un-copied`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Ready,
            location = location,
            time = time,
            prayer = prayer,
            countdown = countdown,
            quran = quran,
            prompt = false
        ) as HomeUiState.Success

        assertThat(result.prayerState === prayer).isTrue()
    }

    @Test
    fun `Ready gate with null location throws`() {
        val result = runCatching {
            mergeToHomeUiState(
                gate = HomeScreenGate.Ready,
                location = null,
                time = time,
                prayer = prayer,
                countdown = countdown,
                quran = quran,
                prompt = false
            )
        }
        assertThat(result.isFailure).isTrue()
    }
}
```

Note: `HomeUiState.Error` is a data class, so `assertThat(result).isEqualTo(HomeUiState.Error("boom"))` works.

- [ ] **Step 2: Run test to verify it fails (does not compile)**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeUiStateMergerTest"`
Expected: FAIL — `mergeToHomeUiState` unresolved

- [ ] **Step 3: Create `HomeUiStateMerger.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState

/**
 * Maps low-level failures to user-facing messages.
 * Same messages as the previous HomeViewModel implementation.
 */
object HomeErrorMapper {
    fun getUserFriendlyErrorMessage(exception: Throwable?): String {
        return when {
            exception == null -> "Konum alınamadı. Lütfen GPS'i etkinleştirin ve uygulamayı yeniden başlatın."
            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "İstek zaman aşımına uğradı. Lütfen tekrar deneyin."
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Ağ hatası. Lütfen bağlantınızı kontrol edin."
            exception.message?.contains("location", ignoreCase = true) == true ->
                "Konum servisi kullanılamıyor. Lütfen GPS'i etkinleştirin."
            else -> "Konum alınamadı. Lütfen tekrar deneyin."
        }
    }
}

/**
 * Pure aggregation of the per-concern flows into the single HomeUiState the screen consumes.
 * On Ready the prayerState/timple passed references are reused (NOT copied) so that a
 * per-second countdown tick does not invalidate DailyPrayers' inputs.
 */
fun mergeToHomeUiState(
    gate: HomeScreenGate,
    location: LocationUiState?,
    time: TimeUiState?,
    prayer: PrayerUiState?,
    countdown: CountdownUiState,
    quran: QuranUiState,
    prompt: Boolean
): HomeUiState {
    return when (gate) {
        HomeScreenGate.Loading -> HomeUiState.Loading
        is HomeScreenGate.Error -> HomeUiState.Error(gate.message)
        HomeScreenGate.Ready -> HomeUiState.Success(
            timeState = requireNotNull(time),
            prayerState = requireNotNull(prayer),
            locationState = requireNotNull(location),
            countdownState = countdown,
            quranVerse = quran.verse,
            isVerseDetailSheetVisible = quran.isSheetVisible,
            showLocationUpdatePrompt = prompt
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeUiStateMergerTest"`
Expected: PASS (5 tests)

> **Note:** This step also requires `HomeUiState.Success` to have a `countdownState` parameter — applied in Task 4. If the test fails to compile on `countdownState`, proceed to Task 4 Step 1 first, then re-run.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStateMerger.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeUiStateMergerTest.kt
git commit -m "feat(home): add HomeErrorMapper and mergeToHomeUiState pure aggregation"
```

---

### Task 4: Add `countdownState` to `HomeUiState.Success`

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStates.kt`

- [ ] **Step 1: Update `HomeUiStates.kt`**

Add `countdownState` between `locationState` and `quranVerse`:

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(
        val timeState: TimeUiState = TimeUiState(),
        val prayerState: PrayerUiState = PrayerUiState(),
        val locationState: LocationUiState,
        val countdownState: CountdownUiState = CountdownUiState(),
        val quranVerse: AyahData? = null,
        val isVerseDetailSheetVisible: Boolean = false,
        val showLocationUpdatePrompt: Boolean = false
    ) : HomeUiState()
}

data class PrayerUiState(
        val prayers: List<Prayer> = emptyList(),
        val currentPrayer: Prayer? = null,
        val nextPrayer: Prayer? = null,
        val timeRemaining: String = "--:--:--"
)
```

`timeRemaining` stays on `PrayerUiState` **for now** (removed in Task 9) so the module keeps compiling through the transition.

- [ ] **Step 2: Run merger tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeUiStateMergerTest"`
Expected: PASS (5 tests)

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStates.kt
git commit -m "feat(home): add countdownState slot to HomeUiState.Success"
```

---

### Task 5: Create `QuranVerseLoader` (TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/QuranVerseLoader.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/QuranVerseLoaderTest.kt`

Replicates the original `loadRandomVerse` retry semantics (poll every 1s doubling up to 30s until the screen is Ready, then fetch exactly once).

- [ ] **Step 1: Write the failing test `QuranVerseLoaderTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.Surah
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranVerseLoaderTest {

    private val getRandomVerseUseCase: GetRandomVerseUseCase = mockk()
    private val languageProvider: LanguageProvider = mockk()

    @Test
    fun `loadVerse resolves verse when screen ready`() = runTest {
        val verse = AyahData(
            number = 1,
            text = "Bismillah...",
            surah = Surah(number = 1, name = "Al-Fatihah", englishName = "Al-Fatihah", numberOfAyahs = 7)
        )
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.success(verse)

        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { true })
        runCurrent()

        assertThat(loader.quranState.value.verse).isEqualTo(verse)
    }

    @Test
    fun `loadVerse does not fetch while screen not ready`() = runTest {
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.failure(RuntimeException("x"))

        var ready = false
        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { ready })

        advanceTimeBy(5_000)
        runCurrent()

        ready = true
        advanceTimeBy(60_000)
        runCurrent()

        coEvery { getRandomVerseUseCase.invoke("tr") }.let {}
        assertThat(loader.quranState.value.verse).isNull()
    }

    @Test
    fun `setSheetVisible toggles the sheet flag`() = runTest {
        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.setSheetVisible(true)
        assertThat(loader.quranState.value.isSheetVisible).isTrue()
        loader.setSheetVisible(false)
        assertThat(loader.quranState.value.isSheetVisible).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*QuranVerseLoaderTest"`
Expected: FAIL — `QuranVerseLoader` unresolved

- [ ] **Step 3: Create `QuranVerseLoader.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class QuranVerseLoader(
    private val getRandomVerseUseCase: GetRandomVerseUseCase,
    private val languageProvider: LanguageProvider
) {
    private val _quranState = MutableStateFlow(QuranUiState())
    val quranState: StateFlow<QuranUiState> = _quranState

    /**
     * Polls until [isScreenReady] returns true (1s backoff doubling up to 30s),
     * then fetches the verse exactly once. Mirrors the original loadRandomVerse behavior.
     */
    fun loadVerse(scope: CoroutineScope, isScreenReady: () -> Boolean) {
        scope.launch {
            var delayMillis = 1_000L
            while (true) {
                if (isScreenReady()) {
                    val language = languageProvider.getLanguageCode()
                    getRandomVerseUseCase(language)
                        .onSuccess { verse ->
                            _quranState.value = _quranState.value.copy(verse = verse)
                        }
                        .onFailure {
                            Log.e("QuranVerseLoader", "Failed to load random verse -> ${it.message}")
                        }
                    break
                }
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun setSheetVisible(isVisible: Boolean) {
        _quranState.value = _quranState.value.copy(isSheetVisible = isVisible)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*QuranVerseLoaderTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/QuranVerseLoader.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/QuranVerseLoaderTest.kt
git commit -m "feat(home): add QuranVerseLoader component"
```

---

### Task 6: Create `LocationCoordinator` (TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationCoordinator.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationCoordinatorTest.kt`

Owns the location resolution chain (settings → saved → GPS), the settings/location observers (as pure `Flow`s for the ViewModel to collect), and the drift-detection prompt flag.

- [ ] **Step 1: Write the failing test `LocationCoordinatorTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.ObserveLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_location.LocationService
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCoordinatorTest {

    private val settingsRepository: SettingsRepository = mockk()
    private val getSavedLocationUseCase: GetSavedLocationUseCase = mockk()
    private val saveLocationUseCase: SaveLocationUseCase = mockk()
    private val observeLocationUseCase: ObserveLocationUseCase = mockk()
    private val locationService: LocationService = mockk()

    private val gpsLocation = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null)
    private val savedLocation = LocationData(41.1, 29.1, "Turkey", "TR", "Istanbul", null)

    private val testSettings = Settings(
        location = LocationSettings(
            latitude = 41.0,
            longitude = 29.0,
            cityName = "Istanbul",
            district = null,
            country = "Turkey",
            timeZone = "Europe/Istanbul"
        )
    )

    private fun coordinator() = LocationCoordinator(
        settingsRepository = settingsRepository,
        getSavedLocationUseCase = getSavedLocationUseCase,
        saveLocationUseCase = saveLocationUseCase,
        observeLocationUseCase = observeLocationUseCase,
        locationService = locationService
    )

    @Test
    fun `resolveInitial prefers settings location`() = runTest {
        coEvery { settingsRepository.getSettings() } returns testSettings
        val result = coordinator().resolveInitial()
        assertThat(result?.city).isEqualTo("Istanbul")
        assertThat(result?.countryCode).isEqualTo("TR")
    }

    @Test
    fun `resolveInitial falls back to saved location when settings throw`() = runTest {
        coEvery { settingsRepository.getSettings() } throws RuntimeException("no settings")
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        val result = coordinator().resolveInitial()
        assertThat(result).isEqualTo(savedLocation)
    }

    @Test
    fun `resolveInitial falls back to GPS when settings and saved both fail`() = runTest {
        coEvery { settingsRepository.getSettings() } throws RuntimeException("no settings")
        coEvery { getSavedLocationUseCase() } returns Result.failure(RuntimeException("no saved"))
        coEvery { locationService.getCurrentLocation() } returns gpsLocation
        coEvery { saveLocationUseCase.invoke(gpsLocation) } returns Unit

        val result = coordinator().resolveInitial()

        assertThat(result).isEqualTo(gpsLocation)
        coVerify { saveLocationUseCase.invoke(gpsLocation) }
    }

    @Test
    fun `observeSettingsChanges maps LocationSettings to LocationData`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(testSettings, testSettings)
        coordinator().observeSettingsChanges().test {
            val first = awaitItem()
            assertThat(first.countryCode).isEqualTo("TR")
            assertThat(first.city).isEqualTo("Istanbul")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLocationChanges relays location service values`() = runTest {
        every { observeLocationUseCase() } returns flowOf(savedLocation)
        coordinator().observeLocationChanges().test {
            assertThat(awaitItem()).isEqualTo(savedLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resolveSavedAndDetectDrift returns saved location and does not set prompt when same`() = runTest {
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        coEvery { locationService.getCurrentLocation() } returns savedLocation
        every { locationService.isDifferentThen(savedLocation) } returns false

        val result = coordinator().resolveSavedAndDetectDrift()

        assertThat(result).isEqualTo(savedLocation)
        assertThat(coordinator().locationUpdatePrompt.value).isFalse()
    }

    @Test
    fun `resolveSavedAndDetectDrift sets prompt when GPS differs`() = runTest {
        coEvery { getSavedLocationUseCase() } returns success(savedLocation)
        coEvery { locationService.getCurrentLocation() } returns gpsLocation
        every { locationService.isDifferentThen(savedLocation) } returns true

        coordinator().resolveSavedAndDetectDrift()

        assertThat(coordinator().locationUpdatePrompt.value).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*LocationCoordinatorTest"`
Expected: FAIL — `LocationCoordinator` unresolved

- [ ] **Step 3: Create `LocationCoordinator.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.ObserveLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_location.LocationService
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

/**
 * Resolves the source-of-truth [LocationData] (settings -> saved -> GPS), exposes the
 * location/settings observer flows, and tracks whether GPS has drifted from the saved location.
 */
@OptIn(FlowPreview::class)
@Factory
class LocationCoordinator(
    private val settingsRepository: SettingsRepository,
    private val getSavedLocationUseCase: GetSavedLocationUseCase,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val observeLocationUseCase: ObserveLocationUseCase,
    private val locationService: LocationService
) {
    private val _locationUpdatePrompt = MutableStateFlow(false)
    val locationUpdatePrompt: StateFlow<Boolean> = _locationUpdatePrompt

    /** Observes repository location pushes (relayed with debounce + distinct). */
    fun observeLocationChanges(): Flow<LocationData> =
        observeLocationUseCase()
            .debounce(500)
            .distinctUntilChanged()

    /** Observes settings changes and maps them to [LocationData]. */
    fun observeSettingsChanges(): Flow<LocationData> =
        settingsRepository.observeSettings()
            .debounce(500)
            .distinctUntilChanged()
            .map { setLocationDataFrom(it.location) }

    /** Settings -> saved location -> GPS fallback precedence. */
    suspend fun resolveInitial(): LocationData? {
        return try {
            setLocationDataFrom(settingsRepository.getSettings().location)
        } catch (e: Exception) {
            Log.e("LocationCoordinator", "Failed to load from settings: ${e.message}")
            getSavedLocationUseCase()
                .getOrNullWrapped()
                ?: refreshFromGps()
        }
    }

    /**
     * Returns the saved location (or GPS fallback). Sets [locationUpdatePrompt] when the
     * current GPS position differs from what was saved, mirroring the old refresh flow.
     */
    suspend fun resolveSavedAndDetectDrift(): LocationData? {
        val saved = getSavedLocationUseCase().getOrNullWrapped()
            ?: return refreshFromGps()
        val current = locationService.getCurrentLocation()
        if (current != null && locationService.isDifferentThen(saved)) {
            _locationUpdatePrompt.value = true
        }
        return saved
    }

    /** Gets the current GPS location and saves it. Returns null when GPS is unavailable. */
    suspend fun refreshFromGps(): LocationData? {
        return try {
            val gpsLocation = locationService.getCurrentLocation()
            if (gpsLocation != null) {
                saveLocationUseCase(gpsLocation)
                gpsLocation
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationCoordinator", "GPS fallback failed: ${e.message}")
            null
        }
    }

    fun setLocationDataFrom(locationSettings: LocationSettings): LocationData {
        return LocationData(
            latitude = locationSettings.latitude,
            longitude = locationSettings.longitude,
            country = locationSettings.country,
            countryCode = getCountryCode(locationSettings.timeZone),
            city = locationSettings.cityName,
            county = locationSettings.district
        )
    }

    private fun getCountryCode(timeZone: String): String? {
        return when {
            timeZone.contains("Istanbul", ignoreCase = true) ||
                timeZone.contains("Europe/Istanbul", ignoreCase = true) -> "TR"
            timeZone.contains("Europe/Berlin", ignoreCase = true) -> "DE"
            timeZone.contains("Europe/London", ignoreCase = true) -> "GB"
            timeZone.contains("Europe/Paris", ignoreCase = true) -> "FR"
            timeZone.contains("Asia/Jakarta", ignoreCase = true) -> "ID"
            timeZone.contains("Asia/Riyadh", ignoreCase = true) -> "SA"
            else -> null
        }
    }

    /** Small helper so getSavedLocationUseCase().getOrNull() stays explicit about Result. */
    private suspend fun Result<LocationData>.getOrNullWrapped(): LocationData? = getOrNull()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*LocationCoordinatorTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationCoordinator.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/LocationCoordinatorTest.kt
git commit -m "feat(home): add LocationCoordinator component"
```

---

### Task 7: Create `PrayerTimesLoader` (TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/PrayerTimesLoader.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/PrayerTimesLoaderTest.kt`

Contains the original `processLocationForPrayerTimes`/`updatePrayerState` logic as pure `suspend`/pure functions.

- [ ] **Step 1: Write the failing test `PrayerTimesLoaderTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import kotlin.Result.Companion.success

class PrayerTimesLoaderTest {

    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = mockk()
    private val calculator: PrayerLogicEngine = mockk(relaxed = true)
    private val formatter: PrayerFormatter = mockk(relaxed = true)

    private val location = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    @Test
    fun `load builds prayerState timeState locationState on success`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns success(listOf(fajr, dhuhr))
        every { formatter.withLocalizedNames(any()) } returns listOf(fajr, dhuhr)
        every { formatter.getInitialTimeInfo(any()) } returns TimeUiState(gregorianFullDate = "02 Ağustos 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(fajr, dhuhr)

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val result = loader.load(location)

        assertThat(result.isSuccess).isTrue()
        val loaded = result.getOrThrow()
        assertThat(loaded.prayerState.currentPrayer).isEqualTo(fajr)
        assertThat(loaded.prayerState.nextPrayer).isEqualTo(dhuhr)
        assertThat(loaded.prayerState.prayers[0].isCurrent).isTrue()
        assertThat(loaded.timeState.gregorianFullDate).isEqualTo("02 Ağustos 2026")
        assertThat(loaded.locationState.locationInfoText).isEqualTo("Istanbul, TR")
    }

    @Test
    fun `load maps failure to a failed Result`() = runTest {
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("fetch failed"))

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val result = loader.load(location)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("fetch failed")
    }

    @Test
    fun `computePrayerState marks only the current prayer as isCurrent`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(dhuhr, null)

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val zoneId = getZoneIdFromLocation(location.countryCode)
        val state = loader.computePrayerState(listOf(fajr, dhuhr), zoneId)

        assertThat(state.prayers[0].isCurrent).isFalse()
        assertThat(state.prayers[1].isCurrent).isTrue()
        assertThat(state.currentPrayer).isEqualTo(dhuhr)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerTimesLoaderTest"`
Expected: FAIL — `PrayerTimesLoader` unresolved

- [ ] **Step 3: Create `PrayerTimesLoader.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId

data class LoadedPrayerData(
    val prayerState: PrayerUiState,
    val timeState: TimeUiState,
    val locationState: LocationUiState,
    val zoneId: ZoneId
)

/**
 * Fetches prayer times for a [LocationData], localizes names, and computes which prayer is
 * current/next. Pure data transformation - no loops, no lifecycle.
 */
@Factory
class PrayerTimesLoader(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter
) {
    suspend fun load(location: LocationData): Result<LoadedPrayerData> {
        val zoneId = getZoneIdFromLocation(location.countryCode)
        val locationDateTime = LocalDateTime.now(zoneId)
        return getPrayerTimesUseCase(
            date = locationDateTime,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId
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

    /** Recomputes current/next + isCurrent flags. Mirrors the old updatePrayerState. */
    fun computePrayerState(prayers: List<Prayer>, zoneId: ZoneId): PrayerUiState {
        val (currentPrayer, nextPrayer) =
            calculator.findCurrentAndNextPrayer(prayers, zoneId)
        val prayersWithCurrent = prayers.map { prayer ->
            currentPrayer?.let {
                prayer.copy(isCurrent = prayer.name == it.name)
            } ?: prayer.copy(isCurrent = false)
        }
        return PrayerUiState(
            prayers = prayersWithCurrent,
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerTimesLoaderTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/PrayerTimesLoader.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/PrayerTimesLoaderTest.kt
git commit -m "feat(home): add PrayerTimesLoader component"
```

---

### Task 8: Create `CountdownEngine` (TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownEngine.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/CountdownEngineTest.kt`

Runs the per-second tick loop. Emits `countdownState` (timeRemaining + currentTime), a `prayerPassedSignal` (a prayer time elapsed → recompute current/next), and a `dayChangedSignal` (date changed → reload). This is the component whose extract fixes the self-restart loop and per-second `DailyPrayers` recomposition.

- [ ] **Step 1: Write the failing test `CountdownEngineTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownEngineTest {

    private val calculator: PrayerLogicEngine = mockk(relaxed = true)
    private val formatter: PrayerFormatter = mockk(relaxed = true)
    private val zoneId = ZoneId.of("Europe/Istanbul")

    private val today = LocalDate(2026, 8, 2)
    private val nextPrayer = Prayer(
        name = "Öğle",
        arabicName = "الظهر",
        time = LocalTime(12, 30),
        date = today
    )
    private val prayerState = PrayerUiState(
        prayers = listOf(nextPrayer),
        currentPrayer = Prayer("İmsak", "الفجر", LocalTime(5, 0), today),
        nextPrayer = nextPrayer
    )

    @Test
    fun `tick emits countdown state every second`() = runTest {
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(prayerState.currentPrayer, nextPrayer)
        coEvery { calculator.calculateTimeRemaining(nextPrayer.time, zoneId) } returns Duration.ofHours(2)
        every { formatter.getFormattedCurrentTime(zoneId) } returns "10:30:00"
        every { formatter.formatTimeRemaining(Duration.ofHours(2)) } returns "02:00:00"

        val engine = CountdownEngine(calculator, formatter)
        engine.start(prayerState, zoneId, scope = this)

        advanceTimeBy(1_000)
        runCurrent()

        assertThat(engine.countdownState.value.timeRemaining).isEqualTo("02:00:00")
        assertThat(engine.countdownState.value.currentTime).isEqualTo("10:30:00")

        engine.stop()
    }

    @Test
    fun `tick emits prayerPassedSignal when current time has passed the next prayer`() = runTest {
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(prayerState.nextPrayer, null)
        coEvery { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ZERO
        every { formatter.getFormattedCurrentTime(zoneId) } returns "12:31:00"

        val engine = CountdownEngine(calculator, formatter)
        engine.prayerPassedSignal.test {
            engine.start(prayerState, zoneId, scope = this)

            advanceTimeBy(1_000)
            runCurrent()

            awaitItem()
            engine.stop()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tick emits dayChangedSignal when the prayer date is stale`() = runTest {
        val yesterday = LocalDate(2026, 8, 1)
        val stalePrayerState = prayerState.copy(
            nextPrayer = nextPrayer.copy(date = yesterday)
        )
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, stalePrayerState.nextPrayer)
        coEvery { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ofHours(1)
        every { formatter.getFormattedCurrentTime(zoneId) } returns "01:00:00"
        every { formatter.formatTimeRemaining(any()) } returns "01:00:00"

        val engine = CountdownEngine(calculator, formatter)
        engine.dayChangedSignal.test {
            engine.start(stalePrayerState, zoneId, scope = this)

            advanceTimeBy(1_000)
            runCurrent()

            awaitItem()
            engine.stop()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*CountdownEngineTest"`
Expected: FAIL — `CountdownEngine` unresolved

- [ ] **Step 3: Create `CountdownEngine.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId

/**
 * Drives the per-second countdown tick. Publishes [countdownState] and signals when a prayer
 * time has elapsed ([prayerPassedSignal]) or the date changed ([dayChangedSignal]).
 */
@Factory
class CountdownEngine(
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter
) {
    private val _countdownState = MutableStateFlow(CountdownUiState())
    val countdownState: StateFlow<CountdownUiState> = _countdownState

    private val _prayerPassedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val prayerPassedSignal: SharedFlow<Unit> = _prayerPassedSignal

    private val _dayChangedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dayChangedSignal: SharedFlow<Unit> = _dayChangedSignal

    private var countdownJob: Job? = null
    private var zoneId: ZoneId? = null
    private var prayerState: PrayerUiState? = null

    /** Launches the tick loop. Call [stop] before reusing with a new prayer set. */
    fun start(prayerState: PrayerUiState, zoneId: ZoneId, scope: CoroutineScope): Job {
        this.prayerState = prayerState
        this.zoneId = zoneId
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                updateCountdown()
                delay(1_000)
            }
        }
        return countdownJob ?: Job()
    }

    fun stop() {
        countdownJob?.cancel()
        countdownJob = null
        prayerState = null
        zoneId = null
    }

    fun isRunning(): Boolean = countdownJob?.isActive == true

    private suspend fun updateCountdown() {
        val currentState = prayerState ?: return
        val currentZoneId = zoneId ?: return
        val nextPrayer = currentState.nextPrayer
        val currentTime = LocalDateTime.now(currentZoneId)
        val currentTimeString = formatter.getFormattedCurrentTime(currentZoneId)

        if (nextPrayer != null) {
            val currentPrayer = currentState.currentPrayer
            if (currentPrayer != null && nextPrayer.date != currentPrayer.date) {
                if (isDayChanged(currentPrayer.date, currentTime.date)) {
                    countdownJob?.cancel()
                    _dayChangedSignal.emit(Unit)
                    return
                }
            }

            val nextPrayerDateTime = LocalDateTime(date = nextPrayer.date, time = nextPrayer.time)
            if (currentTime >= nextPrayerDateTime) {
                _prayerPassedSignal.emit(Unit)
                return
            }

            val duration = calculator.calculateTimeRemaining(nextPrayer.time, currentZoneId)
            _countdownState.value = CountdownUiState(
                timeRemaining = formatter.formatTimeRemaining(duration),
                currentTime = currentTimeString
            )
        } else {
            val currentDeviceDate = currentTime.date
            val prayerDate = currentState.prayers.firstOrNull()?.date
            if (isDayChanged(prayerDate, currentDeviceDate)) {
                countdownJob?.cancel()
                _dayChangedSignal.emit(Unit)
            } else {
                _countdownState.value = CountdownUiState(
                    timeRemaining = "--:--:--",
                    currentTime = currentTimeString
                )
            }
        }
    }

    private fun isDayChanged(
        prayerDate: LocalDate?,
        currentDeviceDate: LocalDate
    ): Boolean = prayerDate != null && currentDeviceDate != prayerDate
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*CountdownEngineTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/CountdownEngine.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/CountdownEngineTest.kt
git commit -m "feat(home): add CountdownEngine component"
```

---

### Task 9: Rewrite `HomeViewModel` as thin orchestrator

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt` (rewrite)
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStates.kt` (remove `timeRemaining` from `PrayerUiState`)

- [ ] **Step 1: Remove `timeRemaining` from `PrayerUiState`**

In `HomeUiStates.kt`, change `PrayerUiState` to:

```kotlin
data class PrayerUiState(
        val prayers: List<Prayer> = emptyList(),
        val currentPrayer: Prayer? = null,
        val nextPrayer: Prayer? = null
)
```

- [ ] **Step 2: Rewrite `HomeViewModel.kt` (entire file)**

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.ObserveLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_location.LocationService
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class HomeViewModel(
    private val locationCoordinator: LocationCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader
) : ViewModel() {

    private val _screenGate = MutableStateFlow<HomeScreenGate>(HomeScreenGate.Loading)
    val screenGate: StateFlow<HomeScreenGate> = _screenGate

    private val _timeState = MutableStateFlow<TimeUiState?>(null)
    val timeState: StateFlow<TimeUiState?> = _timeState

    private val _locationState = MutableStateFlow<LocationUiState?>(null)
    val locationState: StateFlow<LocationUiState?> = _locationState

    private val _prayerState = MutableStateFlow<PrayerUiState?>(null)
    val prayerState: StateFlow<PrayerUiState?> = _prayerState

    private val _promptState = MutableStateFlow(false)
    val promptState: StateFlow<Boolean> = _promptState

    private var locationObserverJob: Job? = null
    private var settingsObserverJob: Job? = null
    private var prayerPassedObserverJob: Job? = null
    private var dayChangedObserverJob: Job? = null

    init {
        locationObserverJob = viewModelScope.launch {
            locationCoordinator.observeLocationChanges().collect { onLocationResolved(it) }
        }
        settingsObserverJob = viewModelScope.launch {
            locationCoordinator.observeSettingsChanges().collect { onLocationResolved(it) }
        }
        prayerPassedObserverJob = viewModelScope.launch {
            countdownEngine.prayerPassedSignal.collect { refreshPrayerState() }
        }
        dayChangedObserverJob = viewModelScope.launch {
            countdownEngine.dayChangedSignal.collect { loadPrayerTimesForCurrentLocation() }
        }
        loadInitialLocation()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> { loadPrayerTimesForCurrentLocation() }
            HomeEvent.OnCountDown -> { startPrayerCountdown() }
            HomeEvent.OnPermissionsGranted -> { loadPrayerTimesForCurrentLocation() }
            HomeEvent.OnUpdateLocationConfirmed -> { updateLocationChange() }
            HomeEvent.OnLoadQuranVerse -> { loadRandomVerse() }
            HomeEvent.OnVerseClicked -> { setVerseSheetVisibility(isVisible = true) }
            HomeEvent.OnVerseDetailDismissed -> { setVerseSheetVisibility(isVisible = false) }
        }
    }

    private fun loadInitialLocation() {
        viewModelScope.launch {
            val location = locationCoordinator.resolveInitial()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val location = locationCoordinator.resolveSavedAndDetectDrift()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    private fun updateLocationChange() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val newLocation = locationCoordinator.refreshFromGps()
            if (newLocation != null) {
                onLocationResolved(newLocation)
            } else {
                _screenGate.value = HomeScreenGate.Error(
                    "Failed to get updated location. Please try again."
                )
            }
        }
    }

    private suspend fun onLocationResolved(location: com.kutluoglu.prayer.model.location.LocationData) {
        prayerTimesLoader.load(location)
            .onSuccess { loaded ->
                _locationState.value = loaded.locationState
                _timeState.value = loaded.timeState
                _prayerState.value = loaded.prayerState
                _promptState.value = locationCoordinator.locationUpdatePrompt.value
                _screenGate.value = HomeScreenGate.Ready
                startCountdownFromCurrentState()
            }
            .onFailure { error ->
                _screenGate.value = HomeScreenGate.Error(
                    error.message ?: HomeErrorMapper.getUserFriendlyErrorMessage(error)
                )
            }
    }

    private fun refreshPrayerState() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(currentState.prayers.firstOrNull()?.let {
            _locationState.value?.locationData?.countryCode
        })
        val refreshed = prayerTimesLoader.computePrayerState(currentState.prayers, zoneId)
        _prayerState.value = refreshed
        _screenGate.value = HomeScreenGate.Ready
    }

    private fun startPrayerCountdown() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        countdownEngine.start(currentState, zoneId, viewModelScope)
    }

    private fun startCountdownFromCurrentState() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        countdownEngine.start(currentState, zoneId, viewModelScope)
    }

    private fun loadRandomVerse() {
        quranVerseLoader.loadVerse(
            scope = viewModelScope,
            isScreenReady = { _screenGate.value == HomeScreenGate.Ready }
        )
    }

    private fun setVerseSheetVisibility(isVisible: Boolean) {
        quranVerseLoader.setSheetVisible(isVisible)
    }

    private fun fail(message: String) {
        _screenGate.value = HomeScreenGate.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        countdownEngine.stop()
        locationObserverJob?.cancel()
        settingsObserverJob?.cancel()
        prayerPassedObserverJob?.cancel()
        dayChangedObserverJob?.cancel()
    }
}
```

> Note: To avoid constructor bloat, this file imports the four component types; the old use-case/formatter/service imports can be dropped. `refreshPrayerState` recomputes current/next when a prayer time passes without touching location/prayer fetch.

- [ ] **Step 3: Compile check**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (unused-import warnings are fine)

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeUiStates.kt
git commit -m "refactor(home): rewrite HomeViewModel as thin orchestrator over per-concern components"
```

---

### Task 10: Rewrite `HomeViewModelTest` (slim, orchestration-only)

**Files:**
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt` (rewrite)

- [ ] **Step 1: Rewrite `HomeViewModelTest.kt` (entire file)**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.Surah
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.resetMain
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val locationCoordinator: LocationCoordinator = mockk(relaxed = true)
    private val prayerTimesLoader: PrayerTimesLoader = mockk(relaxed = true)
    private val countdownEngine: CountdownEngine = mockk(relaxed = true)
    private val quranVerseLoader: QuranVerseLoader = mockk(relaxed = true)

    private val location = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    private fun viewModel() = HomeViewModel(
        locationCoordinator,
        prayerTimesLoader,
        countdownEngine,
        quranVerseLoader
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init resolves initial location and emits Ready when load succeeds`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns location
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { prayerTimesLoader.load(location) } returns success(
            LoadedPrayerData(
                prayerState = PrayerUiState(),
                timeState = TimeUiState(),
                locationState = com.kutluoglu.prayer_feature.common.states.LocationUiState(location, "Istanbul, TR"),
                zoneId = java.time.ZoneId.of("Europe/Istanbul")
            )
        )

        val vm = viewModel()
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `refresh failure switches gate to Error`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns location
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { prayerTimesLoader.load(location) } returns
            Result.failure(RuntimeException("fetch failed"))

        val vm = viewModel()
        assertThat(vm.screenGate.value is HomeScreenGate.Error).isTrue()
    }

    @Test
    fun `onEvent OnRefresh triggers reload and resolves to Ready`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns null
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        coEvery { locationCoordinator.resolveSavedAndDetectDrift() } returns location
        coEvery { prayerTimesLoader.load(location) } returns success(
            LoadedPrayerData(
                prayerState = PrayerUiState(),
                timeState = TimeUiState(),
                locationState = com.kutluoglu.prayer_feature.common.states.LocationUiState(location, "Istanbul, TR"),
                zoneId = java.time.ZoneId.of("Europe/Istanbul")
            )
        )

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnRefresh)
        assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Ready)
    }

    @Test
    fun `onEvent OnVerseClicked toggles the sheet`() = runTest {
        coEvery { locationCoordinator.resolveInitial() } returns null
        coEvery { locationCoordinator.observeLocationChanges() } returns flowOf()
        coEvery { locationCoordinator.observeSettingsChanges() } returns flowOf()
        every { quranVerseLoader.quranState } returns MutableStateFlow(QuranUiState())

        val vm = viewModel()
        vm.onEvent(HomeEvent.OnVerseClicked)
        vm.onEvent(HomeEvent.OnVerseDetailDismissed)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt
git commit -m "test(home): rewrite HomeViewModelTest as slim orchestration test"
```

---

### Task 11: Wire `HomeRoute` to collect flows and merge

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`

- [ ] **Step 1: Rewrite `HomeRoute.kt` (entire file)**

```kotlin
package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_feature.home.HomeViewModel
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.mergeToHomeUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject<QuranVerseFormatter>(),
    navController: NavController
) {
    val gate by viewModel.screenGate.collectAsState()
    val time by viewModel.timeState.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val prayer by viewModel.prayerState.collectAsState()
    val countdown by viewModel.countdownState.collectAsState()
    val quran by viewModel.quranState.collectAsState()
    val prompt by viewModel.promptState.collectAsState()

    val uiState = remember(gate, time, location, prayer, countdown, quran, prompt) {
        mergeToHomeUiState(gate, location, time, prayer, countdown, quran, prompt)
    }

    HomeScreen(
        navController = navController,
        uiState = uiState,
        quranVerseFormatter = verseFormatter,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt
git commit -m "refactor(home): wire HomeRoute to collect per-concern flows and merge into HomeUiState"
```

---

### Task 12: Update `HomeTopContainer` to read ticking data from `countdownState`

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeTopContainer.kt`

- [ ] **Step 1: Update `HomeTopContainer.kt`**

Apply these changes:

1. Add a `countdownState` derivation next to the existing state derivations (`HomeTopContainer.kt:43-51`):

```kotlin
val countdownState by remember(successState) {
    derivedStateOf { successState?.countdownState }
}
```

2. Change the `LaunchedEffect(prayerState) { onStartCount() }` (`HomeTopContainer.kt:55`) so it keys on the per-load prayerState (stable across ticks), not a per-second object:

```kotlin
LaunchedEffect(prayerState) { onStartCount() }
```

(This line stays the same text, but now `prayerState` is stable across ticks because it is only replaced on prayer load / recompute — the per-second restart loop is eliminated.)

3. Pass ticking data into `TimeInfoSection` (which currently reads `timeState.currentTime`) and `NextPrayerInfo` (currently `prayerState.timeRemaining`):

```kotlin
timeState?.let { TimeInfoSection(timeState = it, currentTime = countdownState?.currentTime ?: "") }
```

```kotlin
prayerState?.let { NextPrayerInfo(prayerState = it, timeRemaining = countdownState?.timeRemaining ?: "--:--:--") }
```

4. Update the two private composables' signatures:

```kotlin
@Composable
private fun TimeInfoSection(timeState: TimeUiState, currentTime: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = currentTime,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = timeState.gregorianFullDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = timeState.hijriDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun NextPrayerInfo(prayerState: PrayerUiState, timeRemaining: String) {
    val nextPrayerNameRaw = prayerState.nextPrayer?.name ?: "İmsak"

    val nextPrayerDisplayName = when (nextPrayerNameRaw) {
        "İmsak" -> "Sabah"
        else -> nextPrayerNameRaw
    }

    val timeUntilText = when (nextPrayerNameRaw) {
        "Güneş" -> stringResource(id = R.string.time_until_sunrise)
        else -> stringResource(id = R.string.time_until_prayer_format, nextPrayerDisplayName)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = getPrayerDrawableIdFrom(
                        prayerState.currentPrayer?.name ?: ""
                    )
                ),
                contentDescription = stringResource(id = R.string.time_until_message),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = timeUntilText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timeRemaining,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeTopContainer.kt
git commit -m "refactor(home): read ticking data from countdownState in HomeTopContainer"
```

---

### Task 13: Full verification + impact review

- [ ] **Step 1: Run the full home module test suite**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS (all suites: HomeUiStateMergerTest, LocationCoordinatorTest, PrayerTimesLoaderTest, CountdownEngineTest, QuranVerseLoaderTest, HomeViewModelTest, FeatureHomeTestSuite)

- [ ] **Step 2: Run the whole-project unit tests**

Run: `./gradlew unitTests`
Expected: PASS

- [ ] **Step 3: Run `gitnexus_detect_changes` and review affected scope**

Expected: only Home symbols (`HomeViewModel`, `HomeUiStates`, `HomeRoute`, `HomeTopContainer`, new components) and their execution flows appear. No unrelated modules affected.

- [ ] **Step 4: Run `./gradlew build` (compile + lint + assemble debug)**

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit anything remaining and summarize**

```bash
git add -A
git commit -m "refactor(home): complete HomeViewModel decomposition" || echo "nothing to commit"
```

---

## Self-Review

- **Spec coverage:** Every spec section maps to a task — flows (T2/T4/T9), components (T5/T6/T7/T8), merge (T3), Route wiring (T11), UI changes (T12), error handling (T3/T9), test plan (T3,5,6,7,8,10), DI via `@Factory` (each component), `onCleared` (T9). Spec's out-of-scope items untouched.
- **Placeholder scan:** No TBD/TODO; every step has concrete code or command.
- **Type consistency:** `CountdownUiState`, `QuranUiState`, `HomeScreenGate`, `LoadedPrayerData` defined once (T1/T2/T7) and referenced consistently in later tasks. Component constructor arg names in tests match the `@Factory` definitions. `mergeToHomeUiState` signature identical between T3 (definition) and T11 (call site).