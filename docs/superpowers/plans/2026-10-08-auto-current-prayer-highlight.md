# Auto Current-Prayer Highlight Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Highlight the current prayer on both Android Auto screens — `●` marker + `CarColor.PRIMARY` span on the time text — and add a current-prayer row to `NextPrayerScreen`.

**Architecture:** Extend `SurfacePrayerData` with a defaulted `currentPrayerName: String?` field populated by `PrayerSurfaceDataProvider` from `result.currentPrayer?.name`. Both Auto screens consume it: `PrayerTimesScreen` highlights the matching row; `NextPrayerScreen` gains a current-prayer row above the next-prayer row. All changes are additive; existing rows render unchanged.

**Tech Stack:** Kotlin 2.2.20, AndroidX Car App library 1.7.0 (`ForegroundCarColorSpan`, `CarColor`, `CarText`), JUnit 5 + MockK (domain), JUnit 4 + Robolectric + MockK + Truth (auto), kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-08-auto-current-prayer-highlight-design.md`

---

### Task 1: Domain — expose `currentPrayerName` on `SurfacePrayerData`

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/SurfacePrayerData.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerSurfaceDataProvider.kt:71-87`
- Test: `prayer/domain/src/test/java/com/kutluoglu/prayer/domain/PrayerSurfaceDataProviderTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `PrayerSurfaceDataProviderTest.kt` (after the existing tests, before the closing brace). This file uses JUnit 5, MockK, `runTest`, and the helpers `prayer(name, time, date = LocalDate(2026, 10, 5))` and `appSettings()`:

```kotlin
    @Test
    fun `load exposes current prayer name`() = runTest {
        val dailyLoader = mockk<DailyPrayerTimesLoader>(relaxed = true)
        val locationSource = mockk<SurfaceLocationSource>(relaxed = true)
        val settings = mockk<SettingsProvider>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-10-05T08:00:00Z"), ZoneOffset.UTC)

        coEvery { locationSource.resolveSelected() } returns LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih")
        coEvery { settings.getSettings() } returns appSettings()
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            DailyPrayerTimes(
                prayers = listOf(prayer("Imsak", LocalTime(5, 47)), prayer("Dhuhr", LocalTime(12, 58))),
                currentPrayer = prayer("Imsak", LocalTime(5, 47)),
                nextPrayer = prayer("Dhuhr", LocalTime(12, 58)),
                currentPrayerEpochMillis = 1L,
                nextPrayerEpochMillis = 2L,
                isJumuah = false
            )
        )

        val provider = PrayerSurfaceDataProvider(dailyLoader, locationSource, settings, clock)
        val result = provider.load()

        assertTrue(result != null)
        assertEquals("Imsak", result!!.currentPrayerName)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:domain:test --console=plain -q`
Expected: FAIL — compile error, unresolved reference `currentPrayerName`.

- [ ] **Step 3: Add the field to `SurfacePrayerData`**

In `SurfacePrayerData.kt`, add the field after `tomorrowImsakTime` (line 29) and extend the KDoc's last sentence:

```kotlin
    val nextPrayerDate: LocalDate,
    val tomorrowImsakTime: LocalTime? = null,
    val currentPrayerName: String? = null
```

KDoc: change the final sentence to:

```
 * localized prayer list. [tomorrowImsakTime] is only populated after Isha,
 * when the next prayer falls on tomorrow's date. [currentPrayerName] is the
 * name of the prayer currently in progress, or null when unknown.
```

- [ ] **Step 4: Populate it in `PrayerSurfaceDataProvider.load()`**

In `PrayerSurfaceDataProvider.kt`, inside the `SurfacePrayerData(...)` constructor call (after `tomorrowImsakTime = tomorrowImsakTime`, line 85), add:

```kotlin
            currentPrayerName = result.currentPrayer?.name
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer:domain:test --console=plain -q`
Expected: PASS — all tests green (existing 7 + new 1).

- [ ] **Step 6: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/domain/SurfacePrayerData.kt \
  prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerSurfaceDataProvider.kt \
  prayer/domain/src/test/java/com/kutluoglu/prayer/domain/PrayerSurfaceDataProviderTest.kt
git commit -m "feat: expose currentPrayerName on SurfacePrayerData"
```

---

### Task 2: `PrayerTimesScreen` — highlight the current prayer row

