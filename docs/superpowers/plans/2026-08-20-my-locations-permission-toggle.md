# My Locations: GPS Toggle Depends on Location Permission — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the "Mevcut Konumu Kullan" (Use My Current Location) toggle on the Settings > Locations screen depend on the location permission: it shows OFF without permission, requests permission when toggled ON, and only enables GPS after permission is granted.

**Architecture:** UI-layer permission handling in `MyLocationsRoute` (prayer_feature:settings). The displayed toggle state is `gpsEnabled && hasLocationPermission()`. Toggling ON checks permission first; if missing, it launches a permission request and only calls `SetGpsEnabled(true)` on grant. On denial a rationale snackbar is shown (with an "Open Settings" action when permanently denied). A lifecycle ON_RESUME observer re-evaluates permission so the toggle turns off if permission is revoked while the screen is open. No changes to `LocationsCoordinator`, `LocationsDataStore`, or `MyLocationsViewModel`.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, androidx.activity (`rememberLauncherForActivityResult`), androidx.core (`ContextCompat`), Robolectric + Compose UI tests.

**Impact analysis (already run):** `gitnexus_impact` on `MyLocationsRoute` and `MyLocationsScreen` returns LOW risk (0 upstream callers). Safe to edit.

---

### Task 1: Add rationale strings

**Files:**
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml`

- [ ] **Step 1: Add two strings**

Add before the closing `</resources>` tag (after line 47, the `no_locations_yet` entry):

```xml
    <string name="location_permission_required">Location permission is required to use your current location.</string>
    <string name="open_settings">Open Settings</string>
```

- [ ] **Step 2: Commit**

```bash
git add prayer_feature/settings/src/main/res/values/strings.xml
git commit -m "feat(settings): add location permission rationale strings"
```

### Task 2: Write failing screen tests

**Files:**
- Modify: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreenTest.kt`

- [ ] **Step 1: Add imports**

Add these imports to the existing import block (alphabetical, matching the file's style):

```kotlin
import android.Manifest
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.mockk.any
import org.robolectric.Shadows.shadowOf
```

- [ ] **Step 2: Add three test methods**

Add before the closing brace of the class (after the `drag reorder persists new order to coordinator` test):

```kotlin
    @Test
    fun `toggle is off when location permission is missing even if gps is enabled`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = true, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `toggling on with permission granted enables gps`() {
        shadowOf(composeTestRule.activity).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = false, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.waitForIdle()
        coVerify { coordinator.setGpsEnabled(true) }
    }

    @Test
    fun `toggling on without permission requests permission and does not enable gps`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = false, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.waitForIdle()
        coVerify(exactly = 0) { coordinator.setGpsEnabled(any()) }
        assertThat(shadowOf(composeTestRule.activity).requestedPermissions)
            .contains(Manifest.permission.ACCESS_FINE_LOCATION)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*MyLocationsScreenTest"`
Expected: The first test FAILS (the toggle still shows ON because the switch is not permission-aware yet). The other two may also fail.

### Task 3: Implement permission-aware toggle

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreen.kt`

- [ ] **Step 1: Add imports**

Add these imports to the existing import block:

```kotlin
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Add permission state, launcher, and lifecycle observer**

Inside `MyLocationsRoute`, after the `reorderableState` block (line ~70) and before the `LaunchedEffect(state.entries)` block (line ~72), add:

```kotlin
    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var permissionCheckTrigger by remember { mutableIntStateOf(0) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionGranted = remember(permissionCheckTrigger) { hasLocationPermission() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(true))
        } else {
            val permanentDenial = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.location_permission_required),
                    actionLabel = if (permanentDenial) context.getString(R.string.open_settings) else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionCheckTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
```

- [ ] **Step 3: Add SnackbarHost to the Scaffold**

Change the `Scaffold(` call so it includes `snackbarHost` after the `topBar` block:

```kotlin
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_locations)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
```

- [ ] **Step 4: Make the Switch permission-aware**

Replace the existing `Switch(...)` block (lines ~121-124):

```kotlin
                Switch(
                    checked = state.gpsEnabled && permissionGranted,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (permissionGranted) {
                                viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(true))
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        } else {
                            viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(false))
                        }
                    }
                )
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*MyLocationsScreenTest"`
Expected: All tests PASS.

### Task 4: Run full module tests and build

- [ ] **Step 1: Run all settings module tests**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: All tests pass (including `MyLocationsViewModelTest` — no ViewModel changes were made).

- [ ] **Step 2: Build the app**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

### Task 5: Commit

- [ ] **Step 1: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreen.kt
git add prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/location/MyLocationsScreenTest.kt
git commit -m "feat(settings): gate GPS toggle on location permission"
```
