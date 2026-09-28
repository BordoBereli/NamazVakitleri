# Per-App Language Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate both `runBlocking` DataStore reads at cold start by migrating locale handling to Android's per-app language API (`AppCompatDelegate.setApplicationLocales`).

**Architecture:** The OS/AppCompat stores the language preference and applies the correct locale to each activity's base context before `attachBaseContext` runs. A thin `AppLocaleController` maps the app's language codes (`"system"`, `"tr"`, `"ar"`, …) to `LocaleListCompat`. A one-time flag-gated migration copies the DataStore `language` value into the system API. The custom `LocaleManager` and both `attachBaseContext` overrides are deleted.

**Tech Stack:** AndroidX AppCompat 1.7.1 (already in version catalog), Jetpack Compose, Koin (KSP annotations), kotlinx-coroutines, JUnit 5 + Robolectric + MockK + Truth.

**Spec:** `docs/superpowers/specs/2026-09-28-per-app-language-migration-design.md`

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `app/src/main/java/com/kutluoglu/namazvakitleri/locale/AppLocaleController.kt` | Create | Maps language codes ↔ `LocaleListCompat`; wraps `AppCompatDelegate` calls |
| `app/src/test/java/com/kutluoglu/namazvakitleri/locale/AppLocaleControllerTest.kt` | Create | Robolectric tests for the mapping |
| `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt` | Delete | Replaced by AppLocaleController + system API |
| `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt` | Delete | Tests deleted class |
| `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt` | Delete | Tests deleted class |
| `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleMigration.kt` | Create | One-time DataStore → system API migration |
| `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleMigrationTest.kt` | Create | Tests for the migration logic |
| `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt` | Modify | `AppCompatActivity` superclass; delete `attachBaseContext` |
| `app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt` | Modify | Delete `attachBaseContext`; start migration in `onCreate` |
| `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt` | Modify | `applyLanguage` → single `AppLocaleController` call |
| `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt` | Modify | Remove `provideLocaleManager`; add `AppLocaleController` + `LocaleMigration` |
| `app/src/main/res/values/themes.xml` | Modify | AppCompat theme parent |
| `app/src/main/res/xml/locales_config.xml` | Create | 15 supported locales for system settings |
| `app/src/main/AndroidManifest.xml` | Modify | `localeConfig` attribute + `AppLocalesMetadataHolderService` |

---

### Task 1: Create `AppLocaleController` with tests (TDD)