**Files:**
- Modify: `prayer_auto/src/main/java/com/kutluoglu/prayer_auto/screen/PrayerTimesScreen.kt:100-112`
- Test: `prayer_auto/src/test/java/com/kutluoglu/prayer_auto/screen/PrayerTimesScreenTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `PrayerTimesScreenTest.kt` (after the existing tests, before the closing brace). The file already has `today`, `tomorrow`, `surfaceData(tomorrowImsakTime)`, `screenWith(data)`, and `rows(screen)` helpers. The existing `surfaceData` fixture has `nextPrayerName = "Imsak"` and `currentPrayerLocalTime = LocalTime(20, 0)` (Isha) — the current prayer is Isha.

Add the imports at the top of the file (merge with existing imports):

```kotlin
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText
import androidx.car.app.model.ForegroundCarColorSpan
```

Add the tests:

```kotlin
    @Test
    fun `current prayer row has marker and primary-colored time`() {
        val data = surfaceData(tomorrowImsakTime = LocalTime(5, 48))
        val rows = rows(screenWith(data))

        val current = rows.first { it.title.toString().contains("Isha") }
        assertThat(current.title.toString()).startsWith("●")
        val span = current.texts.single().spans.single().carSpan
        assertThat(span).isInstanceOf(ForegroundCarColorSpan::class.java)
        assertThat((span as ForegroundCarColorSpan).color).isEqualTo(CarColor.PRIMARY)
    }

    @Test
    fun `non-current rows have no marker and no color span`() {
        val data = surfaceData(tomorrowImsakTime = LocalTime(5, 48))
        val rows = rows(screenWith(data))

        val dhuhr = rows.first { it.title.toString().contains("Dhuhr") }
        assertThat(dhuhr.title.toString()).doesNotContain("●")
        assertThat(dhuhr.texts.single().spans).isEmpty()
    }
```

Note: `CarText.getSpans()` returns `List<CarText.SpanWrapper>`; `SpanWrapper.getCarSpan()` returns the `CarSpan`. Both are public API on car-app 1.7.0 (verified against the AAR bytecode). `CarText.spans` and `SpanWrapper.carSpan` are the Kotlin property accesses.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_auto:test --tests "*PrayerTimesScreenTest*" --console=plain -q`
Expected: FAIL — `current prayer row has marker...` fails (no `●` prefix, no span); `non-current rows...` passes pre-change (acceptable — it locks in the invariant).

- [ ] **Step 3: Implement the highlight in `onGetTemplate()`**

In `PrayerTimesScreen.kt`, replace the today-list rendering block (lines 100-112):

```kotlin
        val today = LocalDate.now(clock).toKotlinLocalDate()
        val isAfterIsha = data.nextPrayerDate > today
        val list = ItemList.Builder()
        data.prayers.forEach { p ->
            val isNext = p.name == data.nextPrayerName && p.date == data.nextPrayerDate
            val isCurrent = p.name == data.currentPrayerName && p.date == today
            val title = when {
                isCurrent -> "● ${p.name}"
                isNext -> "▶ ${p.name}"
                else -> p.name
            }
            val timeText: CharSequence = if (isCurrent) {
                CarText.Builder(
                    ForegroundCarColorSpan.create(CarColor.PRIMARY).applyTo(formatClockTime(p.time))
                ).build()
            } else {
                formatClockTime(p.time)
            }
            list.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(timeText)
                    .build()
            )
        }
```

Add the imports at the top of the file:

```kotlin
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText
import androidx.car.app.model.ForegroundCarColorSpan
import android.text.SpannableString
```

And add this private helper at the bottom of the class (before the closing brace):

```kotlin
    private fun ForegroundCarColorSpan.applyTo(text: String): SpannableString {
        val spannable = SpannableString(text)
        spannable.setSpan(this, 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_auto:test --console=plain -q`
Expected: PASS — all tests green (existing 9 + new 2).

- [ ] **Step 5: Commit**

```bash
git add prayer_auto/src/main/java/com/kutluoglu/prayer_auto/screen/PrayerTimesScreen.kt \
  prayer_auto/src/test/java/com/kutluoglu/prayer_auto/screen/PrayerTimesScreenTest.kt
git commit -m "feat: highlight current prayer on Auto PrayerTimesScreen"
```

---

### Task 3: `NextPrayerScreen` — add current-prayer row

**Files:**
- Modify: `prayer_auto/src/main/java/com/kutluoglu/prayer_auto/screen/NextPrayerScreen.kt:95-105`
- Test: `prayer_auto/src/test/java/com/kutluoglu/prayer_auto/screen/NextPrayerScreenTest.kt` (create)

- [ ] **Step 1: Write the failing tests**

Create `prayer_auto/src/test/java/com/kutluoglu/prayer_auto/screen/NextPrayerScreenTest.kt`, following the `PrayerTimesScreenTest` pattern (MockK relaxed `CarContext`, reflection-set `surface` field, fixed `Clock`):

