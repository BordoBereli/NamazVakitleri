# Settings Version Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the app version name and code in an informative "about" footer at the bottom of the Settings page.

**Architecture:** Enable `buildConfig` in the `:app` module, define a plain `AppVersion(name, code)` data class in `:core:common` (a pure JVM module), expose it via a Koin `single` in `AppModule`, inject it into `SettingsViewModel`, and render it in `SettingsScreen`. The settings module already depends on `:core:common`, so it can reference `AppVersion` directly.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, JUnit 5 + MockK + Turbine (ViewModel), Robolectric + Compose UI test (Screen).

---

## File Structure

| File | Responsibility | Action |
|------|----------------|--------|
| `app/build.gradle.kts` | Enable `buildConfig` so `BuildConfig.VERSION_NAME/CODE` exist | Modify |
| `core/common/src/main/java/com/kutluoglu/core/common/AppVersion.kt` | Plain `AppVersion(name, code)` data class | Create |
| `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt` | Koin `single` for `AppVersion`; add `get()` to `SettingsViewModel` | Modify |
| `prayer_feature/settings/.../SettingsContract.kt` | Add `version: AppVersion` to `Success` state | Modify |
| `prayer_feature/settings/.../SettingsViewModel.kt` | Inject `AppVersion`, populate `Success` state | Modify |
| `prayer_feature/settings/.../SettingsScreen.kt` | Render the version footer | Modify |
| `prayer_feature/settings/src/main/res/values*/strings.xml` | Add `version_format` (all locales) and `app_name` (default) | Modify |
| `prayer_feature/settings/.../SettingsViewModelTest.kt` | Assert version in `Success` state | Modify |
| `prayer_feature/settings/.../SettingsScreenTest.kt` | Assert footer renders version | Modify |

Note: the launcher icon lives in `:app` and is NOT accessible from the settings library module, so the footer uses a Material `Icons.Filled.Info` icon in a circular surface instead of the launcher icon.

---

## Task 1: Enable buildConfig, add AppVersion, wire Koin

**Files:**
- Modify: `app/build.gradle.kts:47-49`
- Create: `core/common/src/main/java/com/kutluoglu/core/common/AppVersion.kt`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt`

- [ ] **Step 1: Enable buildConfig in the app module**

In `app/build.gradle.kts`, change the `buildFeatures` block (lines 47-49) from:

```kotlin
    buildFeatures {
        compose = true
    }
```

to:

```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
```

- [ ] **Step 2: Create the AppVersion data class**

Create `core/common/src/main/java/com/kutluoglu/core/common/AppVersion.kt`:

```kotlin
package com.kutluoglu.core.common

data class AppVersion(
    val name: String,
    val code: Int
)
```

- [ ] **Step 3: Add the Koin single and update the ViewModel wiring**

In `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt`:

Add the `AppVersion` single inside the `module { ... }` block (e.g., after the `LocaleManager` single at line 29). `BuildConfig` is in the same package (`com.kutluoglu.namazvakitleri`), so no import is needed:

```kotlin
    // App version info (from BuildConfig)
    single {
        AppVersion(
            name = BuildConfig.VERSION_NAME,
            code = BuildConfig.VERSION_CODE
        )
    }
```

Do NOT change the `SettingsViewModel` wiring yet — it still has 8 constructor params until Task 3. The wiring is updated to 9 `get()` calls in Task 3, Step 3.

- [ ] **Step 4: Verify the build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no errors)

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts core/common/src/main/java/com/kutluoglu/core/common/AppVersion.kt app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt
git commit -m "feat: add AppVersion provider from BuildConfig"
```

---

## Task 2: Add string resources

**Files:**
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-tr/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ar/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-bn/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-de/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-es/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-fa/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-fr/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-hi/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-id/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ms/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ru/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ta/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-th/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ur/strings.xml`

- [ ] **Step 1: Add `app_name` to the default strings**

In `prayer_feature/settings/src/main/res/values/strings.xml`, add before `</resources>` (line 99):

```xml
    <string name="app_name">NamazVakitleri</string>
