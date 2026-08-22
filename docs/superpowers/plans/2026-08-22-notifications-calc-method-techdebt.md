# Notifications, Calculation Method Fix & Technical Debt — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a full prayer-time notification system, fix the calculation-method name bug, and clear the identified technical debt (dedupe, DataStore consolidation, doc rewrite, Compose UI tests, CI).

**Architecture:** New `prayer_notifications` module (mirrors `prayer_location`/`prayer_qibla`) with a pure `SchedulePlan` calculator, AlarmManager-exact + WorkManager scheduling, BroadcastReceivers, a notification manager, and an adhan player. The calculation-method fix unifies two duplicate definitions into the `prayer/model` enum and localizes names. Tech debt is handled as independent refactors.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (KSP), DataStore Preferences, AlarmManager, WorkManager, adhan2, JUnit 5 + MockK + Turbine + Truth, Robolectric, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-08-22-notifications-calc-method-techdebt-design.md`

**Build/test commands used throughout:**
- Unit tests: `./gradlew testDebugUnitTest`
- Single test: `./gradlew testDebugUnitTest --tests="*ClassName*"`
- Full suite: `./gradlew allTests`

---

# Phase 1: Calculation Method Fix

## Task 1.1: Unify duplicate `CalculationMethod` definitions

**Files:**
- Delete: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/CalculationMethod.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodViewModel.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodContract.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodScreen.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodViewModelTest.kt`

- [ ] **Step 1: Read the current contract**

Read `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodContract.kt` to see the `CalculationMethodUiState` and `CalculationMethodEvent` definitions that reference `com.kutluoglu.prayer_settings.domain.model.CalculationMethod`.

- [ ] **Step 2: Write the failing test**

In `CalculationMethodViewModelTest.kt`, add a test asserting the ViewModel exposes the 6 enum methods with the enum's IDs:

```kotlin
@Test
fun `loadMethods exposes the six enum methods`() = runTest {
    coEvery { getSettingsUseCase() } returns Settings()
    val viewModel = CalculationMethodViewModel(getSettingsUseCase, updateCalculationMethodUseCase, analyticsTracker)
    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
    val methods = (state as CalculationMethodUiState.MethodsLoaded).methods
    assertThat(methods.map { it.id }).containsExactly(
        "TURKEY_DIYANET", "MWL", "ISNA", "EGYPT", "MAKKAH", "KARACHI"
    )
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CalculationMethodViewModelTest*"`
Expected: FAIL — `CalculationMethod.methods` no longer resolves (data class deleted in next step) or the import is broken.

- [ ] **Step 4: Update the ViewModel to use the enum**

Replace the import and `CalculationMethod.methods` reference in `CalculationMethodViewModel.kt`:

```kotlin
import com.kutluoglu.prayer.model.prayer.CalculationMethod
```

Change `loadMethods()` and `selectMethod(method: CalculationMethod)` to use `CalculationMethod.entries` instead of `CalculationMethod.methods`:

```kotlin
private fun loadMethods() {
    try {
        _uiState.value = CalculationMethodUiState.MethodsLoaded(
            methods = CalculationMethod.entries,
            selectedMethod = currentMethodId
        )
    } catch (e: Exception) {
        _uiState.value = CalculationMethodUiState.Error(e.message ?: "Failed to load methods")
    }
}
```

- [ ] **Step 5: Delete the duplicate data class**

Delete `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/CalculationMethod.kt`.

- [ ] **Step 6: Update the contract**

In `CalculationMethodContract.kt`, change the `CalculationMethod` import to `com.kutluoglu.prayer.model.prayer.CalculationMethod` and update `MethodsLoaded` to carry `List<CalculationMethod>` (enum entries). The `CalculationMethodEvent.SelectMethod` event keeps `val method: CalculationMethod`.

- [ ] **Step 7: Update the screen**

In `CalculationMethodScreen.kt`, the `MethodItem` composable reads `method.name` and `method.description`. The enum has no `description` property. Change `MethodItem` to show the localized name only:

```kotlin
@Composable
private fun MethodItem(
    method: CalculationMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = stringResource(method.displayNameRes()),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: PASS. Fix any other compile errors from the deleted data class (search for `prayer_settings.domain.model.CalculationMethod` usages).

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor(settings): unify CalculationMethod into prayer/model enum"
```

## Task 1.2: Add localized display names for the 6 methods

**Files:**
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodNames.kt`
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml` (and all 13 `values-*/strings.xml`)
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/calculation/CalculationMethodNamesTest.kt`

- [ ] **Step 1: Write the failing test**

Create `CalculationMethodNamesTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.calculation

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import org.junit.jupiter.api.Test

class CalculationMethodNamesTest {

    @Test
    fun `every enum method has a display name resource`() {
        CalculationMethod.entries.forEach { method ->
            assertThat(method.displayNameRes()).isNotNull()
        }
    }

