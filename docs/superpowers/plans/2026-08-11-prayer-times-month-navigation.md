# Prayer Times Month Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users navigate to any past/future month from the Prayer Times screen via the existing header arrows, with a "Today" button, in-memory month caching, and today-highlight/auto-scroll gated to the current month.

**Architecture:** `PrayerTimesViewModel` gains a `selectedMonth` (kotlinx-datetime `YearMonth`), an in-memory `monthCache`, and an `onEvent(PrayerTimesEvent)` dispatcher. `loadMonthlyPrayerTimes()` delegates to `loadMonth(month)` which computes a month day-by-day via `GetPrayerTimesUseCase` and caches the result. `PrayerTimesUiState.Success` exposes `selectedMonth` + `isCurrentMonth`; the header label, "Today" button, auto-scroll, and today-highlight all derive from these. Navigation updates `selectedMonth` immediately (header changes right away) while keeping the current content visible until the new month is computed (no flash).

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3), kotlinx-datetime 0.7.1 (`YearMonth`), Koin, JUnit 5 + MockK + Turbine + Truth + kotlinx-coroutines-test.

**Impact (per AGENTS.md):** LOW risk. `PrayerTimesViewModel` → 1 direct importer (`PrayerTimesRoute.kt`); `PrayerTimesUiState` → 4 direct (Success/Error impls, `TopContainer.kt`, `PrayerContainer.kt`); `PrayerContainer` → 1 direct caller (`PayerTimesScreen`). All changes stay inside `prayer_feature:prayertimes`.

---

## File Structure

**Create:**
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesEvent.kt` — user-initiated events (previous/next/today).
- `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/MainCoroutineRule.kt` — test rule (copied from `prayer_feature:home`).
- `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt` — ViewModel tests.

**Modify:**
- `prayer_feature/prayertimes/build.gradle.kts` — add JUnit 5 / MockK / Turbine / Truth test deps + `useJUnitPlatform()`.
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesUiState.kt` — add `selectedMonth` + `isCurrentMonth` to `Success`.
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt` — month state, cache, `onEvent`, `loadMonth`.
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt` — clickable arrows, "Today" button, month label, gated auto-scroll/highlight.
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/navigation/PrayerTimesRoute.kt` — `LaunchedEffect(Unit)` initial load + pass `onEvent`.
- `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesScreen.kt` — thread `onEvent` to `PrayerContainer`.
- `prayer_feature/prayertimes/src/main/res/values/strings.xml` + `values-tr/strings.xml` — `today`, `previous_month`, `next_month`.
- `TODO.md` — mark item 10 done.

---

### Task 1: Add test dependencies to `prayer_feature:prayertimes/build.gradle.kts`

**Files:**
- Modify: `prayer_feature/prayertimes/build.gradle.kts`

- [ ] **Step 1: Add the JUnit 5 / coroutines / MockK / Turbine / Truth test dependencies**

Replace the `dependencies { ... }` block's testing section. Current block (lines 74–76):

```kotlin
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
```

Replace with:

```kotlin
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //region --- Testing Dependencies ---
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)
    //endregion
```

- [ ] **Step 2: Enable the JUnit Platform**

Append at the end of the file (after the `dependencies { ... }` block):

```kotlin
tasks.withType<Test> { useJUnitPlatform() }
```

- [ ] **Step 3: Verify the module still builds**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/prayertimes/build.gradle.kts
git commit -m "feat(prayertimes): add JUnit5 test dependencies for month navigation"
```

---

### Task 2: Create the `PrayerTimesEvent` sealed class

**Files:**
- Create: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesEvent.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

/**
 * User-initiated events on the Prayer Times screen.
 */
