# Home Empty State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a dedicated full-screen empty state on the home page when no locations exist, with "Add Location" and "Use My Location" actions.

**Architecture:** Extends the existing gate pattern — a new `HomeScreenGate.Empty` / `HomeUiState.Empty` state is set by the ViewModel whenever there is no active location, mapped through `mergeToHomeUiState`, and rendered by a new `HomeEmptyContent` composable. The shared `PermissionHandler` (only consumer: `HomeScreen`) is extended to expose permission actions so "Use My Location" can request permission on demand.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, JUnit 5 + MockK + Truth, Robolectric for UI tests.

**Spec:** `docs/superpowers/specs/2026-08-26-home-empty-state-design.md`

---

## File Structure

| File | Responsibility | Action |
|------|----------------|--------|
| `prayer_feature/home/.../state/HomeScreenGate.kt` | Gate states (Loading/Error/Empty/Ready) | Modify |
| `prayer_feature/home/.../state/HomeUiStates.kt` | `HomeUiState` sealed class | Modify |
| `prayer_feature/home/.../state/HomeUiStateMerger.kt` | Maps gate → UI state | Modify |
| `prayer_feature/home/.../HomeEvent.kt` | User events | Modify |
| `prayer_feature/home/.../HomeViewModel.kt` | Sets `Empty` when no location; handles `OnUseMyLocation` | Modify |
| `core/designsystem/.../components/PermissionHandler.kt` | Exposes `PermissionActions` to content | Modify |
| `prayer_feature/home/.../components/HomeEmptyContent.kt` | Empty state UI | Create |
| `prayer_feature/home/.../LocationPager.kt` | Renders empty state when `entries.isEmpty()` | Modify |
| `prayer_feature/home/.../HomeScreen.kt` | Wires permission actions + empty state | Modify |
| `prayer_feature/home/src/main/res/values*/strings.xml` | `add_location`, `use_my_location` strings | Modify |
| `prayer_feature/home/.../state/HomeUiStateMergerTest.kt` | Merger tests | Modify |
| `prayer_feature/home/.../HomeViewModelTest.kt` | ViewModel tests | Modify |
| `prayer_feature/home/.../HomeScreenTest.kt` | Robolectric UI tests | Modify |

---

## Task 1: State layer — `Empty` gate and UI state

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeScreenGate.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMergerTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `HomeUiStateMergerTest.kt` (after the `merge with Error gate returns HomeUiState Error with message` test):

```kotlin
@Test
fun `merge with Empty gate returns HomeUiState Empty`() {
    val result = mergeToHomeUiState(
        gate = HomeScreenGate.Empty,
        location = null,
        time = null,
        prayer = null,
        countdown = countdown,
        quran = quran
    )
    assertThat(result).isEqualTo(HomeUiState.Empty)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeUiStateMergerTest"`
Expected: COMPILATION FAILURE — `HomeScreenGate.Empty` and `HomeUiState.Empty` do not exist yet.

- [ ] **Step 3: Add `Empty` to `HomeScreenGate`**

In `HomeScreenGate.kt`, change the sealed interface to:

```kotlin
sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Empty : HomeScreenGate
    data object Ready : HomeScreenGate
}
```

- [ ] **Step 4: Add `Empty` to `HomeUiState`**

In `HomeUiStates.kt`, add `Empty` after `Loading`:

```kotlin
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(
        val timeState: TimeUiState = TimeUiState(),
        val prayerState: PrayerUiState = PrayerUiState(),
        val locationState: LocationUiState,
        val countdownState: CountdownUiState = CountdownUiState(),

        val quranVerse: AyahData? = null,
        val isVerseDetailSheetVisible: Boolean = false
    ) : HomeUiState()
}
```

- [ ] **Step 5: Map `Empty` in `mergeToHomeUiState`**

In `HomeUiStateMerger.kt`, change the `when (gate)` block to:

```kotlin
    return when (gate) {
        HomeScreenGate.Loading -> HomeUiState.Loading
        HomeScreenGate.Empty -> HomeUiState.Empty
        is HomeScreenGate.Error -> HomeUiState.Error(gate.message)
        HomeScreenGate.Ready -> {
            if (time == null || prayer == null || location == null) {
                HomeUiState.Loading
            } else {
                HomeUiState.Success(
                    timeState = time,
                    prayerState = prayer,
                    locationState = location,
                    countdownState = countdown,
                    quranVerse = quran.verse,
                    isVerseDetailSheetVisible = quran.isSheetVisible
                )
            }
        }
    }
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeUiStateMergerTest"`
Expected: PASS (all merger tests green).

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeScreenGate.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMergerTest.kt
git commit -m "feat(home): add Empty gate and UI state for no-location case"
```

---

## Task 2: ViewModel — set `Empty` for no-location, add `OnUseMyLocation`

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: Add `OnUseMyLocation` event**

In `HomeEvent.kt`, add the event after `OnPermissionsGranted`:

```kotlin
sealed interface HomeEvent {
    object OnRefresh : HomeEvent
    object OnPermissionsGranted : HomeEvent
    object OnUseMyLocation : HomeEvent
    object OnLoadQuranVerse : HomeEvent
    object OnVerseClicked : HomeEvent
    object OnVerseDetailDismissed : HomeEvent
    data class OnLocationSelected(val locationId: String) : HomeEvent
}
```

- [ ] **Step 2: Write the failing tests**

In `HomeViewModelTest.kt`:

1. Replace the existing `empty locations state after initial emission shows error` test (lines ~242-255) with:

```kotlin
@Test
fun `empty locations state after initial emission shows empty state`() = runTest {
    val stateFlow = MutableStateFlow(
        LocationsState(entries = listOf(entry), selectedId = "loc-1")
    )
    coEvery { locationsCoordinator.observeState() } returns stateFlow
    coEvery { locationsCoordinator.resolveInitial() } returns location
    coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET, any()) } returns success(loadedData())

    val vm = viewModel()
    stateFlow.value = LocationsState()

    assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Empty)
}
```

2. Add these three new tests (place after the test above):

```kotlin
@Test
fun `resolveInitial returns null sets Empty`() = runTest {
    coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
    coEvery { locationsCoordinator.resolveInitial() } returns null

    val vm = viewModel()

    assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Empty)
}

@Test
fun `loadPrayerTimesForCurrentLocation with no location sets Empty`() = runTest {
    coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
    coEvery { locationsCoordinator.resolveInitial() } returns location
    coEvery { locationsCoordinator.resolveSelected() } returns null
    coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET, any()) } returns success(loadedData())

    val vm = viewModel()
    vm.loadPrayerTimesForCurrentLocation()

    assertThat(vm.screenGate.value).isEqualTo(HomeScreenGate.Empty)
}

