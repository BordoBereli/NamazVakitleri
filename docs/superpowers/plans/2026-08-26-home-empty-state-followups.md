# Home Empty State Follow-up Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the four follow-up issues from the home empty-state feature: launch flash, bypassed permission UI, missing interaction test, and missing analytics.

**Architecture:** (1) `LocationPager` shows `LoadingIndicator` (existing designsystem composable) instead of the empty state while the gate is `Loading`. (2) `PermissionActions` gains a `permanentlyDenied` flag; `HomeEmptyContent` shows a hint when permission is permanently denied; `HomeScreen` routes "Use My Location" through a testable `resolveUseMyLocationAction` (granted → event, permanently denied → open settings, else → request permission). (3) Robolectric tests cover the granted/denied interaction. (4) `OnUseMyLocation` logs the existing `USE_MY_LOCATION` analytics event.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, JUnit 5 + MockK + Truth, Robolectric for UI tests.

**Spec:** `docs/superpowers/specs/2026-08-26-home-empty-state-followups-design.md`

---

## File Structure

| File | Responsibility | Action |
|------|----------------|--------|
| `prayer_feature/home/.../LocationPager.kt` | Loading branch + `permissionDenied` param | Modify |
| `core/designsystem/.../components/PermissionHandler.kt` | `PermissionActions.permanentlyDenied` | Modify |
| `prayer_feature/home/.../components/HomeEmptyContent.kt` | `permissionDenied` param + hint text | Modify |
| `prayer_feature/home/.../HomeScreen.kt` | `resolveUseMyLocationAction` + `openAppSettings` + wiring | Modify |
| `prayer_feature/home/.../HomeViewModel.kt` | Log `USE_MY_LOCATION` | Modify |
| `prayer_feature/home/.../res/values/strings.xml` | `permission_denied_hint` (default) | Modify |
| `prayer_feature/home/.../res/values-*/strings.xml` | `permission_denied_hint` (14 locales) | Modify |
| `prayer_feature/home/.../HomeScreenTest.kt` | Loading + hint + interaction tests | Modify |
| `prayer_feature/home/.../HomeScreenActionsTest.kt` | `resolveUseMyLocationAction` branch tests | Create |
| `prayer_feature/home/.../HomeViewModelTest.kt` | Analytics test | Modify |

---

## Task 1: Loading indicator instead of empty-state flash

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the failing test**

In `HomeScreenTest.kt`, add after the `renders empty state with add location and use my location` test:

```kotlin
@Test
fun `renders loading indicator when entries empty and state is loading`() {
    composeTestRule.setContent {
        HomeScreen(
            navController = mockk<NavController>(relaxed = true),
            uiState = HomeUiState.Loading,
            locationsState = LocationsState(),
            prayerDataByLocation = emptyMap(),
            activeLocationId = null,
            quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
            onEvent = {}
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Add location").assertDoesNotExist()
    composeTestRule.onNodeWithText("Use My Location").assertDoesNotExist()
}
```

Add the import (with the other compose test imports):

```kotlin
import androidx.compose.ui.test.assertDoesNotExist
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: FAIL — `renders loading indicator when entries empty and state is loading` fails because "Add location" IS displayed (current code shows `HomeEmptyContent` whenever `entries.isEmpty()`).

- [ ] **Step 3: Implement the loading branch**

In `LocationPager.kt`, replace the empty-state branch (currently lines 57-63):

```kotlin
    if (entries.isEmpty()) {
        HomeEmptyContent(
            onAddLocation = onAddLocation,
            onUseMyLocation = onUseMyLocation
        )
        return
    }
```

with:

```kotlin
    if (entries.isEmpty()) {
        if (uiState is HomeUiState.Loading) {
            LoadingIndicator()
        } else {
            HomeEmptyContent(
                onAddLocation = onAddLocation,
                onUseMyLocation = onUseMyLocation
            )
        }
        return
    }
```

Add the import (with the other `com.kutluoglu.core.designsystem` imports):

```kotlin
import com.kutluoglu.core.designsystem.components.LoadingIndicator
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: PASS (all three HomeScreen tests green).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "fix(home): show loading indicator instead of empty state flash on launch"
```

---

## Task 2: `PermissionActions.permanentlyDenied` + `HomeEmptyContent` hint

**Files:**
- Modify: `core/designsystem/src/main/java/com/kutluoglu/core/designsystem/components/PermissionHandler.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt`
- Modify: `prayer_feature/home/src/main/res/values/strings.xml`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Add the default-locale string**

In `prayer_feature/home/src/main/res/values/strings.xml`, add before the closing `</resources>` tag (after `use_my_location`):

```xml
    <string name="permission_denied_hint">Location permission is off. Use My Location will open settings.</string>