```kotlin
package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer.domain.SurfacePrayerData
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class NextPrayerScreenTest {

    private val today = LocalDate(2026, 10, 5)

    private fun surfaceData(currentPrayerName: String?): SurfacePrayerData = SurfacePrayerData(
        location = LocationData(41.0082, 28.9784, "Türkiye", "TR", "İstanbul", "İstanbul", "Europe/Istanbul"),
        city = "İstanbul",
        district = null,
        nextPrayerName = "Isha",
        nextPrayerTime = "20:00",
        nextPrayerLocalTime = LocalTime(20, 0),
        currentPrayerLocalTime = if (currentPrayerName != null) LocalTime(18, 45) else null,
        nextPrayerEpochMillis = 0L,
        currentPrayerEpochMillis = 0L,
        isJumuah = false,
        hijriAdjustment = 0,
        prayers = listOf(
            Prayer("Maghrib", "المغرب", LocalTime(18, 45), today),
            Prayer("Isha", "العشاء", LocalTime(20, 0), today),
        ),
        nextPrayerDate = today,
        currentPrayerName = currentPrayerName
    )

    private fun screenWith(data: SurfacePrayerData?): NextPrayerScreen {
        val carContext = mockk<CarContext>(relaxed = true)
        every { carContext.getString(any()) } returns "Isha"
        val screen = NextPrayerScreen(
            carContext,
            mockk<PrayerSurfaceDataProvider>(relaxed = true),
            Clock.fixed(Instant.parse("2026-10-05T19:00:00Z"), ZoneOffset.UTC)
        )
        if (data != null) {
            NextPrayerScreen::class.java
                .getDeclaredField("surface")
                .apply { isAccessible = true }
                .set(screen, data)
        }
        return screen
    }

    private fun rows(screen: NextPrayerScreen): List<Row> {
        val template = screen.onGetTemplate()
        assertThat(template).isInstanceOf(PaneTemplate::class.java)
        val pane = (template as PaneTemplate).pane
        assertThat(pane).isNotNull()
        return pane!!.rows.filterIsInstance<Row>()
    }

    @Test
    fun `pane shows current prayer row above next prayer row`() {
        val rows = rows(screenWith(surfaceData(currentPrayerName = "Maghrib")))

        assertThat(rows).hasSize(2)
        assertThat(rows[0].title.toString()).startsWith("●")
        assertThat(rows[0].title.toString()).contains("Maghrib")
        val span = rows[0].texts.single().spans.single().carSpan
        assertThat(span).isInstanceOf(ForegroundCarColorSpan::class.java)
        assertThat((span as ForegroundCarColorSpan).color).isEqualTo(CarColor.PRIMARY)
    }

    @Test
    fun `pane omits current prayer row when current prayer is unknown`() {
        val rows = rows(screenWith(surfaceData(currentPrayerName = null)))

        assertThat(rows).hasSize(1)
        assertThat(rows[0].title.toString()).doesNotContain("●")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_auto:test --tests "*NextPrayerScreenTest*" --console=plain -q`
Expected: FAIL — `pane shows current prayer row...` fails (`rows` has size 1, no current-prayer row); `pane omits...` passes pre-change (acceptable — it locks in the invariant). The test compiles because Task 1 already added the `currentPrayerName` field.

- [ ] **Step 3: Implement the current-prayer row in `onGetTemplate()`**

In `NextPrayerScreen.kt`, replace the pane-building block (lines 98-105):

```kotlin
        val rows = Pane.Builder()
        data.currentPrayerName?.let { currentName ->
            data.currentPrayerLocalTime?.let { currentTime ->
                val spannable = SpannableString(formatClockTime(currentTime))
                spannable.setSpan(
                    ForegroundCarColorSpan.create(CarColor.PRIMARY),
                    0,
                    spannable.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                rows.addRow(
                    Row.Builder()
                        .setTitle("● $currentName")
                        .addText(spannable)
                        .build()
                )
            }
        }
        val row = Row.Builder()
            .setTitle("$title --> $remaining ${carContext.getString(R.string.auto_remaining_suffix)}")
        if (isTomorrow) {
            row.addText(carContext.getString(R.string.auto_tomorrow_morning))
        }
        rows.addRow(row.build())
```

Add the imports at the top of the file:

```kotlin
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import android.text.SpannableString
import com.kutluoglu.prayer.domain.formatClockTime
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_auto:test --console=plain -q`
Expected: PASS — all tests green (existing 9 + 2 from Task 2 + new 2).

- [ ] **Step 5: Commit**

```bash
git add prayer_auto/src/main/java/com/kutluoglu/prayer_auto/screen/NextPrayerScreen.kt \
  prayer_auto/src/test/java/com/kutluoglu/prayer_auto/screen/NextPrayerScreenTest.kt
git commit -m "feat: show current prayer row on Auto NextPrayerScreen"
```

---

### Task 4: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run all affected module tests**

Run: `./gradlew :prayer:domain:test :prayer_auto:test :prayer_widget:test :wear:test --console=plain -q`
Expected: PASS, exit 0.

- [ ] **Step 2: Build the touched modules (includes lint)**

Run: `./gradlew :prayer:domain:build :prayer_auto:build --console=plain -q`
Expected: PASS, exit 0.

- [ ] **Step 3: Graph change analysis before merge**

Run: `node .gitnexus/run.cjs detect-changes --scope all --repo .` from the worktree root.
Expected: no unexpected side effects; additive-only change confirmed.
