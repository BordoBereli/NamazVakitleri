# PrayerContainer Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve `PrayerContainer.kt` maintainability and performance across 7 TDD-driven phases, each independently tested.

**Architecture:** Pure logic extracted into unit-testable helpers (icon map builder, scroll controller, weekday extractor); UI refactors locked by Compose UI tests (new infra); no public API changes to `getPrayerDrawableIdFrom` (CRITICAL risk, 8 consumers).

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (BOM 2025.10.00), JUnit 5 + MockK + Turbine + Truth, Compose UI test (androidTest).

**Impact summary (from GitNexus):** `PrayerContainer` LOW (1 caller); `getPrayerDrawableIdFrom` CRITICAL (8 consumers — signature preserved); `DailyPrayer` MEDIUM (22 importers — avoided, no model change); `PrayerTimesViewModel` LOW.

---

## Phase 0: Compose UI test infrastructure

**Files:**
- Modify: `prayer_feature/prayertimes/build.gradle.kts`
- Create: `prayer_feature/prayertimes/src/androidTest/java/com/kutluoglu/prayer_feature/prayertimes/PrayerContainerSmokeTest.kt`

- [ ] **Step 1: Add test dependencies** to `prayer_feature/prayertimes/build.gradle.kts` (after line 77, in the existing androidTest block):

```kotlin
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
```

- [ ] **Step 2: Write smoke test** (verifies infra compiles + runs):

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import org.junit.Rule
import org.junit.Test

class PrayerContainerSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loading state renders the loading indicator`() {
        composeRule.setContent {
            PrayerContainer(PrayerTimesUiState.Loading) {}
        }
        composeRule.onNodeWithText("Loading").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Run on emulator/device** (must have one connected):

```bash
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
```

Expected: PASS. If `LoadingIndicator` text differs, adjust the assertion to match `core/designsystem/.../CommonUi.kt`.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/prayertimes/build.gradle.kts prayer_feature/prayertimes/src/androidTest/
git commit -m "test(prayertimes): add Compose UI test infrastructure"
```

---

## Phase 1: P1 — Fix icon map caching (`PrayerIcon.kt`)

**Root cause:** `getPrayerDrawableIdFrom` uses `remember(prayerNames)` but `stringArrayResource()` returns a fresh array each call, so the map is rebuilt on every recomposition.

**Files:**
- Modify: `prayer_feature/common/build.gradle.kts` (add JUnit5 + Truth test deps)
- Modify: `prayer_feature/common/src/main/java/com/kutluoglu/prayer_feature/common/prayerUtils/PrayerIcon.kt`
- Create: `prayer_feature/common/src/test/java/com/kutluoglu/prayer_feature/common/prayerUtils/PrayerIconTest.kt`

- [ ] **Step 1: Add test deps** to `prayer_feature/common/build.gradle.kts` (after line 80):

```kotlin
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.truth)
```
And at the bottom: `tasks.withType<Test> { useJUnitPlatform() }`

- [ ] **Step 2: Write the failing test** `PrayerIconTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.common.prayerUtils

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.common.R as AppR
import org.junit.jupiter.api.Test

class PrayerIconTest {

    private val sixNames = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    @Test
    fun `builds a map for six localized prayer names`() {
        val map = buildPrayerIconMap(sixNames)
        assertThat(map).hasSize(6)
        assertThat(map["Fajr"]).isEqualTo(AppR.drawable.facr)
        assertThat(map["Sunrise"]).isEqualTo(AppR.drawable.sunrise)
        assertThat(map["Dhuhr"]).isEqualTo(AppR.drawable.dhuhr)
        assertThat(map["Asr"]).isEqualTo(AppR.drawable.asr)
        assertThat(map["Maghrib"]).isEqualTo(AppR.drawable.magrip)
        assertThat(map["Isha"]).isEqualTo(AppR.drawable.isha)
    }

    @Test
    fun `returns an empty map when fewer than six names are provided`() {
        assertThat(buildPrayerIconMap(listOf("Fajr", "Sunrise"))).isEmpty()
    }

    @Test
    fun `returns an empty map when the list is empty`() {
        assertThat(buildPrayerIconMap(emptyList())).isEmpty()
    }
}
```

- [ ] **Step 3: Run to verify it fails**

```bash
./gradlew :prayer_feature:common:testDebugUnitTest --tests="*PrayerIconTest"
```

Expected: FAIL — `buildPrayerIconMap` not defined.

- [ ] **Step 4: Implement** — add pure function + fix the composable in `PrayerIcon.kt`:

```kotlin
internal fun buildPrayerIconMap(prayerNames: List<String>): Map<String, Int> =
    if (prayerNames.size < 6) {
        emptyMap()
    } else {
        mapOf(
            prayerNames[0] to AppR.drawable.facr,
            prayerNames[1] to AppR.drawable.sunrise,
            prayerNames[2] to AppR.drawable.dhuhr,
            prayerNames[3] to AppR.drawable.asr,
            prayerNames[4] to AppR.drawable.magrip,
            prayerNames[5] to AppR.drawable.isha
        )
    }

@Composable
fun getPrayerDrawableIdFrom(prayerName: String): Int {
    val prayerNames = stringArrayResource(id = R.array.prayers)
    val prayerIconMap = remember { buildPrayerIconMap(prayerNames) }
    return prayerIconMap[prayerName] ?: AppR.drawable.facr
}
```

> Public signature unchanged — safe for the 8 consumers (PrayersHeader, home `PrayerCard`, `NextPrayerInfo`).

- [ ] **Step 5: Run to verify it passes**

```bash
./gradlew :prayer_feature:common:testDebugUnitTest --tests="*PrayerIconTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/common/build.gradle.kts prayer_feature/common/src/
git commit -m "perf(prayer): cache prayer icon map across recompositions"
```

---

## Phase 2: P3 — Scroll-to-today gating

**Root cause:** `LaunchedEffect(monthlyPrayers, isCurrentMonth)` re-runs `animateScrollToItem` on every list emission, yanking the user's scroll. Fix: scroll once per month entry. Also resolves M7 (position preserved across month switches).

**Files:**
- Create: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerListScrollController.kt`
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt:86-98`
- Create: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerListScrollControllerTest.kt`

- [ ] **Step 1: Write the failing test** `PrayerListScrollControllerTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.prayertimes.components

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import org.junit.jupiter.api.Test

class PrayerListScrollControllerTest {

    @Test
    fun `scrolls to today when entering the current month for the first time`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), isCurrentMonth = true, todayIndex = 16, itemCount = 31)
        assertThat(scrolled).containsExactly(16)
    }

    @Test
    fun `does not scroll again when the same month is re-emitted`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        assertThat(scrolled).containsExactly(16)
    }

    @Test
    fun `does not scroll when the month is not the current month`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 7), isCurrentMonth = false, todayIndex = 16, itemCount = 31)
        assertThat(scrolled).isEmpty()
    }

    @Test
    fun `does not scroll when the today index is out of range`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, todayIndex = 40, itemCount = 31)
        assertThat(scrolled).isEmpty()
    }

    @Test
    fun `scrolls again when returning to the current month after leaving it`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 7), false, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        assertThat(scrolled).containsExactly(16, 16)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerListScrollControllerTest"
```

Expected: FAIL — class not defined.

- [ ] **Step 3: Implement** — create `PrayerListScrollController.kt`:

```kotlin
package com.kutluoglu.prayer_feature.prayertimes.components

import kotlinx.datetime.YearMonth

class PrayerListScrollController(
    private val scrollToItem: suspend (Int) -> Unit
) {
    private var lastScrolledMonth: YearMonth? = null

    suspend fun onMonthChanged(
        month: YearMonth,
        isCurrentMonth: Boolean,
        todayIndex: Int,
        itemCount: Int
    ) {
        if (!isCurrentMonth) return
        if (lastScrolledMonth == month) return
        lastScrolledMonth = month
        if (todayIndex in 0 until itemCount) {
            scrollToItem(todayIndex)
        }
    }
}
```

- [ ] **Step 4: Wire into `PrayerContainer.kt`** — replace lines 86–98:

```kotlin
    val listState = rememberLazyListState()
    val scrollController = remember { PrayerListScrollController(listState::animateScrollToItem) }

    LaunchedEffect(selectedMonth) {
        scrollController.onMonthChanged(
            month = selectedMonth,
            isCurrentMonth = isCurrentMonth,
            todayIndex = currentDayOfMonth - 1,
            itemCount = monthlyPrayers.size
        )
    }
```

- [ ] **Step 5: Run tests + build**

```bash
./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerListScrollControllerTest"
./gradlew :prayer_feature:prayertimes:assembleDebug
```

Expected: PASS + build OK.

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/prayertimes/src/
git commit -m "perf(prayertimes): scroll to today once per month entry"
```

---

## Phase 3: P4 — Precompute weekday/time strings