    @Test
    fun `unknown id falls back to TURKEY_DIYANET`() {
        assertThat(CalculationMethod.fromSettingsId("MUSLIM_WORLD_LEAGUE"))
            .isEqualTo(CalculationMethod.TURKEY_DIYANET)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*CalculationMethodNamesTest*"`
Expected: FAIL — `displayNameRes()` is not defined.

- [ ] **Step 3: Create the name resolver**

Create `CalculationMethodNames.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.calculation

import androidx.annotation.StringRes
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_feature.settings.R

@StringRes
fun CalculationMethod.displayNameRes(): Int = when (this) {
    CalculationMethod.TURKEY_DIYANET -> R.string.calculation_method_turkey_diyanet
    CalculationMethod.MWL -> R.string.calculation_method_mwl
    CalculationMethod.ISNA -> R.string.calculation_method_isna
    CalculationMethod.EGYPT -> R.string.calculation_method_egypt
    CalculationMethod.MAKKAH -> R.string.calculation_method_makkah
    CalculationMethod.KARACHI -> R.string.calculation_method_karachi
}
```

- [ ] **Step 4: Add string resources**

Add to `prayer_feature/settings/src/main/res/values/strings.xml`:

```xml
<string name="calculation_method_turkey_diyanet">Türkiye (Diyanet)</string>
<string name="calculation_method_mwl">Muslim World League</string>
<string name="calculation_method_isna">Islamic Society of North America</string>
<string name="calculation_method_egypt">Egyptian General Authority</string>
<string name="calculation_method_makkah">Umm Al-Qura University</string>
<string name="calculation_method_karachi">University of Karachi</string>
```

Add the same 6 keys to every `values-*/strings.xml` in `prayer_feature/settings` (ar, bn, de, es, fa, fr, hi, id, ms, ru, ta, th, tr, ur) with translated names (use the existing `calculation_method_*` translations if any already exist; otherwise translate).

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(settings): localize calculation method display names"
```

## Task 1.3: Fix `getCalculationMethodName` in SettingsScreen

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Replace the hardcoded mapping**

In `SettingsScreen.kt`, replace the `getCalculationMethodName` composable (lines ~318-329) with a localized, enum-driven lookup:

```kotlin
@Composable
private fun getCalculationMethodName(method: String): String {
    return stringResource(CalculationMethod.fromSettingsId(method).displayNameRes())
}
```

Add the import: `import com.kutluoglu.prayer_feature.settings.calculation.displayNameRes`

- [ ] **Step 2: Verify the bogus branches are gone**

Confirm no references to `MUSLIM_WORLD_LEAGUE`, `EGYPT_SURVEY`, or `TEHRAN` remain in `SettingsScreen.kt`:

Run: `grep -rn "MUSLIM_WORLD_LEAGUE\|EGYPT_SURVEY\|TEHRAN" prayer_feature/settings/src/main`
Expected: no matches.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :prayer_feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix(settings): use enum-driven localized calculation method names"
```

## Task 1.4: Fix `getLanguageName` list

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml` (and all `values-*/strings.xml`)

- [ ] **Step 1: Add localized language-name resources**

Add to `prayer_feature/settings/src/main/res/values/strings.xml`:

```xml
<string name="language_tr">Türkçe</string>
<string name="language_en">English</string>
<string name="language_ar">العربية</string>
<string name="language_de">Deutsch</string>
<string name="language_fr">Français</string>
<string name="language_es">Español</string>
<string name="language_bn">বাংলা</string>
<string name="language_fa">فارسی</string>
<string name="language_hi">हिन्दी</string>
<string name="language_id">Bahasa Indonesia</string>
<string name="language_ms">Bahasa Melayu</string>
<string name="language_ru">Русский</string>
<string name="language_ta">தமிழ்</string>
<string name="language_th">ไทย</string>
<string name="language_ur">اردو</string>
```

Add the same keys to all `values-*/strings.xml` folders (each locale's own native name).

- [ ] **Step 2: Replace `getLanguageName`**

In `SettingsScreen.kt`, replace the `getLanguageName` composable (lines ~331-341):

```kotlin
@Composable
private fun getLanguageName(language: String): String {
    val res = when (language) {
        "tr" -> R.string.language_tr
        "en" -> R.string.language_en
        "ar" -> R.string.language_ar
        "de" -> R.string.language_de
        "fr" -> R.string.language_fr
        "es" -> R.string.language_es
        "bn" -> R.string.language_bn
        "fa" -> R.string.language_fa
        "hi" -> R.string.language_hi
        "id" -> R.string.language_id
        "ms" -> R.string.language_ms
        "ru" -> R.string.language_ru
        "ta" -> R.string.language_ta
        "th" -> R.string.language_th
        "ur" -> R.string.language_ur
        else -> language
    }
    return stringResource(res)
}
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :prayer_feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix(settings): localize all 13 language display names"
```

## Task 1.5: Phase 1 verification

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 2: Run `gitnexus_detect_changes()`**

Run `gitnexus_detect_changes` (per AGENTS.md) and confirm the only affected symbols are the calculation-method ones.

---

# Phase 2: Technical Debt — Quick Wins

## Task 2.1: Dedupe `getCountryCode`

**Files:**
- Create: `core/common/src/main/java/com/kutluoglu/core/common/utils/CountryCodeUtils.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/LocationCoordinator.kt:112`
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/data/repository/SettingsRepositoryImpl.kt:39`
- Test: `core/common/src/test/java/com/kutluoglu/core/common/utils/CountryCodeUtilsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `CountryCodeUtilsTest.kt`:

```kotlin
package com.kutluoglu.core.common.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CountryCodeUtilsTest {

    @Test
    fun `maps known timezones to country codes`() {
        assertThat(countryCodeFromTimeZone("Europe/Istanbul")).isEqualTo("TR")
        assertThat(countryCodeFromTimeZone("Europe/Berlin")).isEqualTo("DE")
        assertThat(countryCodeFromTimeZone("Europe/London")).isEqualTo("GB")
        assertThat(countryCodeFromTimeZone("Europe/Paris")).isEqualTo("FR")
        assertThat(countryCodeFromTimeZone("Asia/Jakarta")).isEqualTo("ID")
        assertThat(countryCodeFromTimeZone("Asia/Riyadh")).isEqualTo("SA")
    }

    @Test
    fun `returns null for unknown timezones`() {
        assertThat(countryCodeFromTimeZone("Pacific/Auckland")).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:common:testDebugUnitTest --tests="*CountryCodeUtilsTest*"`
Expected: FAIL — `countryCodeFromTimeZone` not defined.

- [ ] **Step 3: Create the shared util**

Create `CountryCodeUtils.kt`:

```kotlin
package com.kutluoglu.core.common.utils

fun countryCodeFromTimeZone(timeZone: String): String? {
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
```

- [ ] **Step 4: Update both call sites**

In `LocationCoordinator.kt`, delete the private `getCountryCode` and call the util:

```kotlin
import com.kutluoglu.core.common.utils.countryCodeFromTimeZone
// ...
countryCode = countryCodeFromTimeZone(locationSettings.timeZone),
```

In `SettingsRepositoryImpl.kt`, delete the private `getCountryCode` and call the util:

```kotlin
import com.kutluoglu.core.common.utils.countryCodeFromTimeZone
// ...
countryCode = countryCodeFromTimeZone(location.timeZone),
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :core:common:testDebugUnitTest :prayer_feature:home:testDebugUnitTest :prayer_settings:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: extract shared countryCodeFromTimeZone util"
```

## Task 2.2: Rewrite the aspirational doc + audit other docs

**Files:**
- Modify: `NamazVakitleriTechnicalAnalysis`
- Modify: `README.md` (module list + features if stale)
- Modify: `TODO.md` (mark completed items)

- [ ] **Step 1: Rewrite the technical analysis doc**

Rewrite `NamazVakitleriTechnicalAnalysis` to describe the **actual** architecture:
- DataStore-backed prayer-times cache (not Room)
- No WorkManager/notifications/widgets yet (note: notifications are being added in this plan)
- The real module list from `settings.gradle.kts`
- Actual tech stack (Koin, adhan2, osmdroid, coil, Firebase)

- [ ] **Step 2: Audit README**

Update `README.md`:
- Fix the module list in "Teknik Yapı ve Mimari" to match `settings.gradle.kts` (add `prayer_remote`, `prayer_qibla`, `prayer_cache`, `prayer_location`, `prayer_settings`, `prayer_navigation:core`).
- Remove the placeholder image note or replace with a real screenshot if available.

- [ ] **Step 3: Update TODO.md**

Mark TODO items #11 (getCountryCode), #12 (legacy DataStores — after Phase 3), #14 (aspirational doc) as done with dates.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "docs: rewrite technical analysis to match actual architecture"
```

---

# Phase 3: Technical Debt — DataStore Consolidation

## Task 3.1: Trace consumers of the legacy `prayer:data` interfaces

**Files:**
- Read-only investigation

- [ ] **Step 1: Find consumers of `prayer:data` SettingsDataStore**

Run: `grep -rn "com.kutluoglu.prayer.data.settings.SettingsDataStore\|prayer.data.settings" --include="*.kt" app prayer prayer_feature prayer_* core`
Expected: only `prayer_cache/SettingsDataStoreImp.kt` implements it. Note any other usages.

- [ ] **Step 2: Find consumers of `prayer:data` LocationDataStore**

Run: `grep -rn "com.kutluoglu.prayer.data.repository.location.LocationDataStore\|prayer.data.repository.location" --include="*.kt" app prayer prayer_feature prayer_* core`
Expected: only `prayer_cache/LocationDataStoreImp.kt` implements it. Note any other usages.

- [ ] **Step 3: Record findings**

If both interfaces have **no consumers** outside `prayer_cache`, proceed to Task 3.2 (removal). If consumers exist, note them and migrate them to the typed `prayer_settings` DataStore first (in Task 3.2 Step 0).

## Task 3.2: Remove legacy DataStores and the `prayer_cache` module

**Files:**
- Delete: `prayer_cache/` (entire module)
- Modify: `settings.gradle.kts` (remove `include(":prayer_cache")`)
- Modify: `app/build.gradle.kts` (remove `implementation(project(":prayer_cache"))`)
- Modify: `prayer_settings/build.gradle.kts` (remove `implementation(project(":prayer_cache"))`)

- [ ] **Step 0 (only if consumers found in Task 3.1): Migrate consumers**

For each consumer of the legacy interfaces, switch it to the typed `prayer_settings` `SettingsDataStore` / `LocationsDataStore` API. Update the Koin graph accordingly.

- [ ] **Step 1: Delete the module**

Run: `rm -rf prayer_cache`

- [ ] **Step 2: Remove from settings.gradle.kts**

Delete the line `include(":prayer_cache")  // Local data sources`.

- [ ] **Step 3: Remove the dependency from app**

In `app/build.gradle.kts`, delete `implementation(project(":prayer_cache"))`.

- [ ] **Step 4: Remove the dependency from prayer_settings**

In `prayer_settings/build.gradle.kts`, delete `implementation(project(":prayer_cache"))`.

- [ ] **Step 5: Verify the build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: remove legacy prayer_cache DataStores"
```

---

# Phase 4: Notifications — Module Foundation

## Task 4.1: Create the `prayer_notifications` module

**Files:**
- Create: `prayer_notifications/build.gradle.kts`
- Create: `prayer_notifications/src/main/AndroidManifest.xml`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/di/PrayerNotificationsModule.kt`
- Create: `prayer_notifications/src/main/res/raw/adhan.mp3` (placeholder — see Step 4)
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the module to settings.gradle.kts**

Add after the `prayer_qibla` include:

```kotlin
include(":prayer_notifications")
```

- [ ] **Step 2: Create build.gradle.kts**

Create `prayer_notifications/build.gradle.kts` (copy the `prayer_location/build.gradle.kts` template, change namespace):

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.prayer_notifications"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":prayer:model"))
    implementation(project(":prayer:domain"))
    implementation(project(":prayer:data"))
    implementation(project(":prayer_location"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)

    api(platform(libs.koin.bom))
    api(libs.koin.core)
    api(libs.koin.annotations)
    ksp(libs.koin.ksp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
tasks.withType<Test> { useJUnitPlatform() }
```

- [ ] **Step 3: Add the WorkManager dependency to the version catalog**

In `gradle/libs.versions.toml`, add under `[versions]`:

```toml
workRuntime = "2.10.0"
```

and under `[libraries]`:

```toml
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workRuntime" }
```

- [ ] **Step 4: Add the adhan audio placeholder**

Create `prayer_notifications/src/main/res/raw/adhan.mp3` as a placeholder file (a short silent mp3). Note in the plan: the real adhan audio file must be provided by the user before release.

- [ ] **Step 5: Create the manifest**

Create `prayer_notifications/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application>
        <receiver
            android:name=".scheduler.AlarmReceiver"
            android:exported="false" />
        <receiver
            android:name=".scheduler.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>

</manifest>
```

- [ ] **Step 6: Create the Koin module**

Create `PrayerNotificationsModule.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_notifications**")
object PrayerNotificationsModule
```

- [ ] **Step 7: Wire the module into app**

In `app/build.gradle.kts`, add `implementation(project(":prayer_notifications"))` to dependencies. In `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt`, add `@ComponentScan("com.kutluoglu.prayer_notifications**")` (or add the module to the app's module list).

- [ ] **Step 8: Verify the build**

Run: `./gradlew :prayer_notifications:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat(notifications): scaffold prayer_notifications module"
```

## Task 4.2: `NotificationSettings` model + `NotificationSettingsDataStore`

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/NotificationSettings.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStore.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStoreTest.kt`

- [ ] **Step 1: Write the failing test**

Create `NotificationSettingsDataStoreTest.kt` (Robolectric, following the `SettingsDataStoreTest` pattern in `prayer_settings`):

```kotlin
package com.kutluoglu.prayer_notifications.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSettingsDataStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun freshStore(): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context, "test_notif_${System.nanoTime()}")

    @Test
    fun `defaults are sensible`() = runTest {
        val store = freshStore()
        val settings = store.getSettings()
        assertThat(settings.enabled).isFalse()
        assertThat(settings.prayerToggles["Fajr"]).isTrue()
        assertThat(settings.prePrayerMinutes).isEqualTo(15)
    }

    @Test
    fun `updateEnabled persists`() = runTest {
        val store = freshStore()
        store.updateEnabled(true)
        assertThat(store.getSettings().enabled).isTrue()
    }

    @Test
    fun `updatePrayerToggle persists per prayer`() = runTest {
        val store = freshStore()
        store.updatePrayerToggle("Fajr", false)
        val settings = store.getSettings()
        assertThat(settings.prayerToggles["Fajr"]).isFalse()
        assertThat(settings.prayerToggles["Dhuhr"]).isTrue()
    }

    @Test
    fun `updatePrePrayerReminder persists minutes`() = runTest {
        val store = freshStore()
        store.updatePrePrayerReminder(true, 30)
        val settings = store.getSettings()
        assertThat(settings.prePrayerReminderEnabled).isTrue()
        assertThat(settings.prePrayerMinutes).isEqualTo(30)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*NotificationSettingsDataStoreTest*"`
Expected: FAIL — classes not defined.

- [ ] **Step 3: Create the model**

Create `NotificationSettings.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.domain

data class NotificationSettings(
    val enabled: Boolean = false,
    val prayerToggles: Map<String, Boolean> = defaultPrayerToggles(),
    val adhanEnabled: Boolean = true,
    val countdownEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 8,
    val dailyReminderMinute: Int = 0,
    val prePrayerReminderEnabled: Boolean = false,
    val prePrayerMinutes: Int = 15,
    val jumuahEnabled: Boolean = true,
    val specialDaysEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    companion object {
        val PRAYER_KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun defaultPrayerToggles(): Map<String, Boolean> =
            PRAYER_KEYS.associateWith { true }
    }
}
```

- [ ] **Step 4: Create the DataStore**

Create `NotificationSettingsDataStore.kt` (follow the `SettingsDataStore` pattern in `prayer_settings`):

```kotlin
package com.kutluoglu.prayer_notifications.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class NotificationSettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        fun create(context: Context, name: String = "notification_settings_store"): NotificationSettingsDataStore {
            return NotificationSettingsDataStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    produceFile = { context.preferencesDataStoreFile(name) }
                )
            )
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PRAYER_TOGGLES = stringPreferencesKey("prayer_toggles")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val PRE_PRAYER_ENABLED = booleanPreferencesKey("pre_prayer_enabled")
        val PRE_PRAYER_MINUTES = intPreferencesKey("pre_prayer_minutes")
        val JUMUAH_ENABLED = booleanPreferencesKey("jumuah_enabled")
        val SPECIAL_DAYS_ENABLED = booleanPreferencesKey("special_days_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    fun observeSettings(): Flow<NotificationSettings> = dataStore.data.map { it.toSettings() }

    suspend fun getSettings(): NotificationSettings = dataStore.data.first().toSettings()

    suspend fun updateEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ENABLED] = enabled }

    suspend fun updatePrayerToggle(prayerKey: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.PRAYER_TOGGLES].orEmpty()
                .split(",").filter { it.isNotBlank() }.toMutableSet()
            if (enabled) current.add(prayerKey) else current.remove(prayerKey)
            prefs[Keys.PRAYER_TOGGLES] = current.joinToString(",")
        }
    }

    suspend fun updateAdhanEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ADHAN_ENABLED] = enabled }
    suspend fun updateCountdownEnabled(enabled: Boolean) = dataStore.edit { it[Keys.COUNTDOWN_ENABLED] = enabled }
    suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) = dataStore.edit {
        it[Keys.DAILY_REMINDER_ENABLED] = enabled
        it[Keys.DAILY_REMINDER_HOUR] = hour
        it[Keys.DAILY_REMINDER_MINUTE] = minute
    }
    suspend fun updatePrePrayerReminder(enabled: Boolean, minutes: Int) = dataStore.edit {
        it[Keys.PRE_PRAYER_ENABLED] = enabled
        it[Keys.PRE_PRAYER_MINUTES] = minutes
    }
    suspend fun updateJumuahEnabled(enabled: Boolean) = dataStore.edit { it[Keys.JUMUAH_ENABLED] = enabled }
    suspend fun updateSpecialDaysEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SPECIAL_DAYS_ENABLED] = enabled }
    suspend fun updateSoundEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    suspend fun updateVibrationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }

    private fun Preferences.toSettings(): NotificationSettings {
        val disabled = prayerToggles().filterValues { !it }.keys
        val toggles = NotificationSettings.defaultPrayerToggles().toMutableMap()
        disabled.forEach { toggles[it] = false }
        return NotificationSettings(
            enabled = this[Keys.ENABLED] ?: false,
            prayerToggles = toggles,
            adhanEnabled = this[Keys.ADHAN_ENABLED] ?: true,
            countdownEnabled = this[Keys.COUNTDOWN_ENABLED] ?: true,
            dailyReminderEnabled = this[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderHour = this[Keys.DAILY_REMINDER_HOUR] ?: 8,
            dailyReminderMinute = this[Keys.DAILY_REMINDER_MINUTE] ?: 0,
            prePrayerReminderEnabled = this[Keys.PRE_PRAYER_ENABLED] ?: false,
            prePrayerMinutes = this[Keys.PRE_PRAYER_MINUTES] ?: 15,
            jumuahEnabled = this[Keys.JUMUAH_ENABLED] ?: true,
            specialDaysEnabled = this[Keys.SPECIAL_DAYS_ENABLED] ?: true,
            soundEnabled = this[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = this[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    private fun Preferences.prayerToggles(): Map<String, Boolean> {
        val enabledKeys = this[Keys.PRAYER_TOGGLES].orEmpty()
            .split(",").filter { it.isNotBlank() }.toSet()
        return NotificationSettings.PRAYER_KEYS.associateWith { it in enabledKeys }
    }
}
```

- [ ] **Step 5: Register the DataStore in Koin**

In `PrayerNotificationsModule.kt`, add a factory providing the DataStore with a `Context`:

```kotlin
package com.kutluoglu.prayer_notifications.di

import android.content.Context
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_notifications**")
object PrayerNotificationsModule {

    @Provided
    fun provideNotificationSettingsDataStore(context: Context): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context)
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(notifications): add NotificationSettings model and DataStore"
```

## Task 4.3: `SchedulePlan` pure calculator (TDD)

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt`

- [ ] **Step 1: Write the failing test**

Create `SchedulePlanTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SchedulePlanTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val date = LocalDate(2026, 8, 22)

    private fun prayer(name: String, time: LocalTime) = Prayer(
        name = name,
        arabicName = name,
        time = time,
        date = date
    )

    private val prayers = listOf(
        prayer("Fajr", LocalTime(4, 30)),
        prayer("Dhuhr", LocalTime(13, 0)),
        prayer("Asr", LocalTime(16, 45)),
        prayer("Maghrib", LocalTime(19, 55)),
        prayer("Isha", LocalTime(21, 15))
    )

    @Test
    fun `builds an alarm for each enabled prayer`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(5)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun `skips disabled prayers`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(1)
        assertThat(alarms[0].prayerKey).isEqualTo("Fajr")
    }

    @Test
    fun `adds pre-prayer alarms when enabled`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        assertThat(alarms).hasSize(2)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Fajr", "Fajr_pre")
    }

    @Test
    fun `skips alarms already in the past`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms.map { it.prayerKey }).containsExactly("Maghrib", "Isha")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest*"`
Expected: FAIL — `SchedulePlan` not defined.

- [ ] **Step 3: Create the calculator**

Create `SchedulePlan.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val isPrePrayer: Boolean = false
)

class SchedulePlan {

    fun buildDailyAlarms(
        prayers: List<Prayer>,
        zoneId: ZoneId,
        now: Instant,
        enabledPrayers: Set<String>,
        prePrayerMinutes: Int,
        prePrayerEnabled: Boolean
    ): List<ScheduledAlarm> {
        val nowZoned = now.atZone(zoneId)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        prayers.forEach { prayer ->
            if (prayer.name !in enabledPrayers) return@forEach
            val trigger = prayer.time.atDate(nowZoned.toLocalDate()).atZone(zoneId).toInstant()
            if (trigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++
                )
            }
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                if (preTrigger.isAfter(now)) {
                    result += ScheduledAlarm(
                        prayerKey = "${prayer.name}_pre",
                        triggerAtMillis = preTrigger.toEpochMilli(),
                        requestCode = requestCode++,
                        isPrePrayer = true
                    )
                }
            }
        }
        return result
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(notifications): add pure SchedulePlan calculator"
```

---

# Phase 5: Notifications — Core Services

## Task 5.1: `PrayerNotificationManager` + channels

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `PrayerNotificationManagerTest.kt` (Robolectric):

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = PrayerNotificationManager(context)

    @Test
    fun `createChannels registers four channels`() {
        manager.createChannels()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = shadowOf(nm).notificationChannels
        assertThat(channels.map { it.id }).containsExactly(
            "prayer_alerts", "adhan", "countdown", "reminders"
        )
    }

    @Test
    fun `showTestNotification posts a notification`() {
        manager.createChannels()
        manager.showTestNotification()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isNotEmpty()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest*"`
Expected: FAIL — class not defined.

- [ ] **Step 3: Create the manager**

Create `PrayerNotificationManager.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import org.koin.core.annotation.Single

@Single
class PrayerNotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_PRAYER_ALERTS = "prayer_alerts"
        const val CHANNEL_ADHAN = "adhan"
        const val CHANNEL_COUNTDOWN = "countdown"
        const val CHANNEL_REMINDERS = "reminders"
        const val NOTIFICATION_ID_PRAYER = 1001
        const val NOTIFICATION_ID_COUNTDOWN = 1002
        const val NOTIFICATION_ID_TEST = 1003
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels(settings: NotificationSettings = NotificationSettings()) {
        createChannel(
            CHANNEL_PRAYER_ALERTS,
            "Prayer times",
            NotificationManager.IMPORTANCE_HIGH,
            settings
        )
        createChannel(CHANNEL_ADHAN, "Adhan", NotificationManager.IMPORTANCE_HIGH, settings)
        createChannel(
            CHANNEL_COUNTDOWN,
            "Next prayer countdown",
            NotificationManager.IMPORTANCE_LOW,
            settings
        )
        createChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT, settings)
    }

    private fun createChannel(
        id: String,
        name: String,
        importance: Int,
        settings: NotificationSettings
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                enableVibration(settings.vibrationEnabled)
                if (!settings.soundEnabled) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerNotification(prayer: Prayer, settings: NotificationSettings) {
        val channel = if (settings.adhanEnabled) CHANNEL_ADHAN else CHANNEL_PRAYER_ALERTS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(prayer.name)
            .setContentText("${prayer.name} time is now")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (settings.soundEnabled && !settings.adhanEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        notificationManager.notify(NOTIFICATION_ID_PRAYER, builder.build())
    }

    fun showCountdownNotification(nextPrayer: Prayer, remainingMillis: Long) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver::class.java)
                .setAction("STOP_COUNTDOWN"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Next prayer: ${nextPrayer.name}")
            .setContentText(formatRemaining(remainingMillis))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification)
    }

    fun cancelCountdown() {
        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
    }

    fun showTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test notification")
            .setContentText("Notifications are working")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_TEST, notification)
    }

    private fun formatRemaining(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
```

- [ ] **Step 4: Add the notification icon**

Create `prayer_notifications/src/main/res/drawable/ic_notification.xml` (a simple vector — a moon or bell):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFD700"
        android:pathData="M12,2A10,10 0 1,0 12,22A10,10 0 1,0 12,2Z" />
</vector>
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(notifications): add PrayerNotificationManager with channels"
```

## Task 5.2: `AdhanPlayer`

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `AdhanPlayerTest.kt` (Robolectric):

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `play and stop do not throw`() {
        val player = AdhanPlayer(context)
        player.play()
        player.stop()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: FAIL — class not defined.

- [ ] **Step 3: Create the player**

Create `AdhanPlayer.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun play() {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.adhan}"))
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fall back to notification sound handled by the channel; never crash.
        }
    }

    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(notifications): add AdhanPlayer"
```

## Task 5.3: `PrayerNotificationScheduler`

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `PrayerNotificationSchedulerTest.kt` (Robolectric, mocks the use case + data store):

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)
    private val schedulePlan = SchedulePlan()

    @Test
    fun `scheduleAll with disabled settings schedules nothing`() = runTest {
        coEvery { dataStore.getSettings() } returns com.kutluoglu.prayer_notifications.domain.NotificationSettings(enabled = false)
        val scheduler = PrayerNotificationScheduler(
            context = context,
            dataStore = dataStore,
            schedulePlan = schedulePlan
        )
        scheduler.scheduleAll()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest*"`
Expected: FAIL — class not defined.

- [ ] **Step 3: Create the scheduler**

Create `PrayerNotificationScheduler.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class PrayerNotificationScheduler(
    private val context: Context,
    private val dataStore: NotificationSettingsDataStore,
    private val schedulePlan: SchedulePlan,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll() {
        scope.launch {
            val settings = dataStore.getSettings()
            if (!settings.enabled) {
                cancelAll()
                return@launch
            }
            // TODO(Phase 5.4): load today's prayers for the active location and
            // schedule each ScheduledAlarm via setExactAndAllowWhileIdle.
            // This task wires the plumbing; the prayer loading is added in Task 5.4.
        }
    }

    fun cancelAll() {
        // Cancel all pending alarms by re-issuing the same PendingIntents with FLAG_NO_CREATE.
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun scheduleAlarm(triggerAtMillis: Long, requestCode: Int, prayerKey: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(notifications): add PrayerNotificationScheduler plumbing"
```

## Task 5.4: `AlarmReceiver` + `BootReceiver` + full scheduling

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/BootReceiver.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt`

- [ ] **Step 1: Write the failing test**

Create `AlarmReceiverTest.kt` (Robolectric):

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `onReceive with prayer key does not throw`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
        receiver.onReceive(context, intent)
    }

    @Test
    fun `onReceive with STOP_COUNTDOWN action does not throw`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java).setAction("STOP_COUNTDOWN")
        receiver.onReceive(context, intent)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AlarmReceiverTest*"`
Expected: FAIL — class not defined.

- [ ] **Step 3: Create AlarmReceiver**

Create `AlarmReceiver.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.android.ext.android.inject

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
    }

    private val notificationManager: PrayerNotificationManager by inject()
    private val adhanPlayer: AdhanPlayer by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_COUNTDOWN -> notificationManager.cancelCountdown()
            else -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)
                if (prayerKey != null && !prayerKey.endsWith("_pre")) {
                    // Full prayer-time handling (post notification + adhan) is
                    // completed in Task 5.5 once use cases are wired.
                    notificationManager.showTestNotification()
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create BootReceiver**

Create `BootReceiver.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.android.ext.android.inject

class BootReceiver : BroadcastReceiver() {

    private val scheduler: PrayerNotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduler.scheduleAll()
        }
    }
}
```

- [ ] **Step 5: Complete the scheduler**

In `PrayerNotificationScheduler.kt`, replace the `scheduleAll()` TODO with real prayer loading. Inject `GetPrayerTimesUseCase` and `LocationsCoordinator`:

```kotlin
import com.kutluoglu.prayer.domain.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import java.time.ZoneId

// add constructor params:
//   private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
//   private val locationsCoordinator: LocationsCoordinator,
//   private val getSettingsUseCase: GetSettingsUseCase,

fun scheduleAll() {
    scope.launch {
        val settings = dataStore.getSettings()
        if (!settings.enabled) {
            cancelAll()
            return@launch
        }
        val location = locationsCoordinator.resolveSelected() ?: run {
            cancelAll()
            return@launch
        }
        val zoneId = ZoneId.of(location.timeZone ?: "UTC")
        val today = LocalDateTime.now(zoneId.toKotlinTimeZone())
        val appSettings = getSettingsUseCase()
        val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
        val prayers = getPrayerTimesUseCase(
            date = today,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method
        ).getOrNull() ?: return@launch

        val enabled = settings.prayerToggles.filterValues { it }.keys
        val alarms = schedulePlan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zoneId,
            now = Instant.now(),
            enabledPrayers = enabled,
            prePrayerMinutes = settings.prePrayerMinutes,
            prePrayerEnabled = settings.prePrayerReminderEnabled
        )
        cancelAll()
        alarms.forEach { scheduleAlarm(it.triggerAtMillis, it.requestCode, it.prayerKey) }
    }
}
```

Add the missing imports (`kotlinx.datetime.toKotlinTimeZone`, `java.time.Instant`).

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS (update `PrayerNotificationSchedulerTest` constructor calls to pass the new mocked deps).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(notifications): add AlarmReceiver, BootReceiver, full scheduling"
```