sealed class PrayerTimesEvent {
    data object OnPreviousMonth : PrayerTimesEvent()
    data object OnNextMonth : PrayerTimesEvent()
    data object OnToday : PrayerTimesEvent()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesEvent.kt
git commit -m "feat(prayertimes): add PrayerTimesEvent sealed class"
```

---

### Task 3: Extend `PrayerTimesUiState.Success` with `selectedMonth` and `isCurrentMonth`

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesUiState.kt`

- [ ] **Step 1: Add the new fields (with defaults so existing call sites still compile)**

Current `Success` (lines 18–23):

```kotlin
    data class Success(
            val monthlyPrayers: List<DailyPrayer> = emptyList(),
            val currentDayOfMonth: Int,
            val timeState: TimeUiState,
            val locationState: LocationUiState
    ) : PrayerTimesUiState
```

Replace with:

```kotlin
    data class Success(
            val monthlyPrayers: List<DailyPrayer> = emptyList(),
            val currentDayOfMonth: Int,
            val selectedMonth: YearMonth = YearMonth(1970, 1),
            val isCurrentMonth: Boolean = false,
            val timeState: TimeUiState,
            val locationState: LocationUiState
    ) : PrayerTimesUiState
```

Add the import at the top of the file (after the existing imports):

```kotlin
import kotlinx.datetime.YearMonth
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (existing code compiles because the new fields have defaults)

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesUiState.kt
git commit -m "feat(prayertimes): add selectedMonth and isCurrentMonth to Success UiState"
```

---

### Task 4: Write the failing `PrayerTimesViewModelTest` (RED)

**Files:**
- Create: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/MainCoroutineRule.kt`
- Create: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt`
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt` (add a no-op `onEvent` stub so the test compiles)

- [ ] **Step 1: Create `MainCoroutineRule.kt`** (copy of the one in `prayer_feature:home`)

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : AfterEachCallback, BeforeEachCallback {

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }
}
```

- [ ] **Step 2: Add a no-op `onEvent` stub to `PrayerTimesViewModel`**

In `PrayerTimesViewModel.kt`, add this function after `loadMonthlyPrayerTimes()` (temporary stub — replaced in Task 5):

```kotlin
    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.OnPreviousMonth -> Unit
            PrayerTimesEvent.OnNextMonth -> Unit
            PrayerTimesEvent.OnToday -> Unit
        }
    }
```

- [ ] **Step 3: Create `PrayerTimesViewModelTest.kt`**

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class PrayerTimesViewModelTest {

    private lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase
    private lateinit var getSavedLocationUseCase: GetSavedLocationUseCase
    private lateinit var calculator: PrayerLogicEngine
    private lateinit var formatter: PrayerFormatter
    private lateinit var viewModel: PrayerTimesViewModel

    private val mockLocation = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    private val mockPrayerList = listOf(
        Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0

        getPrayerTimesUseCase = mockk()
        getSavedLocationUseCase = mockk()
        calculator = mockk(relaxed = true)
        formatter = mockk(relaxed = true)

        coEvery { getSavedLocationUseCase() } returns success(mockLocation)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns success(mockPrayerList)
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, null)
        every { formatter.withLocalizedNames(any()) } returns mockPrayerList
        every { formatter.getInitialTimeInfo(any(), any(), any()) } returns TimeUiState(
            gregorianDayAndName = "1 Monday",
            hijriDate = "1 Muharram 1448"
        )
        every { formatter.getInitialTimeInfo(any()) } returns TimeUiState(gregorianShortDate = "August 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            getSavedLocationUseCase,
            calculator,
            formatter
        )
    }

