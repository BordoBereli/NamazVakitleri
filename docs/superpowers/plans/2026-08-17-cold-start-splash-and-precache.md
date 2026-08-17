# Cold Start: Splash Screen + Tomorrow Pre-Cache — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the white flash on cold start with a dark splash screen (Fix 1) and remove the first-launch-of-day placeholder by pre-caching tomorrow's prayer times whenever today's are calculated (Fix 2).

**Architecture:** Fix 1 uses `androidx.core:core-splashscreen` with a dark `Theme.NamazVakitleri.Starting` theme (library attrs for API 26–30, platform attrs for API 31+), a dark window background on the base theme, and `installSplashScreen()` in `MainActivity`. Fix 2 adds a `preCacheTomorrow()` call in `PrayerDataStoreImp.getPrayerTimes` that calculates and caches the next day's data under tomorrow's cache key whenever today's cache misses, so the first launch of a new day hits the cache.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, androidx.core:core-splashscreen 1.0.1, DataStore Preferences, kotlinx-datetime 0.7.1, JUnit 5 + MockK + Truth + kotlinx-coroutines-test.

---

## File Structure

**Modified files:**

| File | Responsibility |
|------|----------------|
| `gradle/libs.versions.toml` | Add `coreSplashscreen` version + `androidx-core-splashscreen` library |
| `app/build.gradle.kts` | Add `implementation(libs.androidx.core.splashscreen)` |
| `app/src/main/res/values/colors.xml` | Add `splash_background` color (`#FF121212`) |
| `app/src/main/res/values/themes.xml` | Dark base theme + `Theme.NamazVakitleri.Starting` (library attrs) |
| `app/src/main/res/values-v31/themes.xml` | Create — `Theme.NamazVakitleri.Starting` (platform attrs, API 31+) |
| `app/src/main/AndroidManifest.xml` | Point `MainActivity` at `Theme.NamazVakitleri.Starting` |
| `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt` | Call `installSplashScreen()` before `super.onCreate()` |
| `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt` | Pre-cache tomorrow's prayer times on cache miss |

**Test files:**

| File | Responsibility |
|------|----------------|
| `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt` | Verify tomorrow's data is calculated + cached on today's cache miss |

---

## Task 1: Add `core-splashscreen` dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version + library to the version catalog**

In `gradle/libs.versions.toml`, add to the `[versions]` block (after `coreKtx = "1.17.0"`):

```toml
coreSplashscreen = "1.0.1"
```

Add to the `[libraries]` block (after `androidx-core-ktx`):

```toml
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
```

- [ ] **Step 2: Add the dependency to the app module**

In `app/build.gradle.kts`, add to the `dependencies` block (after `implementation(libs.androidx.core.ktx)`):

```kotlin
implementation(libs.androidx.core.splashscreen)
```

- [ ] **Step 3: Verify the build**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add core-splashscreen dependency"
```

---

## Task 2: Create the dark splash theme and fix the base theme

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values-v31/themes.xml`

- [ ] **Step 1: Add the splash background color**

In `app/src/main/res/values/colors.xml`, add before the closing `</resources>`:

```xml
<color name="splash_background">#FF121212</color>
```

- [ ] **Step 2: Replace the base theme + add the starting theme**

Replace the entire contents of `app/src/main/res/values/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.NamazVakitleri" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/splash_background</item>
    </style>

    <style name="Theme.NamazVakitleri.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/splash_background</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.NamazVakitleri</item>
    </style>
</resources>
```

- [ ] **Step 3: Create the API 31+ variant**

Create `app/src/main/res/values-v31/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.NamazVakitleri.Starting" parent="Theme.SplashScreen">
        <item name="android:windowSplashScreenBackground">@color/splash_background</item>
        <item name="android:windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.NamazVakitleri</item>
    </style>
</resources>
```

- [ ] **Step 4: Verify the build**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/themes.xml app/src/main/res/values-v31/themes.xml
git commit -m "theme: add dark splash screen theme and dark window background"
```

---

## Task 3: Wire up the splash screen in the manifest and `MainActivity`

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt`