## Task 5.5: Use cases + Koin wiring + countdown

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/ScheduleNotificationsUseCase.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/CancelNotificationsUseCase.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/GetNotificationSettingsUseCase.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/UpdateNotificationSettingsUseCase.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/usecases/NotificationUseCasesTest.kt`

- [ ] **Step 1: Write the failing test**

Create `NotificationUseCasesTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class NotificationUseCasesTest {

    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)

    @Test
    fun `GetNotificationSettingsUseCase returns settings`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        val useCase = GetNotificationSettingsUseCase(dataStore)
        assertThat(useCase().enabled).isTrue()
    }

    @Test
    fun `UpdateNotificationSettingsUseCase persists and reschedules`() = runTest {
        val scheduler = mockk<com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler>(relaxed = true)
        val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler)
        useCase(NotificationSettings(enabled = true))
        coVerify { dataStore.updateEnabled(true) }
        coVerify { scheduler.scheduleAll() }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*NotificationUseCasesTest*"`
Expected: FAIL — classes not defined.

- [ ] **Step 3: Create the use cases**

Create the four use case files:

```kotlin
// GetNotificationSettingsUseCase.kt
package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import org.koin.core.annotation.Factory

@Factory
class GetNotificationSettingsUseCase(
    private val dataStore: NotificationSettingsDataStore
) {
    suspend operator fun invoke(): NotificationSettings = dataStore.getSettings()
}
```