```

- [ ] **Step 2: Add `version_format` to all locale files**

Add the following line before `</resources>` in each locale file (the value differs per locale):

| File | Line to add |
|------|-------------|
| `values/strings.xml` | `<string name="version_format">Version %1$s (%2$d)</string>` |
| `values-tr/strings.xml` | `<string name="version_format">Sürüm %1$s (%2$d)</string>` |
| `values-ar/strings.xml` | `<string name="version_format">الإصدار %1$s (%2$d)</string>` |
| `values-bn/strings.xml` | `<string name="version_format">সংস্করণ %1$s (%2$d)</string>` |
| `values-de/strings.xml` | `<string name="version_format">Version %1$s (%2$d)</string>` |
| `values-es/strings.xml` | `<string name="version_format">Versión %1$s (%2$d)</string>` |
| `values-fa/strings.xml` | `<string name="version_format">نسخه %1$s (%2$d)</string>` |
| `values-fr/strings.xml` | `<string name="version_format">Version %1$s (%2$d)</string>` |
| `values-hi/strings.xml` | `<string name="version_format">संस्करण %1$s (%2$d)</string>` |
| `values-id/strings.xml` | `<string name="version_format">Versi %1$s (%2$d)</string>` |
| `values-ms/strings.xml` | `<string name="version_format">Versi %1$s (%2$d)</string>` |
| `values-ru/strings.xml` | `<string name="version_format">Версия %1$s (%2$d)</string>` |
| `values-ta/strings.xml` | `<string name="version_format">பதிப்பு %1$s (%2$d)</string>` |
| `values-th/strings.xml` | `<string name="version_format">เวอร์ชัน %1$s (%2$d)</string>` |
| `values-ur/strings.xml` | `<string name="version_format">ورژن %1$s (%2$d)</string>` |

- [ ] **Step 3: Verify resources compile**

Run: `./gradlew :prayer_feature:settings:processDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/settings/src/main/res/values prayer_feature/settings/src/main/res/values-tr prayer_feature/settings/src/main/res/values-ar prayer_feature/settings/src/main/res/values-bn prayer_feature/settings/src/main/res/values-de prayer_feature/settings/src/main/res/values-es prayer_feature/settings/src/main/res/values-fa prayer_feature/settings/src/main/res/values-fr prayer_feature/settings/src/main/res/values-hi prayer_feature/settings/src/main/res/values-id prayer_feature/settings/src/main/res/values-ms prayer_feature/settings/src/main/res/values-ru prayer_feature/settings/src/main/res/values-ta prayer_feature/settings/src/main/res/values-th prayer_feature/settings/src/main/res/values-ur
git commit -m "feat: add version and app name strings for settings footer"
```

---

## Task 3: Expose version in SettingsViewModel (TDD)

**Files:**
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/SettingsViewModelTest.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsContract.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsViewModel.kt`

- [ ] **Step 1: Write the failing test**

In `SettingsViewModelTest.kt`:

Add the import and a field:

```kotlin
import com.kutluoglu.core.common.AppVersion
```

```kotlin
    private val appVersion = AppVersion(name = "2.0.0", code = 200)
```

Add `appVersion` as the last argument to the `SettingsViewModel(...)` constructor call in `setUp()` (after `analyticsTracker`):

```kotlin
        viewModel = SettingsViewModel(
            getSettingsUseCase,
            updateLocationUseCase,
            updateCalculationMethodUseCase,
            updateLanguageUseCase,
            updateHijriAdjustmentUseCase,
            clearLocationCacheUseCase,
            clearPrayerTimesCacheUseCase,
            analyticsTracker,
            appVersion
        )
```

Add a new test at the end of the class:

```kotlin
    @Test
    fun `Success state should expose app version`() = runTest {
        // Act
        viewModel.uiState.test {
            val state = awaitItem()

            // Assert
            assertThat(state).isInstanceOf(SettingsUiState.Success::class.java)
            val successState = state as SettingsUiState.Success
            assertThat(successState.version.name).isEqualTo("2.0.0")
            assertThat(successState.version.code).isEqualTo(200)
            cancelAndIgnoreRemainingEvents()
        }
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.SettingsViewModelTest"`
Expected: COMPILATION FAILURE — `SettingsUiState.Success` has no `version` property, and `SettingsViewModel` constructor has no `appVersion` parameter.

- [ ] **Step 3: Implement the minimal change**

In `SettingsContract.kt`, add the import and update `Success`:

```kotlin
import com.kutluoglu.core.common.AppVersion
```

```kotlin
    data class Success(
        val settings: Settings,
        val version: AppVersion
    ) : SettingsUiState()
```

In `SettingsViewModel.kt`, add the import and constructor param, and pass it to `Success`:

```kotlin
import com.kutluoglu.core.common.AppVersion
```

```kotlin
class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateLocationUseCase: UpdateLocationUseCase,
    private val updateCalculationMethodUseCase: UpdateCalculationMethodUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val updateHijriAdjustmentUseCase: UpdateHijriAdjustmentUseCase,
    private val clearLocationCacheUseCase: ClearLocationCacheUseCase,
    private val clearPrayerTimesCacheUseCase: ClearPrayerTimesCacheUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val appVersion: AppVersion
) : ViewModel() {
```