**Root cause:** `nameOfMonth.split(" ").last()` (line 330) and `prayer.time.toString()` (line 282) allocate on every recomposition of every card. Also fixes misleading `nameOfMonth` param (it's actually the weekday from `"dd EEEE"`).

**Files:**
- Modify: `core/common/src/main/java/com/kutluoglu/core/common/utils/TimeFormatter.kt`
- Create: `core/common/src/test/java/com/kutluoglu/core/common/utils/WeekdayNameTest.kt`
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt`

- [ ] **Step 1: Write the failing test** `WeekdayNameTest.kt`:

```kotlin
package com.kutluoglu.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WeekdayNameTest {

    @Test
    fun `extracts weekday from day and name string`() {
        assertThat(extractWeekdayName("17 Monday")).isEqualTo("Monday")
    }

    @Test
    fun `returns input when no space is present`() {
        assertThat(extractWeekdayName("Monday")).isEqualTo("Monday")
    }

    @Test
    fun `returns empty string for empty input`() {
        assertThat(extractWeekdayName("")).isEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :core:common:test --tests="*WeekdayNameTest"
```

Expected: FAIL — `extractWeekdayName` not defined.

- [ ] **Step 3: Implement** — add to `TimeFormatter.kt`:

```kotlin
fun extractWeekdayName(gregorianDayAndName: String): String =
    gregorianDayAndName.substringAfterLast(' ')
```

- [ ] **Step 4: Run to verify it passes**

```bash
./gradlew :core:common:test --tests="*WeekdayNameTest"
```

Expected: PASS.

- [ ] **Step 5: Update `PrayerContainer.kt`** — cache per-item strings with `remember`:
  - In `DailyPrayerCard` (before `PrayerDateInfo` call, line ~257): `val weekdayName = remember(dailyPrayer.gregorianDate) { extractWeekdayName(dailyPrayer.gregorianDate) }` and pass `weekdayName = weekdayName`.
  - Rename `PrayerDateInfo` param `nameOfMonth` → `weekdayName` (lines 292, 259).
  - In `PrayersRow` item (line 280): `val timeText = remember(prayer) { prayer.time.toString() }` and use `text = timeText`.
  - Add import `com.kutluoglu.core.common.extractWeekdayName`.

- [ ] **Step 6: Verify build + existing tests**

```bash
./gradlew :prayer_feature:prayertimes:testDebugUnitTest
./gradlew :prayer_feature:prayertimes:assembleDebug
```

Expected: all existing tests PASS, build OK.

- [ ] **Step 7: Commit**

```bash
git add core/common/src/ prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt
git commit -m "perf(prayertimes): cache weekday and time strings per item"
```

---

## Phase 4: UI micro-refactors (P2, P5, M6, M1, M2)

**Files:**
- Create: `prayer_feature/prayertimes/src/androidTest/java/com/kutluoglu/prayer_feature/prayertimes/PrayerContainerTest.kt` (characterization tests)
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt`

- [ ] **Step 1: Write characterization UI tests** (lock current behavior before refactor):

```kotlin
package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test

class PrayerContainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val prayers = listOf(
        Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    private val dailyPrayers = (1..31).map { day ->
        DailyPrayer(
            dayOfMonth = day,
            gregorianDate = "$day August",
            hijriDate = "$day Muharram 1448",
            prayers = prayers
        )
    }

    private val successState = PrayerTimesUiState.Success(
        monthlyPrayers = dailyPrayers,
        currentDayOfMonth = 17,
        selectedMonth = YearMonth(2026, 8),
        isCurrentMonth = true,
        timeState = TimeUiState(gregorianShortDate = "August 2026"),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    @Test
    fun `success state renders month label and all prayer names in header`() {
        composeRule.setContent { PrayerContainer(successState) {} }
        composeRule.onNodeWithText("August 2026").assertIsDisplayed()
        prayers.forEach { prayer ->
            composeRule.onNodeWithText(prayer.name).assertIsDisplayed()
        }
    }

    @Test
    fun `success state renders all prayer times for the first day`() {
        composeRule.setContent { PrayerContainer(successState) {} }
        prayers.forEach { prayer ->
            composeRule.onNodeWithText(prayer.time.toString()).assertIsDisplayed()
        }
    }

    @Test
    fun `error state renders the error message`() {
        composeRule.setContent { PrayerContainer(PrayerTimesUiState.Error("boom")) {} }
        composeRule.onNodeWithText("boom").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify they pass** (characterization — locks current behavior)

```bash
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
```

Expected: PASS.

- [ ] **Step 3: Refactor `PrayerContainer.kt`** (no behavior change):
  - `PrayersHeader` (line 177): `LazyRow` → `Row`; make it `private`.
  - `PrayersRow` (line 275): `LazyRow` → `Row`.
  - Remove the redundant double background — keep only the `LazyColumn` background (line 219), drop the `Column` background (line 102).
  - `DailyPrayerCard` (lines 238–247): compute `val cardShape = RoundedCornerShape(12.dp)` once, reuse for `border` and `shape`.
  - Extract magic numbers to file-level private constants at the top of the file, e.g.:
    ```kotlin
    private val CardCornerSize = 12.dp
    private val HeaderBackgroundAlpha = 0.1F
    private val TodayBorderAlpha = 0.7F
    private val HeaderSurfaceAlpha = 0.5f
    private val TodayHijriAlpha = 0.7f
    ```
    Replace the literals with these constants.

- [ ] **Step 4: Re-run UI tests + build**

```bash
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
./gradlew :prayer_feature:prayertimes:assembleDebug
```

Expected: PASS + build OK.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/prayertimes/src/
git commit -m "refactor(prayertimes): replace lazy rows with rows, dedupe shapes and constants"
```

---

## Phase 5: M3 — Split `PrayerTimesContent`

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt`

- [ ] **Step 1: Refactor** — extract two focused composables (keep `PrayerTimesContent` as orchestrator holding the scroll controller):
  - `PrayerTimesHeader(selectedMonthLabel, isCurrentMonth, prayers, onPrevious, onNext, onToday)` — combines `TitleHeader` + `PrayersHeader`.
  - `PrayerTimesList(monthlyPrayers, currentDayOfMonth, isCurrentMonth, listState)` — the `LazyColumn` (moved from `PrayerList`).
  - `PrayerTimesContent` body becomes: `Column { PrayerTimesHeader(...); PrayerTimesList(...) }`.

- [ ] **Step 2: Re-run UI tests + build**

```bash
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
./gradlew :prayer_feature:prayertimes:assembleDebug
```

Expected: PASS + build OK.

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt
git commit -m "refactor(prayertimes): split PrayerTimesContent into header and list composables"
```

---

## Phase 6: M4 — Previews + M5 — container behavior UI tests

**Files:**
- Modify: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/components/PrayerContainer.kt`
- Modify: `prayer_feature/prayertimes/src/androidTest/java/com/kutluoglu/prayer_feature/prayertimes/PrayerContainerTest.kt`

- [ ] **Step 1: Add `@Preview` composables** (per AGENTS.md — top-level, `@Composable` + `@Preview`) at the bottom of `PrayerContainer.kt`:

```kotlin
@Preview(showBackground = true)
@Composable
private fun PrayerContainerSuccessPreview() {
    PrayerContainer(
        uiState = PrayerTimesUiState.Success(
            monthlyPrayers = emptyList(),
            currentDayOfMonth = 1,
            selectedMonth = YearMonth(2026, 8),
            isCurrentMonth = true,
            timeState = TimeUiState(gregorianShortDate = "August 2026"),
            locationState = LocationUiState(
                locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
                locationInfoText = "Istanbul, TR"
            )
        ),
        onEvent = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PrayerContainerErrorPreview() {
    PrayerContainer(uiState = PrayerTimesUiState.Error("Failed to load"), onEvent = {})
}
```

- [ ] **Step 2: Add behavior UI tests** to `PrayerContainerTest.kt` (event emission):

```kotlin
    @Test
    fun `clicking next month emits OnNextMonth`() {
        val events = mutableListOf<PrayerTimesEvent>()
        composeRule.setContent { PrayerContainer(successState) { events.add(it) } }
        composeRule.onNodeWithContentDescription("Next month").performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnNextMonth)
    }

    @Test
    fun `clicking previous month emits OnPreviousMonth`() {
        val events = mutableListOf<PrayerTimesEvent>()
        composeRule.setContent { PrayerContainer(successState) { events.add(it) } }
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnPreviousMonth)
    }

    @Test
    fun `non-current month shows Today button that emits OnToday`() {
        val events = mutableListOf<PrayerTimesEvent>()
        val otherMonth = successState.copy(isCurrentMonth = false)
        composeRule.setContent { PrayerContainer(otherMonth) { events.add(it) } }
        composeRule.onNodeWithText("Today").performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnToday)
    }
```
Add imports: `androidx.compose.ui.test.onNodeWithContentDescription`, `androidx.compose.ui.test.performClick`, `com.google.common.truth.Truth.assertThat`.

- [ ] **Step 3: Run UI tests + build**

```bash
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
./gradlew :prayer_feature:prayertimes:assembleDebug
```

Expected: PASS + build OK.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/prayertimes/src/
git commit -m "test(prayertimes): add previews and container behavior UI tests"
```

---

## Phase 7: M7 — Scroll UX

Resolved by Phase 2: `PrayerListScrollController` preserves scroll position across month switches (only auto-scrolls to today when entering the current month), matching the chosen "keep current position" behavior. **No code change required.** Verify with `gitnexus_detect_changes()` that the full refactor only touches expected symbols.

---

## Final verification (after all phases)

```bash
./gradlew allTests
./gradlew :prayer_feature:prayertimes:connectedDebugAndroidTest
gitnexus_detect_changes()   # confirm only expected symbols affected
```