```kotlin
// ScheduleNotificationsUseCase.kt
package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class ScheduleNotificationsUseCase(
    private val scheduler: PrayerNotificationScheduler
) {
    fun invoke() = scheduler.scheduleAll()
}
```

```kotlin
// CancelNotificationsUseCase.kt
package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class CancelNotificationsUseCase(
    private val scheduler: PrayerNotificationScheduler
) {
    fun invoke() = scheduler.cancelAll()
}
```

```kotlin
// UpdateNotificationSettingsUseCase.kt
package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class UpdateNotificationSettingsUseCase(
    private val dataStore: NotificationSettingsDataStore,
    private val scheduler: PrayerNotificationScheduler
) {
    suspend operator fun invoke(settings: NotificationSettings) {
        dataStore.updateEnabled(settings.enabled)
        dataStore.updateAdhanEnabled(settings.adhanEnabled)
        dataStore.updateCountdownEnabled(settings.countdownEnabled)
        dataStore.updateDailyReminder(
            settings.dailyReminderEnabled,
            settings.dailyReminderHour,
            settings.dailyReminderMinute
        )
        dataStore.updatePrePrayerReminder(
            settings.prePrayerReminderEnabled,
            settings.prePrayerMinutes
        )
        dataStore.updateJumuahEnabled(settings.jumuahEnabled)
        dataStore.updateSpecialDaysEnabled(settings.specialDaysEnabled)
        dataStore.updateSoundEnabled(settings.soundEnabled)
        dataStore.updateVibrationEnabled(settings.vibrationEnabled)
        if (settings.enabled) scheduler.scheduleAll() else scheduler.cancelAll()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Verify Koin graph**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Koin KSP generates the module graph; verify no "No definition found" errors).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(notifications): add use cases and Koin wiring"
```