**Files:**
- Create: `app/src/main/java/com/kutluoglu/namazvakitleri/locale/AppLocaleController.kt`
- Test: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/AppLocaleControllerTest.kt`

- [ ] **Step 1: Add appcompat dependency to `:app`**

`app/build.gradle.kts` currently has NO appcompat dependency (verified: `grep appcompat app/build.gradle.kts` returns nothing). Other modules declare `implementation(libs.androidx.appcompat)`; the catalog entry already exists at `gradle/libs.versions.toml:137` (appcompat 1.7.1).

In `app/build.gradle.kts`, in the `dependencies` block, after `implementation(libs.androidx.core.splashscreen)` (line ~115), add:

```kotlin
    implementation(libs.androidx.appcompat)
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/kutluoglu/namazvakitleri/locale/AppLocaleControllerTest.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AppLocaleControllerTest {

    private val controller = AppLocaleController()

    @Test
    fun `toLocaleList returns empty list for system`() {
        assertThat(controller.toLocaleList("system")).isEqualTo(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `toLocaleList returns locale for explicit code`() {
        assertThat(controller.toLocaleList("tr")).isEqualTo(LocaleListCompat.forLanguageTags("tr"))
    }

    @Test
    fun `toLocaleList handles region-qualified code`() {
        assertThat(controller.toLocaleList("pt-BR")).isEqualTo(LocaleListCompat.forLanguageTags("pt-BR"))
    }

    @Test
    fun `toLanguageCode returns system for empty list`() {
        assertThat(controller.toLanguageCode(LocaleListCompat.getEmptyLocaleList())).isEqualTo("system")
    }

    @Test
    fun `toLanguageCode returns language tag for set locales`() {
        assertThat(controller.toLanguageCode(LocaleListCompat.forLanguageTags("ar"))).isEqualTo("ar")
    }

    @Test
    fun `round trip system stays system`() {
        val list = controller.toLocaleList("system")
        assertThat(controller.toLanguageCode(list)).isEqualTo("system")
    }

    @Test
    fun `round trip explicit code survives`() {
        val list = controller.toLocaleList("de")
        assertThat(controller.toLanguageCode(list)).isEqualTo("de")
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests="*AppLocaleControllerTest*"`
Expected: FAIL — `AppLocaleController` unresolved reference (compilation error).

- [ ] **Step 4: Write minimal implementation**

Create `app/src/main/java/com/kutluoglu/namazvakitleri/locale/AppLocaleController.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate

/**
 * Maps the app's language codes to the Android per-app language API.
 *
 * "system" maps to an empty locale list (follow the device language); any other
 * value is treated as a BCP-47 language tag (e.g. "tr", "pt-BR").
 */
class AppLocaleController {

    fun toLocaleList(languageCode: String): LocaleListCompat =
        if (languageCode == SYSTEM_LANGUAGE) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }

    fun toLanguageCode(locales: LocaleListCompat): String =
        if (locales.isEmpty) SYSTEM_LANGUAGE else locales[0]!!.toLanguageTag()

    fun setApplicationLocales(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(toLocaleList(languageCode))
    }

    fun getApplicationLocales(): String =
        toLanguageCode(AppCompatDelegate.getApplicationLocales())

    companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests="*AppLocaleControllerTest*"`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/kutluoglu/namazvakitleri/locale/AppLocaleController.kt app/src/test/java/com/kutluoglu/namazvakitleri/locale/AppLocaleControllerTest.kt
git commit -m "feat: add AppLocaleController wrapping per-app language API"
```

---

### Task 2: Create `LocaleMigration` with tests (TDD)

**Files:**
- Create: `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleMigration.kt`
- Test: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleMigrationTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleMigrationTest.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class LocaleMigrationTest {

    private val controller = mockk<AppLocaleController>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private fun prefs(): android.content.SharedPreferences =
        context.getSharedPreferences("locale_migration", Context.MODE_PRIVATE)

    @Test
    fun `migrates non-system language to system API`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "de")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify { controller.setApplicationLocales("de") }
    }

    @Test
    fun `skips system language`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "system")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `runs only once - flag prevents second migration`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "de")

        val migration = LocaleMigration(context, controller, dataStore)
        migration.migrateIfNeeded()
        migration.migrateIfNeeded()

        coVerify(exactly = 1) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `skips when system locales already match datastore language`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "tr")
        every { controller.getApplicationLocales() } returns "tr"

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `never throws on datastore failure`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } throws RuntimeException("disk error")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    private fun settingsWith(language: String): com.kutluoglu.prayer_settings.domain.model.Settings =
        com.kutluoglu.prayer_settings.domain.model.Settings(language = language)
}
```

Note: `Settings` is a data class where every field has a default (`prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/Settings.kt:3`), so `Settings(language = "de")` is valid. Import `io.mockk.coVerify` directly — do not define a local `coVerify` helper.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests="*LocaleMigrationTest*"`
Expected: FAIL — `LocaleMigration` unresolved reference.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleMigration.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * One-time migration of the persisted DataStore language preference into the
 * per-app language API (AppCompat storage).
 *
 * Flag-gated via SharedPreferences so it never overrides a language the user
 * later changed through system settings. Runs async from Application.onCreate.
 */