In `loadSettings()` (line 60), change:

```kotlin
                _uiState.value = SettingsUiState.Success(settings)
```

to:

```kotlin
                _uiState.value = SettingsUiState.Success(settings, appVersion)
```

Update the `SettingsViewModel` wiring in `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt` (line 53) to add one more `get()`:

```kotlin
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.SettingsViewModelTest"`
Expected: All tests PASS (including the new `Success state should expose app version`).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/SettingsViewModelTest.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsContract.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsViewModel.kt app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt
git commit -m "feat: expose app version in settings UI state"
```

---

## Task 4: Render the version footer in SettingsScreen (TDD)

**Files:**
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/SettingsScreenTest.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Write the failing test**

In `SettingsScreenTest.kt`:

Add the import and a field:

```kotlin
import com.kutluoglu.core.common.AppVersion
```

```kotlin
    private val appVersion = AppVersion(name = "2.0.0", code = 200)
```

Add `appVersion` as the last argument to the `SettingsViewModel(...)` constructor call in `launchScreen()` (after `analyticsTracker`):

```kotlin
        val viewModel = SettingsViewModel(
            getSettingsUseCase,
            updateLocationUseCase,
            updateCalculationMethodUseCase,
            updateLanguageUseCase,
            updateHijriAdjustmentUseCase,
            clearLocationCacheUseCase,
            clearPrayerTimesCacheUseCase,
            analyticsTracker,
            appVersion
        )
```

Add a new test at the end of the class:

```kotlin
    @Test
    fun `renders version footer with name and code`() {
        launchScreen()

        composeTestRule.onNodeWithText("Version 2.0.0 (200)").assertIsDisplayed()
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.SettingsScreenTest"`
Expected: FAIL — assertion error because "Version 2.0.0 (200)" is not rendered yet (the footer does not exist).

- [ ] **Step 3: Implement the footer**

In `SettingsScreen.kt`:

Add imports:

```kotlin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import com.kutluoglu.core.common.AppVersion
```

In `SettingsScreen`, pass the version to `SettingsContent` (line 104-112):

```kotlin
                is SettingsUiState.Success -> {
                    SettingsContent(
                        settings = state.settings,
                        version = state.version,
                        onClearCacheClick = { showClearCacheDialog = true },
                        onNavigateToMyLocations = onNavigateToMyLocations,
                        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
                        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
                        onNavigateToLanguage = onNavigateToLanguage,
                        onNavigateToNotifications = onNavigateToNotifications
                    )
                }
```

Update the `SettingsContent` signature to accept `version`:

```kotlin
private fun SettingsContent(
    settings: Settings,
    version: AppVersion,
    onClearCacheClick: () -> Unit,
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
```

Add the footer call at the end of the `Column`, after the Clear Cache `Card` (after line 240):

```kotlin
        VersionFooter(version = version)
    }
}
```

Add the `VersionFooter` composable at the end of the file (after `getLanguageName`):

```kotlin
@Composable
private fun VersionFooter(version: AppVersion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp)
            )
        }
        Text(
            text = stringResource(SettingsR.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(SettingsR.string.version_format, version.name, version.code),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.SettingsScreenTest"`
Expected: All tests PASS (including `renders version footer with name and code`).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/SettingsScreenTest.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt
git commit -m "feat: show app version footer on settings page"
```

---

## Task 5: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full settings test suite**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run gitnexus_detect_changes to confirm scope**

Run: `gitnexus_detect_changes({scope: "all"})`
Expected: only `AppVersion`, `SettingsViewModel`, `SettingsContract`, `SettingsScreen`, `AppModule` symbols affected — no unexpected execution flows.

- [ ] **Step 3: Run the debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

---

## Self-Review Notes

- **Spec coverage:** buildConfig enabled (Task 1), `AppVersion` in core:common (Task 1), Koin single (Task 1), injected into ViewModel and exposed in `Success` state (Task 3), footer rendered with icon + app name + version name/code (Task 4), localized `version_format` string (Task 2). All spec requirements covered.
- **Type consistency:** `AppVersion(name: String, code: Int)` is defined once in Task 1 and referenced identically in Tasks 3 and 4. `SettingsUiState.Success(settings, version)` matches the ViewModel construction and Screen access `state.version`.
- **Placeholder scan:** No TBD/TODO; every code step shows complete code.