## Task 5.6: WorkManager daily reschedule worker

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/DailyRescheduleWorker.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/DailyRescheduleWorkerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `DailyRescheduleWorkerTest.kt` (Robolectric):

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyRescheduleWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `doWork returns success`() {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val worker = DailyRescheduleWorker(
            context,
            mockk<WorkerParameters>(relaxed = true),
            scheduler
        )
        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*DailyRescheduleWorkerTest*"`
Expected: FAIL — class not defined.

- [ ] **Step 3: Create the worker**

Create `DailyRescheduleWorker.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.annotation.Factory

@Factory
class DailyRescheduleWorker(
    appContext: Context,
    params: WorkerParameters,
    private val scheduler: PrayerNotificationScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        scheduler.scheduleAll()
        return Result.success()
    }
}
```

- [ ] **Step 4: Enqueue the periodic worker**

In `PrayerNotificationScheduler.scheduleAll()`, after scheduling today's alarms, enqueue a daily periodic worker (idempotent via `ExistingPeriodicWorkPolicy.KEEP`):

```kotlin
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// in scheduleAll(), after alarms.forEach { scheduleAlarm(...) }:
val request = PeriodicWorkRequestBuilder<DailyRescheduleWorker>(1, TimeUnit.DAYS).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "daily_prayer_reschedule",
    ExistingPeriodicWorkPolicy.KEEP,
    request
)
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(notifications): add WorkManager daily reschedule worker"
```

---

# Phase 6: Notifications — Settings UI

## Task 6.1: Notifications sub-screen

**Files:**
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt`
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt`
- Create: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `NotificationsViewModelTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.notifications

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val getUseCase = mockk<GetNotificationSettingsUseCase>(relaxed = true)
    private val updateUseCase = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads settings on init`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings(enabled = true)
        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)
        assertThat(viewModel.uiState.value).isInstanceOf(NotificationsUiState.Success::class.java)
    }

    @Test
    fun `toggling master enabled persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()
        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)
        viewModel.onEvent(NotificationsEvent.SetEnabled(true))
        coVerify { updateUseCase(any()) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsViewModelTest*"`