```

- [ ] **Step 2: Write the failing test**

In `HomeScreenTest.kt`, add after the `renders loading indicator when entries empty and state is loading` test:

```kotlin
@Test
fun `renders permission denied hint when permissionDenied is true`() {
    composeTestRule.setContent {
        HomeEmptyContent(
            onAddLocation = {},
            onUseMyLocation = {},
            permissionDenied = true
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Location permission is off. Use My Location will open settings.").assertIsDisplayed()
}
```

Add the import (with the other `com.kutluoglu.prayer_feature.home` imports):

```kotlin
import com.kutluoglu.prayer_feature.home.components.HomeEmptyContent
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: COMPILATION FAILURE — `HomeEmptyContent` has no `permissionDenied` parameter.

- [ ] **Step 4: Add `permanentlyDenied` to `PermissionActions`**

In `PermissionHandler.kt`, change the `PermissionActions` data class (currently lines 40-43):

```kotlin
data class PermissionActions(
    val allPermissionsGranted: Boolean,
    val requestPermission: () -> Unit
)
```

to:

```kotlin
data class PermissionActions(
    val allPermissionsGranted: Boolean,
    val permanentlyDenied: Boolean,
    val requestPermission: () -> Unit
)
```

In `ShowOf` (currently lines 126-133), change the content call to pass `permanentlyDenied`:

```kotlin
        permissionState.allPermissionsGranted || canProceedWithoutPermission -> {
            content(
                PermissionActions(
                    allPermissionsGranted = permissionState.allPermissionsGranted,
                    permanentlyDenied = !permissionState.allPermissionsGranted &&
                        !permissionState.shouldShowRationale &&
                        permissionResultReceived,
                    requestPermission = { permissionState.launchMultiplePermissionRequest() }
                )
            )
        }
```

- [ ] **Step 5: Add `permissionDenied` param + hint to `HomeEmptyContent`**

In `HomeEmptyContent.kt`, change the signature and body:

```kotlin
@Composable
fun HomeEmptyContent(
    onAddLocation: () -> Unit,
    onUseMyLocation: () -> Unit,
    permissionDenied: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.no_location_selected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddLocation) {
                Text(stringResource(R.string.add_location))
            }
            OutlinedButton(onClick = onUseMyLocation) {
                Text(stringResource(R.string.use_my_location))
            }
            if (permissionDenied) {
                Text(
                    text = stringResource(R.string.permission_denied_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: PASS (all four HomeScreen tests green).

- [ ] **Step 7: Commit**

```bash
git add core/designsystem/src/main/java/com/kutluoglu/core/designsystem/components/PermissionHandler.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt prayer_feature/home/src/main/res/values/strings.xml prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "feat(home): expose permanentlyDenied permission state and show settings hint"
```

---

## Task 3: `HomeScreen` wiring — `resolveUseMyLocationAction`, `openAppSettings`, interaction tests

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenActionsTest.kt` (create)
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the failing unit tests for `resolveUseMyLocationAction`**

Create `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenActionsTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HomeScreenActionsTest {

    @Test
    fun `granted permission triggers use my location`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = true,
            permanentlyDenied = false,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("use")
    }

    @Test
    fun `permanently denied opens settings`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = false,
            permanentlyDenied = true,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("settings")
    }

    @Test
    fun `not granted and not denied requests permission`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = false,
            permanentlyDenied = false,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("request")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenActionsTest"`
Expected: COMPILATION FAILURE — `resolveUseMyLocationAction` does not exist.

- [ ] **Step 3: Write the failing interaction tests**

In `HomeScreenTest.kt`, add after the `renders permission denied hint when permissionDenied is true` test:

```kotlin
@Test
fun `use my location with permission granted fires OnUseMyLocation`() {
    shadowOf(composeTestRule.activity).grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val events = mutableListOf<HomeEvent>()
    composeTestRule.setContent {
        HomeScreen(
            navController = mockk<NavController>(relaxed = true),
            uiState = HomeUiState.Empty,
            locationsState = LocationsState(),
            prayerDataByLocation = emptyMap(),
            activeLocationId = null,
            quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
            onEvent = { events.add(it) }
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Use My Location").performClick()
    composeTestRule.waitForIdle()
    assertThat(events).contains(HomeEvent.OnUseMyLocation)
}

@Test
fun `use my location without permission requests permission and does not fire OnUseMyLocation`() {
    shadowOf(composeTestRule.activity).denyPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val events = mutableListOf<HomeEvent>()
    composeTestRule.setContent {
        HomeScreen(
            navController = mockk<NavController>(relaxed = true),
            uiState = HomeUiState.Empty,
            locationsState = LocationsState(),
            prayerDataByLocation = emptyMap(),
            activeLocationId = null,
            quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
            onEvent = { events.add(it) }
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Use My Location").performClick()
    composeTestRule.waitForIdle()
    assertThat(events).doesNotContain(HomeEvent.OnUseMyLocation)
    assertThat(shadowOf(composeTestRule.activity).lastRequestedPermission.requestedPermissions)
        .asList()
        .contains(Manifest.permission.ACCESS_FINE_LOCATION)
}
```

Add the imports to `HomeScreenTest.kt`:

```kotlin
import android.Manifest
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.robolectric.Shadows.shadowOf
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: FAIL — `use my location with permission granted fires OnUseMyLocation` fails because the current `onUseMyLocation` wiring calls `requestPermission()` when not granted, and the granted path is not yet wired through `resolveUseMyLocationAction`.

- [ ] **Step 5: Implement `resolveUseMyLocationAction` and wire `HomeScreen`**

In `HomeScreen.kt`:

1. Add a top-level function at the bottom of the file (after the `HomeScreen` composable):

```kotlin
internal fun resolveUseMyLocationAction(
    allPermissionsGranted: Boolean,
    permanentlyDenied: Boolean,
    onUseMyLocation: () -> Unit,
    openSettings: () -> Unit,
    requestPermission: () -> Unit
) {
    when {
        allPermissionsGranted -> onUseMyLocation()
        permanentlyDenied -> openSettings()
        else -> requestPermission()
    }
}
```

2. Add imports:

```kotlin
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
```

3. Add `openAppSettings` and rewire `onUseMyLocation` inside the `HomeScreen` composable. Add `val context = LocalContext.current` after the `onChooseLocation` lambda (line 49), then change the `PermissionHandler` content block (currently lines 56-72):

```kotlin
        ) { permissionActions ->
            val openAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    this.data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
            LocationPager(
                entries = locationsState.entries,
                selectedId = locationsState.selectedId,
                activeLocationId = activeLocationId,
                uiState = uiState,
                prayerDataByLocation = prayerDataByLocation,
                quranVerseFormatter = quranVerseFormatter,
                onPrayerTimesClick = onPrayerTimesClick,
                onAddLocation = onAddLocation,
                onChooseLocation = onChooseLocation,
                onUseMyLocation = {
                    resolveUseMyLocationAction(
                        allPermissionsGranted = permissionActions.allPermissionsGranted,
                        permanentlyDenied = permissionActions.permanentlyDenied,
                        onUseMyLocation = { onEvent(HomeEvent.OnUseMyLocation) },
                        openSettings = openAppSettings,
                        requestPermission = permissionActions.requestPermission
                    )
                },
                permissionDenied = permissionActions.permanentlyDenied,
                onEvent = onEvent
            )
        }
```

- [ ] **Step 6: Add `permissionDenied` param to `LocationPager`**

In `LocationPager.kt`, add the parameter to the signature (after `onUseMyLocation`, currently line 54):

```kotlin
    onUseMyLocation: () -> Unit,
    permissionDenied: Boolean,
    onEvent: (HomeEvent) -> Unit
```

and pass it to `HomeEmptyContent` in the empty-state branch:

```kotlin
            HomeEmptyContent(
                onAddLocation = onAddLocation,
                onUseMyLocation = onUseMyLocation,
                permissionDenied = permissionDenied
            )
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest" --tests="*HomeScreenActionsTest"`
Expected: PASS (all HomeScreen + HomeScreenActions tests green).

- [ ] **Step 8: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenActionsTest.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "feat(home): route use my location through permission-aware action"
```

---

## Task 4: Log `USE_MY_LOCATION` analytics event

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

In `HomeViewModelTest.kt`, add after the `OnUseMyLocation triggers loadPrayerTimesForCurrentLocation` test (currently ends at line ~293):

```kotlin
@Test
fun `OnUseMyLocation logs use my location analytics event`() = runTest {
    coEvery { locationsCoordinator.observeState() } returns flowOf(
        LocationsState(entries = listOf(entry), selectedId = "loc-1")
    )
    coEvery { locationsCoordinator.resolveInitial() } returns location
    coEvery { locationsCoordinator.resolveSelected() } returns location
    coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET, any()) } returns success(loadedData())

    val vm = viewModel()
    vm.onEvent(HomeEvent.OnUseMyLocation)

    verify { analyticsTracker.logEvent(AnalyticsEvents.USE_MY_LOCATION) }
}
```

Add the import (with the other `com.kutluoglu.core.common` imports):

```kotlin
import com.kutluoglu.core.common.analytics.AnalyticsEvents
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: FAIL — `OnUseMyLocation logs use my location analytics event` fails because `logEvent(USE_MY_LOCATION)` is never called.