- [ ] **Step 1: Point the launcher activity at the starting theme**

In `app/src/main/AndroidManifest.xml`, change the `MainActivity` theme from `@style/Theme.NamazVakitleri` to `@style/Theme.NamazVakitleri.Starting`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.NamazVakitleri.Starting">
```

- [ ] **Step 2: Call `installSplashScreen()` in `MainActivity`**

Replace the entire contents of `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt` with:

```kotlin
package com.kutluoglu.namazvakitleri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kutluoglu.core.designsystem.theme.NamazVakitleriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamazVakitleriTheme(darkTheme = true) {
               MainAppScreen()
            }
        }
    }
}
```

Note: `installSplashScreen()` MUST be called before `super.onCreate(savedInstanceState)`.

- [ ] **Step 3: Verify the build**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt
git commit -m "feat: install splash screen on app start"
```

---

## Task 4: Verify Fix 1 on a device/emulator (manual)

**Files:** none

- [ ] **Step 1: Install and cold-start the app**

Run:
```bash
./gradlew :app:installDebug
```
Then force-stop the app (`adb shell am force-stop com.kutluoglu.namazvakitleri`) and relaunch it from the launcher.

Expected: The cold start shows a **dark** splash (`#121212`) with the launcher icon — no white flash. The transition from splash to the home screen is seamless (both dark).

If no device/emulator is available, skip this task and rely on the build succeeding; note it for the user to verify manually.

---

## Task 5: Fix 2 — write the failing test for tomorrow pre-cache

**Files:**
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`

- [ ] **Step 1: Add the `DateTimeUnit` import**

In `prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt`, add to the imports (after `import kotlinx.coroutines.test.runTest`):

```kotlin
import kotlinx.datetime.DateTimeUnit
```

- [ ] **Step 2: Write the failing test**

Append to `PrayerDataStoreImpTest.kt` before the closing brace:

```kotlin
@Test
fun `getPrayerTimes pre-caches tomorrow's prayers on cache miss`() = runTest {
    val testDate = LocalDateTime.createBy(2024, 1, 1)
    val zoneId = ZoneId.of("Europe/Istanbul")
    val todayPrayers = listOf(
        Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
    )
    val tomorrowPrayers = listOf(
        Prayer("Fajr", "الفجر", LocalTime.parse("05:01"), testDate.date.plus(1, DateTimeUnit.DAY))
    )
    coEvery { prayerTimesCache.get(any()) } returns null
    coEvery {
        prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
    } returnsMany listOf(todayPrayers, tomorrowPrayers)

    val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

    assertThat(result).isEqualTo(todayPrayers)
    coVerify(exactly = 1) {
        prayerCalculationService.calculateDailyPrayerTimes(
            41.0, 29.0, zoneId, testDate,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
        )
    }
    coVerify(exactly = 1) {
        prayerCalculationService.calculateDailyPrayerTimes(
            41.0, 29.0, zoneId, testDate.plus(1, DateTimeUnit.DAY),
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
        )
    }
    coVerify(exactly = 1) {
        prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul", todayPrayers)
    }
    coVerify(exactly = 1) {
        prayerTimesCache.put("2024-01-02|41.0|29.0|Europe/Istanbul", tomorrowPrayers)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:
```bash
./gradlew :prayer:data:testDebugUnitTest --tests="*PrayerDataStoreImpTest"
```
Expected: FAIL — `coVerify(exactly = 1)` for `prayerTimesCache.put("2024-01-02|...", ...)` fails because tomorrow is not pre-cached yet.

---

## Task 6: Fix 2 — implement tomorrow pre-cache in `PrayerDataStoreImp`

**Files:**
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`

- [ ] **Step 1: Add the datetime imports**

In `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt`, add to the imports (after `import kotlinx.coroutines.withContext`):

```kotlin
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
```

Note: `LocalDateTime.plus(Int, DateTimeUnit)` does NOT exist in kotlinx-datetime 0.7.1 — only `LocalDate.plus` does. Tomorrow must be built via `date.date.plus(1, DateTimeUnit.DAY).atTime(...)`, and both `plus` and `atTime` are extensions that require explicit imports (matches the existing convention in `PrayerLogicEngine.kt:10`).

- [ ] **Step 2: Implement the pre-cache**

In `PrayerDataStoreImp.kt`, modify `getPrayerTimes` to call `preCacheTomorrow(...)` after caching today's data, and add the private `preCacheTomorrow` method:

```kotlin
override suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
): List<Prayer> {
    val cacheKey = buildCacheKey(date, latitude, longitude, zoneId)
    prayerTimesCache.get(cacheKey)?.let { return it }

    val calculated = withContext(Dispatchers.Default) {
        prayerCalculationService.calculateDailyPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            date = date,
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD
        )
    }
    prayerTimesCache.put(cacheKey, calculated)
    preCacheTomorrow(date, latitude, longitude, zoneId)
    return calculated
}