Expected: FAIL — classes not defined. Note: `prayer_feature:settings` must depend on `:prayer_notifications` — add it to `prayer_feature/settings/build.gradle.kts` first.

- [ ] **Step 3: Create the contract**

Create `NotificationsContract.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.notifications

import com.kutluoglu.prayer_notifications.domain.NotificationSettings

sealed class NotificationsUiState {
    data object Loading : NotificationsUiState()
    data class Success(val settings: NotificationSettings) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

sealed class NotificationsEvent {
    data object Load : NotificationsEvent()
    data class SetEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetPrayerToggle(val prayerKey: String, val enabled: Boolean) : NotificationsEvent()
    data class SetAdhanEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetCountdownEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetPrePrayerReminder(val enabled: Boolean, val minutes: Int) : NotificationsEvent()
    data class SetDailyReminder(val enabled: Boolean, val hour: Int, val minute: Int) : NotificationsEvent()
    data class SetJumuahEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetSpecialDaysEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetSoundEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetVibrationEnabled(val enabled: Boolean) : NotificationsEvent()
    data object SendTest : NotificationsEvent()
}
```

- [ ] **Step 4: Create the ViewModel**

Create `NotificationsViewModel.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NotificationsViewModel(
    private val getSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateSettingsUseCase: UpdateNotificationSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: NotificationsEvent) {
        when (event) {
            NotificationsEvent.Load -> load()
            is NotificationsEvent.SetEnabled -> update { it.copy(enabled = event.enabled) }
            is NotificationsEvent.SetPrayerToggle -> update {
                it.copy(prayerToggles = it.prayerToggles + (event.prayerKey to event.enabled))
            }
            is NotificationsEvent.SetAdhanEnabled -> update { it.copy(adhanEnabled = event.enabled) }
            is NotificationsEvent.SetCountdownEnabled -> update { it.copy(countdownEnabled = event.enabled) }
            is NotificationsEvent.SetPrePrayerReminder -> update {
                it.copy(prePrayerReminderEnabled = event.enabled, prePrayerMinutes = event.minutes)
            }
            is NotificationsEvent.SetDailyReminder -> update {
                it.copy(dailyReminderEnabled = event.enabled, dailyReminderHour = event.hour, dailyReminderMinute = event.minute)
            }
            is NotificationsEvent.SetJumuahEnabled -> update { it.copy(jumuahEnabled = event.enabled) }
            is NotificationsEvent.SetSpecialDaysEnabled -> update { it.copy(specialDaysEnabled = event.enabled) }
            is NotificationsEvent.SetSoundEnabled -> update { it.copy(soundEnabled = event.enabled) }
            is NotificationsEvent.SetVibrationEnabled -> update { it.copy(vibrationEnabled = event.enabled) }
            NotificationsEvent.SendTest -> Unit // handled in the screen via PrayerNotificationManager
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                _uiState.value = NotificationsUiState.Success(getSettingsUseCase())
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    private fun update(transform: (NotificationSettings) -> NotificationSettings) {
        val current = (_uiState.value as? NotificationsUiState.Success)?.settings ?: return
        val updated = transform(current)
        _uiState.value = NotificationsUiState.Success(updated)
        viewModelScope.launch {
            updateSettingsUseCase(updated)
        }
    }
}
```