- [ ] **Step 3: Implement the analytics logging**

In `HomeViewModel.kt`, change the `OnUseMyLocation` branch in `onEvent` (currently line 150):

```kotlin
            HomeEvent.OnUseMyLocation -> loadPrayerTimesForCurrentLocation()
```

to:

```kotlin
            HomeEvent.OnUseMyLocation -> {
                analyticsTracker.logEvent(AnalyticsEvents.USE_MY_LOCATION)
                loadPrayerTimesForCurrentLocation()
            }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: PASS (all ViewModel tests green).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt
git commit -m "feat(home): log use my location analytics event"
```

---

## Task 5: Localize `permission_denied_hint`

**Files:**
- Modify: all 14 locale files under `prayer_feature/home/src/main/res/` (`values-ar`, `values-bn`, `values-de`, `values-es`, `values-fa`, `values-fr`, `values-hi`, `values-id`, `values-ms`, `values-ru`, `values-ta`, `values-th`, `values-tr`, `values-ur`)

- [ ] **Step 1: Add the string to each locale file**

For each of the 14 locale files, add this line just before the closing `</resources>` tag (after the `use_my_location` entry), using the translation below:

| Locale | `permission_denied_hint` |
|--------|--------------------------|
| `values-ar` | `إذن الموقع مغلق. سيؤدي استخدام موقعي إلى فتح الإعدادات.` |
| `values-bn` | `অবস্থানের অনুমতি বন্ধ। আমার অবস্থান ব্যবহার করলে সেটিংস খুলবে।` |
| `values-de` | `Standortberechtigung ist deaktiviert. Meinen Standort verwenden öffnet die Einstellungen.` |
| `values-es` | `El permiso de ubicación está desactivado. Usar mi ubicación abrirá la configuración.` |
| `values-fa` | `مجوز موقعیت غیرفعال است. استفاده از موقعیت من تنظیمات را باز میکند.` |
| `values-fr` | `L'autorisation de localisation est désactivée. Utiliser ma localisation ouvrira les paramètres.` |
| `values-hi` | `स्थान अनुमति बंद है। मेरा स्थान उपयोग करें से सेटिंग्स खुलेंगी।` |
| `values-id` | `Izin lokasi nonaktif. Gunakan Lokasi Saya akan membuka pengaturan.` |
| `values-ms` | `Kebenaran lokasi dimatikan. Gunakan Lokasi Saya akan membuka tetapan.` |
| `values-ru` | `Разрешение на местоположение отключено. Использовать моё местоположение откроет настройки.` |
| `values-ta` | `இருப்பிட அனுமதி முடக்கப்பட்டுள்ளது. என் இருப்பிடத்தைப் பயன்படுத்து அமைப்புகளைத் திறக்கும்.` |
| `values-th` | `ปิดสิทธิ์ตำแหน่งแล้ว การใช้ตำแหน่งของฉันจะเปิดการตั้งค่า` |
| `values-tr` | `Konum izni kapalı. Konumumu Kullan ayarları açacak.` |
| `values-ur` | `مقام کی اجازت بند ہے۔ میرا مقام استعمال کریں سیٹنگز کھولے گا۔` |

Example — for `values-tr/strings.xml`, add before `</resources>`:

```xml
    <string name="permission_denied_hint">Konum izni kapalı. Konumumu Kullan ayarları açacak.</string>
```

- [ ] **Step 2: Verify the build compiles**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/res/
git commit -m "feat(home): localize permission denied hint"
```

---

## Task 6: Full verification

- [ ] **Step 1: Run the home module unit tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. (Note: `unitTests` is NOT a real Gradle task in this project — it is only an AGP `testOptions` config block. Use `testDebugUnitTest`.)

- [ ] **Step 3: Run GitNexus change detection**

Run: `gitnexus_detect_changes()` (repo: `NamazVakitleri`)
Expected: only home empty-state symbols and `PermissionHandler` affected; no unexpected execution flows.

- [ ] **Step 4: Commit any remaining changes**

```bash
git status
git add -A
git commit -m "chore(home): verify empty state follow-up fixes"
```