private suspend fun preCacheTomorrow(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
) {
    val tomorrow = date.date.plus(1, DateTimeUnit.DAY)
        .atTime(date.hour, date.minute, date.second, date.nanosecond)
    val tomorrowKey = buildCacheKey(tomorrow, latitude, longitude, zoneId)
    if (prayerTimesCache.get(tomorrowKey) != null) return
    val tomorrowPrayers = withContext(Dispatchers.Default) {
        prayerCalculationService.calculateDailyPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            date = tomorrow,
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD
        )
    }
    prayerTimesCache.put(tomorrowKey, tomorrowPrayers)
}
```

- [ ] **Step 3: Run the new test to verify it passes**

Run:
```bash
./gradlew :prayer:data:testDebugUnitTest --tests="*PrayerDataStoreImpTest"
```
Expected: PASS

- [ ] **Step 4: Run the full data module test suite**

Run:
```bash
./gradlew :prayer:data:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` (existing tests — cache hit, cache miss, off-main-thread, monthly — must still pass; the pre-cache only adds a second calculation/put with a different date/key, so the existing `coVerify(exactly = 1)` assertions with the specific today date/key remain valid).

- [ ] **Step 5: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/PrayerDataStoreImpTest.kt
git commit -m "feat: pre-cache tomorrow's prayer times on cache miss"
```

### Review refinements (applied after final code review)

The final review flagged three issues in the synchronous pre-cache, all fixed in commit `695c9d3` (`fix: make tomorrow pre-cache best-effort and off the critical path`):

1. **Best-effort** — `preCacheTomorrow` is wrapped in `runCatching` + `Log.e` so a pre-cache failure can never fail the primary `getPrayerTimes` request.
2. **Off the critical path** — `preCacheTomorrow` is now non-suspend and launches fire-and-forget in an injected `@Named("preCacheScope")` `CoroutineScope` (provided in `PrayerDataModule` as `CoroutineScope(SupervisorJob() + Dispatchers.Default)`), so the caller never blocks on tomorrow's calc/read/write.
3. **Pre-cache on both hit and miss** — `getPrayerTimes` calls `preCacheTomorrow` on the cache-hit path too, so the first-launch-of-day placeholder is fully eliminated (not just every other day).

The pre-cache calc runs on the scope's `Dispatchers.Default` (no redundant `withContext`). Tests were updated: `PrayerDataStoreImpTest` injects `CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)` for deterministic eager execution, mocks `Log`, captures the first calc call's thread in the off-main-thread test, and adds a failure-resilience test (`695c9d3`) plus a pre-cache-on-hit test (`a74137c`).

---

## Task 7: Full verification

**Files:** none

- [ ] **Step 1: Run the entire unit test suite**

Run:
```bash
./gradlew testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify the change scope with GitNexus**

Run `gitnexus_detect_changes()` (scope `unstaged`). Confirm the affected symbols are limited to:
- `PrayerDataStoreImp` (data layer)
- `MainActivity` / splash theme resources (app layer)

No unexpected cross-module consumers should be affected.

- [ ] **Step 3: Final build**

Run:
```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`