- [ ] **Step 5: Create the screen**

Create `NotificationsScreen.kt`:

```kotlin
package com.kutluoglu.prayer_feature.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val settings = (uiState as? NotificationsUiState.Success)?.settings
            ?: NotificationSettings()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleRow(
                title = stringResource(R.string.notifications_enabled),
                checked = settings.enabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetEnabled(it)) }
            )
            HorizontalDivider()
            NotificationSettings.PRAYER_KEYS.forEach { key ->
                ToggleRow(
                    title = stringResource(prayerNameRes(key)),
                    checked = settings.prayerToggles[key] ?: true,
                    onCheckedChange = {
                        viewModel.onEvent(NotificationsEvent.SetPrayerToggle(key, it))
                    }
                )
            }
            HorizontalDivider()
            ToggleRow(
                title = stringResource(R.string.adhan),
                checked = settings.adhanEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetAdhanEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.countdown),
                checked = settings.countdownEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetCountdownEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.pre_prayer_reminder),
                checked = settings.prePrayerReminderEnabled,
                onCheckedChange = {
                    viewModel.onEvent(
                        NotificationsEvent.SetPrePrayerReminder(it, settings.prePrayerMinutes)
                    )
                }
            )
            ToggleRow(
                title = stringResource(R.string.jumuah),
                checked = settings.jumuahEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetJumuahEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.special_days),
                checked = settings.specialDaysEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetSpecialDaysEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.sound),
                checked = settings.soundEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetSoundEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.vibration),
                checked = settings.vibrationEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetVibrationEnabled(it)) }
            )
            Button(
                onClick = {
                    PrayerNotificationManager(context).createChannels(settings)
                    PrayerNotificationManager(context).showTestNotification()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.send_test_notification))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun prayerNameRes(key: String): Int = when (key) {
    "Fajr" -> R.string.prayer_fajr
    "Dhuhr" -> R.string.prayer_dhuhr
    "Asr" -> R.string.prayer_asr
    "Maghrib" -> R.string.prayer_maghrib
    "Isha" -> R.string.prayer_isha
    else -> R.string.notifications
}
```