@Test
fun `OnUseMyLocation triggers loadPrayerTimesForCurrentLocation`() = runTest {
    coEvery { locationsCoordinator.observeState() } returns flowOf(
        LocationsState(entries = listOf(entry), selectedId = "loc-1")
    )
    coEvery { locationsCoordinator.resolveInitial() } returns location
    coEvery { locationsCoordinator.resolveSelected() } returns location
    coEvery { prayerTimesLoader.load(location, CalculationMethod.TURKEY_DIYANET, any()) } returns success(loadedData())

    val vm = viewModel()
    vm.onEvent(HomeEvent.OnUseMyLocation)

    coVerify { locationsCoordinator.resolveSelected() }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: FAIL — the empty-state tests assert `HomeScreenGate.Empty` but the ViewModel still sets `Error`; `OnUseMyLocation` is not handled so `resolveSelected` is never called.

- [ ] **Step 4: Implement `noLocation()` and replace `fail()` calls**

In `HomeViewModel.kt`:

1. Add a helper next to `fail(...)` (line ~325):

```kotlin
private fun noLocation() {
    _screenGate.value = HomeScreenGate.Empty
}
```

2. Replace these four `fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))` calls with `noLocation()`:
   - In `loadInitialLocation()` (line ~177)
   - In `loadPrayerTimesForCurrentLocation()` when `activeId == null` (line ~208)
   - In `loadPrayerTimesForCurrentLocation()` when `location == null` (line ~211)
   - In `handleState()` when `activeId == null` (line ~225)

   Do NOT change the `fail(...)` calls that represent real load failures (lines ~203-205 and ~239).

3. Handle the new event in `onEvent(...)` — add after the `HomeEvent.OnPermissionsGranted` branch:

```kotlin
HomeEvent.OnUseMyLocation -> loadPrayerTimesForCurrentLocation()
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeViewModelTest"`
Expected: PASS (all ViewModel tests green).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt
git commit -m "feat(home): set Empty gate when no location and add OnUseMyLocation event"
```

---

## Task 3: UI — `PermissionHandler`, `HomeEmptyContent`, `LocationPager`, `HomeScreen`

**Files:**
- Modify: `core/designsystem/src/main/java/com/kutluoglu/core/designsystem/components/PermissionHandler.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- Modify: `prayer_feature/home/src/main/res/values/strings.xml` (default locale — needed so the test compiles)
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Add default-locale strings**

In `prayer_feature/home/src/main/res/values/strings.xml`, add before the closing `</resources>` tag (after `no_location_selected`, line ~133):

```xml
    <string name="add_location">Add location</string>
    <string name="use_my_location">Use My Location</string>
```

- [ ] **Step 2: Write the failing test**

In `HomeScreenTest.kt`, add after the existing `renders error state with retry` test:

```kotlin
@Test
fun `renders empty state with add location and use my location`() {
    composeTestRule.setContent {
        HomeScreen(
            navController = mockk<NavController>(relaxed = true),
            uiState = HomeUiState.Empty,
            locationsState = LocationsState(),
            prayerDataByLocation = emptyMap(),
            activeLocationId = null,
            quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
            onEvent = {}
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Add location").assertIsDisplayed()
    composeTestRule.onNodeWithText("Use My Location").assertIsDisplayed()
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: FAIL — the empty state UI does not exist yet.

- [ ] **Step 4: Expose `PermissionActions` from `PermissionHandler`**

In `PermissionHandler.kt`:

1. Add a `PermissionActions` data class at the top of the file (after the imports):

```kotlin
data class PermissionActions(
    val allPermissionsGranted: Boolean,
    val requestPermission: () -> Unit
)
```

2. Change the `PermissionHandler` signature — `content` now receives `PermissionActions`:

```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onChooseLocation: (() -> Unit)? = null,
    canProceedWithoutPermission: Boolean = false,
    content: @Composable (PermissionActions) -> Unit
) {
```

3. Change `ShowOf` to pass `PermissionActions` when showing content:

```kotlin
@Composable
@OptIn(ExperimentalPermissionsApi::class)
private fun ShowOf(
        permissionState: MultiplePermissionsState,
        onChooseLocation: (() -> Unit)?,
        canProceedWithoutPermission: Boolean,
        permissionResultReceived: Boolean,
        content: @Composable (PermissionActions) -> Unit
) {
    when {
        // If all permissions are granted, or the user can proceed without them
        // (e.g. already has a manually selected location), display the main content.
        permissionState.allPermissionsGranted || canProceedWithoutPermission -> {
            content(
                PermissionActions(
                    allPermissionsGranted = permissionState.allPermissionsGranted,
                    requestPermission = { permissionState.launchMultiplePermissionRequest() }
                )
            )
        }

        // If rationale should be shown, display the rationale UI.
        permissionState.shouldShowRationale   -> {
            PermissionRationale(
                onRequestPermission = { permissionState.launchMultiplePermissionRequest() },
                onChooseLocation = onChooseLocation
            )
        }

        // Otherwise, it's the first launch or permissions are permanently denied.
        else                                  -> {
            PermissionFirstLaunchOrDenied(permissionState, onChooseLocation, permissionResultReceived)
        }
    }
}
```

- [ ] **Step 5: Create `HomeEmptyContent`**

Create `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.home.R

@Composable
fun HomeEmptyContent(
    onAddLocation: () -> Unit,
    onUseMyLocation: () -> Unit
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
        }
    }
}
```

- [ ] **Step 6: Render empty state in `LocationPager`**

In `LocationPager.kt`:

1. Add the `onUseMyLocation` parameter to the signature:

```kotlin
@Composable
fun LocationPager(
    entries: List<LocationEntry>,
    selectedId: String?,
    activeLocationId: String?,
    uiState: HomeUiState,
    prayerDataByLocation: Map<String, LoadedPrayerData>,
    quranVerseFormatter: QuranVerseFormatter,
    onPrayerTimesClick: () -> Unit,
    onAddLocation: () -> Unit,
    onChooseLocation: () -> Unit,
    onUseMyLocation: () -> Unit,
    onEvent: (HomeEvent) -> Unit
) {
```

2. Add the empty-state branch at the top of the composable body (before `val selectedIndex = ...`):

```kotlin
    if (entries.isEmpty()) {
        HomeEmptyContent(
            onAddLocation = onAddLocation,
            onUseMyLocation = onUseMyLocation
        )
        return
    }
```

3. Add the import:

```kotlin
import com.kutluoglu.prayer_feature.home.components.HomeEmptyContent
```

- [ ] **Step 7: Wire `HomeScreen`**

In `HomeScreen.kt`:

1. Change `canProceedWithoutPermission` to always allow content (empty state replaces permission-first UI):

```kotlin
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) },
            onChooseLocation = onChooseLocation,
            canProceedWithoutPermission = true
        ) { permissionActions ->
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
                    if (permissionActions.allPermissionsGranted) onEvent(HomeEvent.OnUseMyLocation)
                    else permissionActions.requestPermission()
                },
                onEvent = onEvent
            )
        }
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: PASS (both `renders error state with retry` and `renders empty state with add location and use my location`).