    @Test
    fun `initial load computes the current month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            assertThat(success.selectedMonth).isEqualTo(currentMonth)
            assertThat(success.isCurrentMonth).isTrue()
            assertThat(success.monthlyPrayers.size).isEqualTo(currentMonth.numberOfDays)
            assertThat(success.currentDayOfMonth).isEqualTo(LocalDateTime.now(zoneId).day)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNextMonth loads the adjacent month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
            assertThat(success.selectedMonth).isEqualTo(nextMonth)
            assertThat(success.isCurrentMonth).isFalse()
            assertThat(success.monthlyPrayers.size).isEqualTo(nextMonth.numberOfDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPreviousMonth loads the adjacent month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnPreviousMonth)

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            val previousMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
            assertThat(success.selectedMonth).isEqualTo(previousMonth)
            assertThat(success.isCurrentMonth).isFalse()
            assertThat(success.monthlyPrayers.size).isEqualTo(previousMonth.numberOfDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToday returns to the current month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)
        viewModel.onEvent(PrayerTimesEvent.OnToday)

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            assertThat(success.selectedMonth).isEqualTo(currentMonth)
            assertThat(success.isCurrentMonth).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `revisiting a cached month does not recompute`() = runTest {
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)
        viewModel.onEvent(PrayerTimesEvent.OnToday)

        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
        val expectedCalls = currentMonth.numberOfDays + nextMonth.numberOfDays
        assertThat(callCount).isEqualTo(expectedCalls)
    }
}
```

- [ ] **Step 4: Run the tests to verify they FAIL (RED)**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: FAIL — `initial load computes the current month` fails because `selectedMonth` is the default `YearMonth(1970, 1)` (the stub `onEvent` does nothing, so navigation tests also fail).

- [ ] **Step 5: Commit the failing test**

```bash
git add prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/MainCoroutineRule.kt
git add prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModelTest.kt
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt
git commit -m "test(prayertimes): add failing PrayerTimesViewModelTest for month navigation"
```

---

### Task 5: Implement month navigation in `PrayerTimesViewModel` (GREEN)

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`

- [ ] **Step 1: Replace the whole file body with the month-aware implementation**

Replace the entire contents of `PrayerTimesViewModel.kt` with:

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.yearMonth
import org.koin.android.annotation.KoinViewModel
import java.time.ZoneId
import java.time.chrono.HijrahDate

@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getSavedLocationUseCase: GetSavedLocationUseCase,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerTimesUiState>(PrayerTimesUiState.Loading)
    val uiState: StateFlow<PrayerTimesUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrayerTimesUiState.Loading
        )

    private var savedLocation: LocationData? = null
    private var zoneId: ZoneId? = null
    private var selectedMonth: YearMonth = LocalDateTime.now(ZoneId.systemDefault()).date.yearMonth
    private val monthCache = mutableMapOf<YearMonth, List<DailyPrayer>>()
    private var isLoading = false

    fun loadMonthlyPrayerTimes() {
        viewModelScope.launch {
            getSavedLocationUseCase()
                .onSuccess { location ->
                    savedLocation = location
                    val resolvedZoneId = getZoneIdFromLocation(location.countryCode)
                    zoneId = resolvedZoneId
                    val today = LocalDateTime.now(resolvedZoneId)
                    selectedMonth = today.date.yearMonth
                    loadMonth(selectedMonth)
                }
                .onFailure {
                    _uiState.value = PrayerTimesUiState.Error(it.message ?: "Failed to get saved location.")
                }
        }
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.OnPreviousMonth -> navigateToMonth(selectedMonth.minus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnNextMonth -> navigateToMonth(selectedMonth.plus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnToday -> navigateToMonth(currentMonth())
        }
    }

    private fun navigateToMonth(month: YearMonth) {
        selectedMonth = month
        val current = _uiState.value
        if (current is PrayerTimesUiState.Success) {
            _uiState.value = current.copy(
                selectedMonth = month,
                isCurrentMonth = month == currentMonth()
            )
        }
        loadMonth(month)
    }

    private fun currentMonth(): YearMonth =
        LocalDateTime.now(zoneId ?: ZoneId.systemDefault()).date.yearMonth

    private fun loadMonth(month: YearMonth) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                val cached = monthCache[month]
                if (cached != null) {
                    emitSuccess(month, cached)
                    return@launch
                }
                val location = savedLocation ?: return@launch
                val resolvedZoneId = zoneId ?: return@launch
                val today = LocalDateTime.now(resolvedZoneId)
                val monthlyPrayers = mutableListOf<DailyPrayer>()
                for (day in 1..month.numberOfDays) {
                    val date = month.onDay(day)
                    getPrayerTimesUseCase(
                        date = date.atTime(0, 0),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        zoneId = resolvedZoneId
                    ).onSuccess { prayerTimes ->
                        val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                        val isToday = date == today.date
                        val (currentPrayer, _) = if (isToday) {
                            calculator.findCurrentAndNextPrayer(langDetectedPrayerTimes, resolvedZoneId)
                        } else {
                            Pair(null, null)
                        }
                        val prayersWithCurrent = langDetectedPrayerTimes.map {
                            it.copy(isCurrent = isToday && it.name == currentPrayer?.name)
                        }
                        val timeState = formatter.getInitialTimeInfo(
                            resolvedZoneId,
                            date.toJavaLocalDate(),
                            HijrahDate.from(date.toJavaLocalDate())
                        )
                        monthlyPrayers.add(
                            DailyPrayer(
                                dayOfMonth = date.day,
                                prayers = prayersWithCurrent,
                                gregorianDate = timeState.gregorianDayAndName,
                                hijriDate = timeState.hijriDate
                            )
                        )
                    }.onFailure {
                        _uiState.value = PrayerTimesUiState.Error(
                            it.message ?: "Failed to load prayer times for day $day."
                        )
                        return@launch
                    }
                }
                monthCache[month] = monthlyPrayers
                emitSuccess(month, monthlyPrayers)
            } finally {
                isLoading = false
            }
        }
    }

    private fun emitSuccess(month: YearMonth, monthlyPrayers: List<DailyPrayer>) {
        val location = savedLocation ?: return
        val resolvedZoneId = zoneId ?: return
        val today = LocalDateTime.now(resolvedZoneId)
        _uiState.value = PrayerTimesUiState.Success(
            monthlyPrayers = monthlyPrayers,
            currentDayOfMonth = today.day,
            selectedMonth = month,
            isCurrentMonth = month == today.date.yearMonth,
            timeState = formatter.getInitialTimeInfo(resolvedZoneId),
            locationState = LocationUiState(
                locationData = location,
                locationInfoText = formatter.locationInfo(location)
            )
        )
    }
}
```

Notes:
- `month.onDay(day)` and `LocalDate.yearMonth` are kotlinx-datetime 0.7.1 extensions (verified against the 0.7.1 jar).
- `month.numberOfDays` is a member property of `YearMonth`.
- `selectedMonth` updates immediately in `navigateToMonth` (header changes right away) while the current content stays visible until `loadMonth` emits the new `Success`.

- [ ] **Step 2: Run the tests to verify they PASS (GREEN)**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesViewModelTest"`
Expected: PASS — all 5 tests green.

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt
git commit -m "feat(prayertimes): implement month navigation with caching in PrayerTimesViewModel"
```

---

### Task 6: Add string resources for the header controls

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/res/values/strings.xml`
- Modify: `prayer_feature/prayertimes/src/main/res/values-tr/strings.xml`

- [ ] **Step 1: Add English strings**

In `values/strings.xml`, after the `<string name="image_desc">Kubbe</string>` line, add:

```xml
    <string name="today">Today</string>
    <string name="previous_month">Previous month</string>
    <string name="next_month">Next month</string>
```

- [ ] **Step 2: Add Turkish strings**

In `values-tr/strings.xml`, after the `<string name="image_desc">Kubbe</string>` line, add:

```xml
    <string name="today">Bugün</string>
    <string name="previous_month">Önceki ay</string>
    <string name="next_month">Sonraki ay</string>
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/prayertimes/src/main/res/values/strings.xml prayer_feature/prayertimes/src/main/res/values-tr/strings.xml
git commit -m "feat(prayertimes): add header control string resources"
```

---

### Task 7: Wire up the header controls and gating in `PrayerContainer`

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt`

- [ ] **Step 1: Update imports**

Add these imports to the import block:

```kotlin
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.kutluoglu.core.common.gregorianShortFormatter
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesEvent
import kotlinx.datetime.YearMonth
```

- [ ] **Step 2: Add `onEvent` to `PrayerContainer` and pass month state into `PrayerTimesContent`**

Replace the `PrayerContainer` function (lines 54–67):

```kotlin
@Composable
fun PrayerContainer(
        uiState: PrayerTimesUiState,
        onEvent: (PrayerTimesEvent) -> Unit
) {
    when (uiState) {
        is PrayerTimesUiState.Loading -> LoadingIndicator()
        is PrayerTimesUiState.Error -> ErrorMessage(message = uiState.message)
        is PrayerTimesUiState.Success -> PrayerTimesContent(
            monthlyPrayers = uiState.monthlyPrayers,
            currentDayOfMonth = uiState.currentDayOfMonth,
            selectedMonth = uiState.selectedMonth,
            isCurrentMonth = uiState.isCurrentMonth,
            onEvent = onEvent
        )
    }
}
```

- [ ] **Step 3: Update `PrayerTimesContent` — month label, gated auto-scroll, threaded callbacks**

Replace the `PrayerTimesContent` function (lines 69–99):

```kotlin
@Composable
private fun PrayerTimesContent(
        monthlyPrayers: List<DailyPrayer>,
        currentDayOfMonth: Int,
        selectedMonth: YearMonth,
        isCurrentMonth: Boolean,
        onEvent: (PrayerTimesEvent) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(monthlyPrayers, isCurrentMonth) {
        if (isCurrentMonth) {
            // Find the index of the current day. The list is 0-indexed, so subtract 1.
            val todayIndex = currentDayOfMonth - 1
            // Make sure the index is valid before scrolling
            if (todayIndex in monthlyPrayers.indices) {
                // Animate the scroll to the item. Use scrollToItem for an instant jump.
                listState.animateScrollToItem(index = todayIndex)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary), // Apply background to the whole container
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Items ---
        TitleHeader(
            selectedMonthLabel = selectedMonthLabel(selectedMonth),
            isCurrentMonth = isCurrentMonth,
            onPrevious = { onEvent(PrayerTimesEvent.OnPreviousMonth) },
            onNext = { onEvent(PrayerTimesEvent.OnNextMonth) },
            onToday = { onEvent(PrayerTimesEvent.OnToday) }
        )
        PrayersHeader(monthlyPrayers.firstOrNull()?.prayers ?: emptyList())
        // --- Content Items ---
        PrayerList(monthlyPrayers, currentDayOfMonth, isCurrentMonth, listState)
    }

}

private fun selectedMonthLabel(month: YearMonth): String =
    java.time.YearMonth.of(month.year, month.monthNumber).format(gregorianShortFormatter)
```

- [ ] **Step 4: Replace `TitleHeader` — clickable arrows, month label, "Today" button**

Replace the `TitleHeader` function (lines 101–136):

```kotlin
@Composable
private fun TitleHeader(
        selectedMonthLabel: String,
        isCurrentMonth: Boolean,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1F))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.btn_left),
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedMonthLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCurrentMonth) {
                Text(
                    text = stringResource(R.string.page_sub_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = onToday) {
                    Text(text = stringResource(R.string.today))
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(id = R.drawable.btn_right),
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}
```

- [ ] **Step 5: Gate the today highlight in `PrayerList`**

Replace the `PrayerList` function (lines 171–193):

```kotlin
@Composable
private fun PrayerList(
        monthlyPrayers: List<DailyPrayer>,
        currentDayOfMonth: Int,
        isCurrentMonth: Boolean,
        listState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(monthlyPrayers, key = { it.dayOfMonth }) { dailyPrayer ->
            val isToday = isCurrentMonth && dailyPrayer.dayOfMonth == currentDayOfMonth
            DailyPrayerCard(
                dailyPrayer = dailyPrayer,
                isToday = isToday
            )
        }
    }
}
```

- [ ] **Step 6: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt
git commit -m "feat(prayertimes): wire header month navigation controls in PrayerContainer"
```

---

### Task 8: Thread `onEvent` through the route and screen

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/navigation/PrayerTimesRoute.kt`
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesScreen.kt`

- [ ] **Step 1: Update `PrayerTimesRoute` — `LaunchedEffect(Unit)` initial load + pass `onEvent`**

Replace the entire contents of `PrayerTimesRoute.kt`:

```kotlin
package com.kutluoglu.prayer_feature.prayertimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kutluoglu.prayer_feature.prayertimes.PayerTimesScreen
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Created by F.K. on 24.12.2025.
 *
 */

@Composable
fun PrayerTimesRoute(
        viewModel: PrayerTimesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMonthlyPrayerTimes() }
    PayerTimesScreen(uiState = uiState, onEvent = viewModel::onEvent)
}
```

- [ ] **Step 2: Update `PayerTimesScreen` — accept and thread `onEvent`**

Replace the `PayerTimesScreen` function signature and the two `PrayerContainer(uiState)` call sites (lines 27–79):

```kotlin
@Composable
fun PayerTimesScreen(
    modifier: Modifier = Modifier,
    uiState: PrayerTimesUiState,
    onEvent: (PrayerTimesEvent) -> Unit
){
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.weight(0.43f),
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Card(
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    PrayerContainer(uiState, onEvent)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.fillMaxHeight(0.35f), // Top takes 30% of the height
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f) // The card column starts from the bottom and overlaps a bit
                        .align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        PrayerContainer(uiState, onEvent)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/navigation/PrayerTimesRoute.kt
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesScreen.kt
git commit -m "feat(prayertimes): thread onEvent through route and screen"
```

---

### Task 9: Full verification and housekeeping

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all tests pass (including the new `PrayerTimesViewModelTest`).

- [ ] **Step 2: Run the debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run `gitnexus_detect_changes()` (per AGENTS.md)**

Run: `gitnexus_detect_changes()` via the GitNexus MCP tool.
Expected: only `prayer_feature:prayertimes` symbols affected; no HIGH/CRITICAL risk.

- [ ] **Step 4: Update `TODO.md`**

Mark item 10 as done:

```markdown
- [x] **10. Monthly prayer times only for current month**
  - File: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`
  - No month navigation.
  - Status: DONE 2026-08-11 (TDD) — `PrayerTimesViewModel` now tracks `selectedMonth` (kotlinx-datetime `YearMonth`), caches loaded months in `monthCache`, and exposes `onEvent(PrayerTimesEvent)` (OnPreviousMonth/OnNextMonth/OnToday). `PrayerTimesUiState.Success` gains `selectedMonth` + `isCurrentMonth`; header arrows are clickable, a "Today" button appears when not on the current month, and today-highlight/auto-scroll are gated to the current month. Added `PrayerTimesViewModelTest` (5 tests, RED→GREEN). Full suite green.
```

- [ ] **Step 5: Commit**

```bash
git add TODO.md
git commit -m "docs: mark monthly prayer times navigation as done"
```

---

## Self-Review

**Spec coverage:**
- State & Events (`PrayerTimesEvent`, `selectedMonth`, `monthCache`, `isLoading`, `onEvent`, `loadMonthlyPrayerTimes` delegates to `loadMonth`) → Tasks 2, 5.
- UiState (`selectedMonth`, `isCurrentMonth`, `currentDayOfMonth` stays) → Task 3.
- Data flow & caching (cache hit → immediate Success; compute on miss; keep content while loading; `selectedMonth` updates immediately; `OnToday` → current month) → Task 5.
- UI changes (`TitleHeader` callbacks + label + Today button; `PrayerList` gated auto-scroll; `isToday` gated; `PrayerTimesRoute` `LaunchedEffect(Unit)` + `onEvent`; `PayerTimesScreen` threads `onEvent`) → Tasks 6, 7, 8.
- Testing (new `PrayerTimesViewModelTest`, JUnit5+MockK+Turbine+runTest, test deps in build.gradle.kts; all 6 spec cases covered by 5 tests) → Tasks 1, 4, 5.

**Placeholder scan:** No TBD/TODO/placeholders; every code step shows full code.

**Type consistency:** `PrayerTimesEvent` (sealed class, `data object` members) used consistently in ViewModel, Route, Screen, Container. `selectedMonth: YearMonth`, `isCurrentMonth: Boolean` consistent across UiState, ViewModel, Container. `onEvent: (PrayerTimesEvent) -> Unit` consistent across Route/Screen/Container. `month.onDay(day)`, `month.numberOfDays`, `LocalDate.yearMonth`, `plus/minus(Int, DateTimeUnit.MONTH)` all verified against kotlinx-datetime 0.7.1.