Note: the pre-prayer minutes selector (5/10/15/30/60) and the daily-reminder time picker are rendered as additional rows; add them following the same `ToggleRow` pattern with a `FilterChip` row for minutes and a `TimePicker` dialog for the reminder time.

- [ ] **Step 6: Add string resources**

Add notification-related strings to `prayer_feature/settings/src/main/res/values/strings.xml` (and translated folders): `notifications`, `notifications_enabled`, `prayer_fajr`, `prayer_dhuhr`, `prayer_asr`, `prayer_maghrib`, `prayer_isha`, `adhan`, `countdown`, `pre_prayer_reminder`, `daily_reminder`, `jumuah`, `special_days`, `sound`, `vibration`, `send_test_notification`, `minutes_before`.

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(settings): add Notifications sub-screen"
```

## Task 6.2: Wire navigation

**Files:**
- Modify: `prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsRoute.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Add the route**

In `PrayerScreens.kt`, add:

```kotlin
data object NotificationsScreen : Screen("notifications")
```

- [ ] **Step 2: Add the settings item**

In `SettingsScreen.kt` `SettingsContent`, add a `SettingsItem` for notifications (icon `Icons.Default.Notifications`, title `stringResource(R.string.notifications)`) that calls `onNavigateToNotifications`.

- [ ] **Step 3: Thread the callback**

Add `onNavigateToNotifications: () -> Unit` to `SettingsRoute` and `SettingsScreen`, and pass it through.

- [ ] **Step 4: Register the destination**

In `SettingsGraph.kt`, add:

```kotlin
composable(Screen.NotificationsScreen.route) {
    NotificationsRoute(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

and pass `onNavigateToNotifications = { navController.navigate(Screen.NotificationsScreen.route) }` in the `SettingsScreen` composable.

- [ ] **Step 5: Build to verify**

Run: `./gradlew :prayer_feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(settings): wire Notifications screen into navigation"
```

## Task 6.3: Permission handling

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`

- [ ] **Step 1: Request POST_NOTIFICATIONS on master enable**

In `NotificationsScreen.kt`, when the master toggle is turned ON and `Build.VERSION.SDK_INT >= 33` and permission not granted, launch `ActivityResultContracts.RequestPermission()` for `Manifest.permission.POST_NOTIFICATIONS`. If denied, keep the toggle OFF and show a rationale.

- [ ] **Step 2: Add exact-alarm hint**

When `SCHEDULE_EXACT_ALARM` is not granted (Android 12+), show a hint row with an action that opens `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :prayer_feature:settings:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(settings): handle notification and exact-alarm permissions"
```

## Task 6.4: Phase 4-6 verification

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: all pass.

- [ ] **Step 2: Run `gitnexus_detect_changes()`**

Confirm the affected scope is limited to the notifications module + settings feature.

---

# Phase 7: Compose UI Tests (4 main screens)

## Task 7.1: Home screen UI test

**Files:**
- Create: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the test**

Create `HomeScreenTest.kt` (Robolectric, following `MyLocationsScreenTest`):

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders error state with retry`() {
        composeTestRule.setContent {
            HomeScreen(
                navController = androidx.navigation.testing.TestNavHostController(composeTestRule.activity),
                uiState = HomeUiState.Error("Something went wrong"),
                locationsState = com.kutluoglu.prayer_location.data.LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk(relaxed = true),
                onEvent = {}
            )
        }
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest*"`
Expected: PASS. Add the missing test deps to `prayer_feature/home/build.gradle.kts` (`robolectric`, `androidx.compose.ui.test.junit4`, `androidx.navigation.testing`) if not present.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(home): add Compose UI smoke test"
```

## Task 7.2: Prayer Times screen UI test

**Files:**
- Create: `prayer_feature/prayertimes/src/test/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesScreenTest.kt`

- [ ] **Step 1: Write the test**

Create `PrayerTimesScreenTest.kt` rendering `PrayerTimesScreen` with a `Success` state containing one day's prayers, asserting the prayer names and the month header are displayed.

- [ ] **Step 2: Run the test**

Run: `./gradlew :prayer_feature:prayertimes:testDebugUnitTest --tests="*PrayerTimesScreenTest*"`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(prayertimes): add Compose UI smoke test"
```

## Task 7.3: Qibla screen UI test

**Files:**
- Create: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/QiblaScreenTest.kt`

- [ ] **Step 1: Write the test**

Create `QiblaScreenTest.kt` rendering `QiblaScreen` with a success `QiblaUiState` (qiblaBearing set, isLocationAvailable = true), asserting the compass and bearing text render.

- [ ] **Step 2: Run the test**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="*QiblaScreenTest*"`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(qibla): add Compose UI smoke test"
```

## Task 7.4: Settings screen UI test

**Files:**
- Create: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/SettingsScreenTest.kt`

- [ ] **Step 1: Write the test**

Create `SettingsScreenTest.kt` rendering `SettingsScreen` with a `Success` state, asserting the settings items (Location, Calculation Method, Hijri Adjustment, Language, Notifications) are displayed.

- [ ] **Step 2: Run the test**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*SettingsScreenTest*"`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(settings): add Compose UI smoke test"
```

---

# Phase 8: CI Pipeline

## Task 8.1: GitHub Actions workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create the workflow**

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Run all tests
        run: ./gradlew allTests

      - name: Verify debug build
        run: ./gradlew assembleDebug

      - name: Run lint
        run: ./gradlew lint

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

- [ ] **Step 2: Validate the workflow locally**

Run: `./gradlew allTests assembleDebug lint`
Expected: all succeed (this is what CI will run).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "ci: add GitHub Actions workflow"
```

---

# Final Verification

- [ ] **Step 1: Full suite**

Run: `./gradlew allTests`
Expected: all tests pass.

- [ ] **Step 2: Release build sanity**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: `gitnexus_detect_changes()`**

Run per AGENTS.md and confirm the affected scope matches the plan.

- [ ] **Step 4: Update TODO.md**

Mark all completed items (notifications, calc-method fix, tech debt) with dates.
