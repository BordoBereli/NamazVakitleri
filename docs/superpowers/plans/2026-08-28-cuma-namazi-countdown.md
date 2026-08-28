# Cuma Namazı Countdown Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On Fridays, show "Cuma" instead of the localized Dhuhr/Öğle name in the home screen "`xxx namazına kalan süre`" section and in the persistent countdown notification, whenever the countdown targets the noon prayer.

**Architecture:** Two isolated label changes. (1) Home screen: add a testable `PrayerUiState.isJumuahCountdown()` helper (detects next prayer = Dhuhr via stable `arabicName` + Friday via `date.dayOfWeek`) and use it in the `NextPrayerInfo` composable with a new localized `prayer_jumuah` string. (2) Notifications: in `PrayerNotificationManager.showCountdownNotification`, when the raw key is `"Dhuhr"` and the target time's date is Friday, reuse the existing localized `notification_jumuah_title` string. The countdown value is unchanged (Cuma is at Dhuhr time).

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.datetime, JUnit 5 (home) / JUnit 4+Robolectric (notifications), Truth, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-28-cuma-namazi-countdown-design.md`

---

### Task 1: Add `PrayerUiState.isJumuahCountdown()` helper (TDD)

**Files:**
- Create: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/state/PrayerUiStateTest.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`

- [ ] **Step 1: Write the failing test**

Create `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/state/PrayerUiStateTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.state

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test

class PrayerUiStateTest {

    private fun prayer(name: String, arabicName: String, date: LocalDate) =
        Prayer(name = name, arabicName = arabicName, time = LocalTime(12, 30), date = date)

    @Test
    fun `isJumuahCountdown true when next prayer is Dhuhr on Friday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("Öğle", "الظهر", LocalDate(2026, 8, 28)) // Friday
        )
        assertThat(state.isJumuahCountdown()).isTrue()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is Dhuhr on Monday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("Öğle", "الظهر", LocalDate(2026, 8, 24)) // Monday
        )
        assertThat(state.isJumuahCountdown()).isFalse()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is Asr on Friday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("İkindi", "العصر", LocalDate(2026, 8, 28)) // Friday
        )
        assertThat(state.isJumuahCountdown()).isFalse()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is null`() {
        val state = PrayerUiState(nextPrayer = null)
        assertThat(state.isJumuahCountdown()).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerUiStateTest"`
Expected: FAIL — compilation error `unresolved reference: isJumuahCountdown`

- [ ] **Step 3: Implement the helper**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`:

Add the import (after the existing `com.kutluoglu.prayer_feature.common.states.TimeUiState` import):

```kotlin
import kotlinx.datetime.DayOfWeek
```

Add a private constant at the top of the file (after the imports, before `sealed class HomeUiState`):

```kotlin
private const val DHUHR_ARABIC_NAME = "الظهر"
```

Change the `PrayerUiState` data class to:

```kotlin
data class PrayerUiState(
        val prayers: List<Prayer> = emptyList(),
        val currentPrayer: Prayer? = null,
        val nextPrayer: Prayer? = null
) {
    fun isJumuahCountdown(): Boolean =
        nextPrayer?.let { it.arabicName == DHUHR_ARABIC_NAME && it.date.dayOfWeek == DayOfWeek.FRIDAY } ?: false
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*PrayerUiStateTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/state/PrayerUiStateTest.kt
git commit -m "feat(home): add isJumuahCountdown helper to PrayerUiState"
```

---

### Task 2: Add `prayer_jumuah` string to all 15 home locale files

**Files:** Modify all 15 files in `prayer_feature/home/src/main/res/`:
`values/strings.xml`, `values-ar/strings.xml`, `values-bn/strings.xml`, `values-de/strings.xml`, `values-es/strings.xml`, `values-fa/strings.xml`, `values-fr/strings.xml`, `values-hi/strings.xml`, `values-id/strings.xml`, `values-ms/strings.xml`, `values-ru/strings.xml`, `values-ta/strings.xml`, `values-th/strings.xml`, `values-tr/strings.xml`, `values-ur/strings.xml`

In each file, insert the `prayer_jumuah` line **immediately after** the `time_until_sunrise` line (which is line 9 in every file). The exact line to add per file:

| File | Line to insert |
|------|----------------|
| `values/strings.xml` | `<string name="prayer_jumuah">Jumu\'ah</string>` |
| `values-ar/strings.xml` | `<string name="prayer_jumuah">جمعة</string>` |
| `values-bn/strings.xml` | `<string name="prayer_jumuah">জুমু\'আহ</string>` |
| `values-de/strings.xml` | `<string name="prayer_jumuah">Jumu\'ah</string>` |
| `values-es/strings.xml` | `<string name="prayer_jumuah">Yumu\'ah</string>` |
| `values-fa/strings.xml` | `<string name="prayer_jumuah">جمعه</string>` |
| `values-fr/strings.xml` | `<string name="prayer_jumuah">Jumu\'ah</string>` |
| `values-hi/strings.xml` | `<string name="prayer_jumuah">जुमुआ</string>` |
| `values-id/strings.xml` | `<string name="prayer_jumuah">Jumu\'ah</string>` |
| `values-ms/strings.xml` | `<string name="prayer_jumuah">Jumaat</string>` |
| `values-ru/strings.xml` | `<string name="prayer_jumuah">Джума</string>` |
| `values-ta/strings.xml` | `<string name="prayer_jumuah">ஜுமுஆ</string>` |
| `values-th/strings.xml` | `<string name="prayer_jumuah">ญุมุอะฮ์</string>` |
| `values-tr/strings.xml` | `<string name="prayer_jumuah">Cuma</string>` |
| `values-ur/strings.xml` | `<string name="prayer_jumuah">جمعہ</string>` |

Example — in `values-tr/strings.xml`, the block becomes:

```xml
    <string name="time_until_sunrise">Güneş\'in doğmasına kalan süre: </string>
    <string name="prayer_jumuah">Cuma</string>
```

- [ ] **Step 1: Add the string to all 15 files** (as above)
- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no resource errors)

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/res/
git commit -m "feat(home): add prayer_jumuah string in all locales"
```

---

### Task 3: Use `isJumuahCountdown()` in `NextPrayerInfo`

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeTopContainer.kt:124-135`