- [ ] **Step 9: Commit**

```bash
git add core/designsystem/src/main/java/com/kutluoglu/core/designsystem/components/PermissionHandler.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeEmptyContent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt prayer_feature/home/src/main/res/values/strings.xml prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "feat(home): add full-screen empty state with add location and use my location"
```

---

## Task 4: Localize `add_location` and `use_my_location`

**Files:**
- Modify: all 14 locale files under `prayer_feature/home/src/main/res/` (`values-ar`, `values-bn`, `values-de`, `values-es`, `values-fa`, `values-fr`, `values-hi`, `values-id`, `values-ms`, `values-ru`, `values-ta`, `values-th`, `values-tr`, `values-ur`)

- [ ] **Step 1: Add the two strings to each locale file**

For each of the 14 locale files, add these two lines just before the closing `</resources>` tag (after the `no_location_selected` entry), using the translations below:

| Locale | `add_location` | `use_my_location` |
|--------|----------------|-------------------|
| `values-ar` | `إضافة موقع` | `استخدم موقعي` |
| `values-bn` | `অবস্থান যোগ করুন` | `আমার অবস্থান ব্যবহার করুন` |
| `values-de` | `Standort hinzufügen` | `Meinen Standort verwenden` |
| `values-es` | `Añadir ubicación` | `Usar mi ubicación` |
| `values-fa` | `افزودن موقعیت` | `استفاده از موقعیت من` |
| `values-fr` | `Ajouter une localisation` | `Utiliser ma localisation` |
| `values-hi` | `स्थान जोड़ें` | `मेरा स्थान उपयोग करें` |
| `values-id` | `Tambah lokasi` | `Gunakan Lokasi Saya` |
| `values-ms` | `Tambah lokasi` | `Gunakan Lokasi Saya` |
| `values-ru` | `Добавить местоположение` | `Использовать моё местоположение` |
| `values-ta` | `இருப்பிடம் சேர்` | `என் இருப்பிடத்தைப் பயன்படுத்து` |
| `values-th` | `เพิ่มตำแหน่ง` | `ใช้ตำแหน่งของฉัน` |
| `values-tr` | `Konum ekle` | `Konumumu Kullan` |
| `values-ur` | `مقام شامل کریں` | `میرا مقام استعمال کریں` |

Example — for `values-tr/strings.xml`, add before `</resources>`:

```xml
    <string name="add_location">Konum ekle</string>
    <string name="use_my_location">Konumumu Kullan</string>
```

- [ ] **Step 2: Verify the build compiles**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/res/
git commit -m "feat(home): localize empty state add location and use my location strings"
```

---

## Task 5: Full verification

- [ ] **Step 1: Run the home module unit tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew unitTests`
Expected: PASS.

- [ ] **Step 3: Run GitNexus change detection**

Run: `gitnexus_detect_changes()`
Expected: only home empty-state symbols and the `PermissionHandler` are affected; no unexpected execution flows.

- [ ] **Step 4: Commit any remaining changes**

```bash
git status
git add -A
git commit -m "chore(home): verify empty state implementation"
```