class LocaleMigration(
    private val context: Context,
    private val controller: AppLocaleController,
    private val settingsDataStore: SettingsDataStore
) {

    suspend fun migrateIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return

        try {
            val language = settingsDataStore.observeSettings().first().language
            if (language != AppLocaleController.SYSTEM_LANGUAGE &&
                controller.getApplicationLocales() != language
            ) {
                controller.setApplicationLocales(language)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Migration must never crash the app; the flag is still set so we
            // don't retry a permanently failing read.
        } finally {
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "locale_migration"
        private const val KEY_MIGRATED = "locale_migrated"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests="*LocaleMigrationTest*"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleMigration.kt app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleMigrationTest.kt
git commit -m "feat: add one-time DataStore-to-system locale migration"
```

---

### Task 3: Register in Koin (`AppModule`)

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt`

- [ ] **Step 1: Add `AppLocaleController` and `LocaleMigration` providers**

In `AppModule.kt`, add imports:

```kotlin
import com.kutluoglu.namazvakitleri.locale.AppLocaleController
import com.kutluoglu.namazvakitleri.locale.LocaleMigration
```

Add two `@Single` providers inside `object AppModule` (after `provideAppVersion`):

```kotlin
    @Single
    fun provideAppLocaleController(): AppLocaleController = AppLocaleController()

    @Single
    fun provideLocaleMigration(
        context: Context,
        controller: AppLocaleController,
        settingsDataStore: SettingsDataStore
    ): LocaleMigration = LocaleMigration(context, controller, settingsDataStore)
```

`Context` and `SettingsDataStore` are already resolvable (`provideSettingsDataStore` exists at the bottom of the module; `Context` comes from `androidContext`).

- [ ] **Step 2: Verify the graph still boots**

Run: `./gradlew :app:testDebugUnitTest --tests="*KoinGraphVerificationTest*"`
Expected: PASS — no duplicate definitions, all ViewModels resolve.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt
git commit -m "feat: register AppLocaleController and LocaleMigration in Koin"
```

---

### Task 4: Switch `MainActivity` to `AppCompatActivity` + AppCompat theme

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt`
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Change the superclass and delete `attachBaseContext`**

In `MainActivity.kt`:

Replace import `androidx.activity.ComponentActivity` with:

```kotlin
import androidx.appcompat.app.AppCompatActivity
```

Replace the class declaration:

```kotlin
class MainActivity : AppCompatActivity() {
```

Delete the entire `attachBaseContext` override (lines 21-25):

```kotlin
    override fun attachBaseContext(newBase: Context) {
        val localeManager = get<LocaleManager>()
        val settingsDataStore = get<SettingsDataStore>()
        super.attachBaseContext(localeManager.applyPersistedLocale(newBase, settingsDataStore))
    }
```

Also remove now-unused imports: `android.content.Context`, `com.kutluoglu.namazvakitleri.locale.LocaleManager`, `com.kutluoglu.prayer_settings.data.local.SettingsDataStore`, `org.koin.android.ext.android.get` (verify each is unused before removing — `get` may be used elsewhere in the file; it is not).

- [ ] **Step 2: Change the theme parent**

In `app/src/main/res/values/themes.xml`, replace:

```xml
    <style name="Theme.NamazVakitleri" parent="android:Theme.Material.NoActionBar">
```

with:

```xml
    <style name="Theme.NamazVakitleri" parent="Theme.AppCompat.DayNight.NoActionBar">
```

Keep the `windowBackground` item unchanged. The splash theme (`Theme.NamazVakitleri.Starting`, parent `Theme.SplashScreen`) is unchanged.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt app/src/main/res/values/themes.xml
git commit -m "feat: switch MainActivity to AppCompatActivity with AppCompat theme"
```

---

### Task 5: Manifest — `localeConfig` + `AppLocalesMetadataHolderService`

**Files:**
- Create: `app/src/main/res/xml/locales_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `locales_config.xml`**

Create `app/src/main/res/xml/locales_config.xml` (the `res/xml/` directory does not exist yet — create it):

```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="tr" />
    <locale android:name="en" />
    <locale android:name="ar" />
    <locale android:name="de" />
    <locale android:name="fr" />
    <locale android:name="es" />
    <locale android:name="bn" />
    <locale android:name="fa" />
    <locale android:name="hi" />
    <locale android:name="id" />
    <locale android:name="ms" />
    <locale android:name="ru" />
    <locale android:name="ta" />
    <locale android:name="th" />
    <locale android:name="ur" />
</locale-config>
```

- [ ] **Step 2: Update the manifest**

In `app/src/main/AndroidManifest.xml`, add to the `<application>` element:

```xml
        android:localeConfig="@xml/locales_config"
```

(after `android:label`, before `android:roundIcon` — attribute order is irrelevant).

Inside `<application>`, after the `<activity>` element, add:

```xml
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <intent-filter>
                <action android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" />
            </intent-filter>
            <meta-data
                android:name="autoStoreLocales"
                android:value="true" />
        </service>
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/xml/locales_config.xml app/src/main/AndroidManifest.xml
git commit -m "feat: declare per-app language support in manifest"
```

---

### Task 6: Wire `Application.onCreate` migration + delete `attachBaseContext`

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt`

- [ ] **Step 1: Delete `attachBaseContext` and its doc comment**

Delete lines 39-57 (the KDoc block starting `Applies the persisted locale...` and the override):

```kotlin
    /**
     * Applies the persisted locale before any activity is created.
     * ... (entire KDoc)
     */
    override fun attachBaseContext(base: Context) {
        val localeManager = LocaleManager()
        super.attachBaseContext(localeManager.applyPersistedLocale(base, SettingsDataStore.create(base)))
    }
```

Remove now-unused imports: `com.kutluoglu.namazvakitleri.locale.LocaleManager`, `com.kutluoglu.prayer_settings.data.local.SettingsDataStore` (verify `SettingsDataStore` is not used elsewhere in the file first — it is not), and `kotlinx.coroutines.runBlocking` is NOT imported in this file (it lives in LocaleManager) — nothing else to remove.

- [ ] **Step 2: Start the migration in `onCreate`**

Add a private method after `applyCrashlyticsConsent()`:

```kotlin
    private fun startLocaleMigration() {
        applicationScope.launch {
            runCatching {
                get<LocaleMigration>().migrateIfNeeded()
            }.onFailure {
                android.util.Log.e("NamazVakitleriApp", "Locale migration failed -> ${it.message}")
            }
        }
    }
```

Add the import:

```kotlin
import com.kutluoglu.namazvakitleri.locale.LocaleMigration
```

Call it in `onCreate()` right after `startKoin { ... }` completes and before `applyCrashlyticsConsent()`:

```kotlin
        startLocaleMigration()
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt
git commit -m "feat: run one-time locale migration from Application.onCreate"
```

---

### Task 7: Update `MainAppScreen.applyLanguage` to use the system API

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt`

- [ ] **Step 1: Replace the `applyLanguage` body**

Current (lines 89-92):

```kotlin
    fun applyLanguage(language: String) {
        activity?.get<LocaleManager>()?.setLanguage(language)
        activity?.recreate()
    }
```

Replace with:

```kotlin
    fun applyLanguage(language: String) {
        koinInject<AppLocaleController>().setApplicationLocales(language)
    }
```

Better form — resolve once at the top of the composable with the other injections (after `val analyticsTracker: AnalyticsTracker = koinInject()` at line 60):

```kotlin
    val appLocaleController: AppLocaleController = koinInject()
```

then:

```kotlin
    fun applyLanguage(language: String) {
        appLocaleController.setApplicationLocales(language)
    }
```

Add import:

```kotlin
import com.kutluoglu.namazvakitleri.locale.AppLocaleController
```

Remove now-unused imports: `com.kutluoglu.namazvakitleri.locale.LocaleManager`, `org.koin.android.ext.android.get` (verify `get` is not used elsewhere in the file — it is not; `findActivity()` and `activity` remain used by nothing else after this change — check: `activity` is only used by `applyLanguage`, so also remove `val context = LocalContext.current` / `val activity = context.findActivity()` at lines 72-73 and the `findActivity`/`ContextWrapper`/`Activity` imports if they become unused).

Note: `AppCompatDelegate.setApplicationLocales` triggers the activity recreation itself on locale change — no manual `recreate()`.

- [ ] **Step 2: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt
git commit -m "feat: apply language via per-app language API in MainAppScreen"
```

---

### Task 8: Delete `LocaleManager` and its tests

**Files:**
- Delete: `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt`
- Delete: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt`
- Delete: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt`

- [ ] **Step 1: Remove `provideLocaleManager` from `AppModule`**

Delete from `AppModule.kt`:

```kotlin
    @Single
    fun provideLocaleManager(): LocaleManager = LocaleManager()
```

and the import `com.kutluoglu.namazvakitleri.locale.LocaleManager`.

- [ ] **Step 2: Delete the three files**

```bash
git rm app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt
```

- [ ] **Step 3: Verify no remaining references**

Run: `grep -rn "LocaleManager" --include="*.kt" app/ prayer_feature/ core/ prayer_settings/`
Expected: no matches (the `LanguageProvider` doc comment in `core/designsystem/.../LanguageProvider.kt:11` mentions LocaleManager in a comment — update that comment to reference the per-app language API instead):

In `core/designsystem/src/main/java/com/kutluoglu/core/designsystem/utils/LanguageProvider.kt`, replace the comment block:

```kotlin
    /**
     * Uygulamanın geçerli dil kodunu (örn: "en", "tr") döndürür.
     * LocaleManager, kalıcı Settings.language tercihini Locale.setDefault ile senkronize ettiği
     * için bu değer cihaz dilini değil uygulama içi dil tercihini yansıtır.
     */
```

with:

```kotlin
    /**
     * Uygulamanın geçerli dil kodunu (örn: "en", "tr") döndürür.
     * Per-app language API (AppCompatDelegate), Locale.setDefault değerini
     * uygulama diline senkronize ettiği için bu değer cihaz dilini değil
     * uygulama içi dil tercihini yansıtır.
     */
```

- [ ] **Step 4: Run the full app test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — including `KoinGraphVerificationTest` (LocaleManager is no longer in the graph; nothing references it).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: delete LocaleManager in favor of per-app language API"
```

---

### Task 9: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all modules pass.

- [ ] **Step 2: Run the release build (R8)**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL — verifies AppCompat + R8 rules coexist (AppCompat ships consumer rules; no new keep rules needed).

- [ ] **Step 3: Graph change analysis (GitNexus)**

Run: `node .gitnexus/run.cjs detect-changes --scope all --repo .`
Expected: changed symbols limited to the locale/MainActivity/MainAppScreen/AppModule area; no unexpected processes affected.

- [ ] **Step 4: Manual smoke test on device/emulator**

1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Cold start with device language ≠ app language → first frame shows the app language (no flash).
3. Settings → Language → pick another language → UI switches without double-recreate flash.
4. Kill + relaunch → chosen language persists.
5. Android 13+ device: Settings → Apps → NamazVakitleri → Language is available and lists 15 locales.
6. Change language from system settings → app follows on next launch.

- [ ] **Step 5: Commit any remaining changes (if manual test required fixes)**

```bash
git status --short
# if clean, nothing to do
```

---

## Rollback

Each task is an independent commit; `git revert` per commit is safe. The riskiest step is Task 4 (theme + superclass) — if Compose rendering regresses, revert Task 4 and Tasks 5-8 still compile (AppLocaleController/LocaleMigration don't depend on AppCompatActivity; only `setApplicationLocales` recreation behavior depends on it, which Tasks 6-7 exercise).

## Out of scope (per spec)

- Removing the DataStore `language` key / `UpdateLanguageUseCase` (still used for Settings display + analytics)
- `LanguageProvider` refactor beyond its doc comment
- Baseline Profile module (separate review item)