- [ ] **Step 1: Update the display-name logic**

In `HomeTopContainer.kt`, change the `NextPrayerInfo` composable's display-name block from:

```kotlin
    val nextPrayerDisplayName = when (nextPrayerNameRaw) {
        "İmsak" -> "Sabah"
        else -> nextPrayerNameRaw
    }
```

to:

```kotlin
    val nextPrayerDisplayName = when {
        prayerState.isJumuahCountdown() -> stringResource(id = R.string.prayer_jumuah)
        nextPrayerNameRaw == "İmsak" -> "Sabah"
        else -> nextPrayerNameRaw
    }
```

No other changes to the composable — the existing `timeUntilText` logic (which checks `nextPrayerNameRaw == "Güneş"`) and the countdown value display stay as-is. `stringResource` and `R` are already imported.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run the home test suite**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS (all existing home tests still green)

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeTopContainer.kt
git commit -m "feat(home): show Cuma label in countdown on Fridays"
```

---

### Task 4: Show "Cuma" in the countdown notification (TDD)

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`
- Modify: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these tests to `PrayerNotificationManagerTest.kt` (before the closing brace of the class):

```kotlin
    @Test
    fun `showCountdownNotification shows Cuma for Dhuhr on Friday`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(12, 30).atDate(LocalDate.of(2026, 8, 28)) // Friday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Dhuhr", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Cuma · 12:30")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `showCountdownNotification shows Dhuhr name for Dhuhr on Monday`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(12, 30).atDate(LocalDate.of(2026, 8, 24)) // Monday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Dhuhr", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Öğle · 12:30")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `showCountdownNotification shows Maghrib on Friday for non-Dhuhr`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 28)) // Friday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Akşam · 18:45")
        } finally {
            Locale.setDefault(original)
        }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest"`
Expected: FAIL — the Friday-Dhuhr test expects `Cuma · 12:30` but gets `Öğle · 12:30`

- [ ] **Step 3: Implement the Jumu'ah detection**

Modify `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`:

Add the import (after the existing `java.time.Instant` import):

```kotlin
import java.time.DayOfWeek
```

Change the title builder inside `showCountdownNotification` from:

```kotlin
            .setContentTitle(
                localizedString(
                    R.string.notification_countdown_title,
                    localizedPrayerName(nextPrayerName),
                    formatClockTime(nextPrayerTimeMillis)
                )
            )
```

to:

```kotlin
            .setContentTitle(
                localizedString(
                    R.string.notification_countdown_title,
                    countdownDisplayName(nextPrayerName, nextPrayerTimeMillis),
                    formatClockTime(nextPrayerTimeMillis)
                )
            )
```

Add these two private functions to the class (e.g. right after `localizedPrayerName`):

```kotlin
    private fun countdownDisplayName(nextPrayerName: String, nextPrayerTimeMillis: Long): String =
        if (nextPrayerName == "Dhuhr" && isFriday(nextPrayerTimeMillis)) {
            localizedString(R.string.notification_jumuah_title)
        } else {
            localizedPrayerName(nextPrayerName)
        }

    private fun isFriday(epochMillis: Long): Boolean =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .dayOfWeek == DayOfWeek.FRIDAY
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest"`
Expected: PASS (all tests, including the 3 new ones and the existing `showCountdownNotification shows prayer name and clock time in title`)

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt
git commit -m "feat(notifications): show Cuma in countdown notification on Fridays"
```

---

### Task 5: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run both affected module test suites**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest :prayer_notifications:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Run gitnexus change detection**

Run: `gitnexus_detect_changes()` (via the GitNexus MCP tool, scope `all`)
Expected: only `PrayerUiState`, `HomeTopContainer.NextPrayerInfo`, `PrayerNotificationManager.showCountdownNotification` (and their tests) affected; no unexpected execution flows.

- [ ] **Step 4: Update TODO.md if it tracks this feature** (check `TODO.md`; add a line marking the Cuma countdown label as done if the file tracks home/notification features)
