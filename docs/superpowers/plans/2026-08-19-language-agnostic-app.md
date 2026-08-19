# Language-Agnostic App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the app language-agnostic — follow the phone's system language by default, and apply an in-app language override (from Settings → Language) to all texts immediately.

**Architecture:** Change the persisted default language from `"tr"` to `"system"`. Add a `LocaleManager` (app module) that wraps the base context with the chosen locale in `attachBaseContext` (Application + MainActivity); on selection, persist via the existing `UpdateLanguageUseCase`, update the manager's in-memory holder, then `activity.recreate()`. Add a "Follow system" entry to the language picker and full translations for all 15 languages across all modules.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (KSP), DataStore, JUnit 5 + MockK + Truth + Turbine.

**Translation content:** The translation tasks (9–14) reference appendix files under `docs/superpowers/plans/translations/`. Each appendix contains the complete `strings.xml` content for every language. Copy the file contents verbatim into the target `res/values-*/strings.xml` paths.

---

## File Structure

**Logic changes (TDD):**
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/Settings.kt` — default `language = "system"`
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStore.kt` — default `"system"`
- Modify: `prayer_cache/src/main/java/com/kutluoglu/prayer_cache/SettingsDataStoreImp.kt` — default `"system"`
- Create: `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt` — register `LocaleManager` if KSP does not auto-discover it
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt` — unchanged except removing nothing (Koin setup stays)
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt` — `attachBaseContext` reads persisted language synchronously and applies locale
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt` — thread `onLanguageSelected` → `setLanguage` + `recreate()`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionScreen.kt` — add "system" entry
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModel.kt` — default `"system"`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt` — pass `onLanguageSelected` up

**Tests:**
- Modify: `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/domain/model/SettingsTest.kt`
- Modify: `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStoreTest.kt`
- Create: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt`
- Create: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt`
- Modify: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModelTest.kt`

**Translations (new `values-*` XML files):**
- `core/designsystem/src/main/res/values-{ur,ms,fa,bn,hi,ta,th,ru,es}/strings.xml`
- `prayer_feature/settings/src/main/res/values-{tr,ar,ur,id,ms,fa,bn,hi,ta,th,ru,fr,de,es}/strings.xml`
- `prayer_feature/home/src/main/res/values-{ar,ur,id,ms,fa,bn,hi,ta,th,ru,fr,de,es}/strings.xml`
- `prayer_feature/qibla/src/main/res/values-{ar,ur,id,ms,fa,bn,hi,ta,th,ru,fr,de,es}/strings.xml`
- `prayer_feature/prayertimes/src/main/res/values-{ar,ur,id,ms,fa,bn,hi,ta,th,ru,fr,de,es}/strings.xml`
- `prayer_navigation/core/src/main/res/values-{ar,ur,id,ms,fa,bn,hi,ta,th,ru,fr,de,es}/strings.xml`

---

## Task 1: Default language → `"system"` (domain model)

**Files:**
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/Settings.kt:6`
- Test: `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/domain/model/SettingsTest.kt:19`

- [ ] **Step 1: Write the failing test**

In `SettingsTest.kt`, change line 19 from `assertThat(settings.language).isEqualTo("tr")` to:

```kotlin
assertThat(settings.language).isEqualTo("system")
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="com.kutluoglu.prayer_settings.domain.model.SettingsTest"`
Expected: FAIL — `expected: system but was: tr`

- [ ] **Step 3: Implement the change**

In `Settings.kt:6`, change:

```kotlin
val language: String = "tr",
```

to:

```kotlin
val language: String = "system",
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="com.kutluoglu.prayer_settings.domain.model.SettingsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer_settings/src/main/java/com/kutluoglu/prayer_settings/domain/model/Settings.kt prayer_settings/src/test/java/com/kutluoglu/prayer_settings/domain/model/SettingsTest.kt
git commit -m "feat(settings): default language is system (follow device)"
```

---

## Task 2: Default language → `"system"` (prayer_settings DataStore)

**Files:**
- Modify: `prayer_settings/src/main/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStore.kt:58,75`
- Test: `prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStoreTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `SettingsDataStoreTest.kt` (inside the class):

```kotlin
@Test
fun `default language is system`() = runBlocking {
    val settings = dataStore.getSettings()
    assertThat(settings.language).isEqualTo("system")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="com.kutluoglu.prayer_settings.data.local.SettingsDataStoreTest"`
Expected: FAIL — `expected: system but was: tr`

- [ ] **Step 3: Implement the change**

In `SettingsDataStore.kt`, change both occurrences of `language = preferences[PreferencesKeys.LANGUAGE] ?: "tr"` (lines 58 and 75) to:

```kotlin
language = preferences[PreferencesKeys.LANGUAGE] ?: "system",
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_settings:testDebugUnitTest --tests="com.kutluoglu.prayer_settings.data.local.SettingsDataStoreTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add prayer_settings/src/main/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStore.kt prayer_settings/src/test/java/com/kutluoglu/prayer_settings/data/local/SettingsDataStoreTest.kt
git commit -m "feat(settings): persist system as default language"
```

---

## Task 3: Default language → `"system"` (prayer_cache DataStore)

**Files:**
- Modify: `prayer_cache/src/main/java/com/kutluoglu/prayer_cache/SettingsDataStoreImp.kt:34`

- [ ] **Step 1: Implement the change**

In `SettingsDataStoreImp.kt`, change line 34:

```kotlin
private const val DEFAULT_LANGUAGE = "tr"
```

to:

```kotlin
private const val DEFAULT_LANGUAGE = "system"
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :prayer_cache:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_cache/src/main/java/com/kutluoglu/prayer_cache/SettingsDataStoreImp.kt
git commit -m "feat(cache): default language is system"
```

---

## Task 4: `LocaleManager` (app module)

**Files:**
- Create: `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt`
- Create: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.Locale

class LocaleManagerTest {

    private val manager = LocaleManager()

    @Test
    fun `default language is system`() {
        assertThat(manager.languageCode).isEqualTo("system")
    }

    @Test
    fun `setLanguage updates the holder synchronously`() {
        manager.setLanguage("ar")
        assertThat(manager.languageCode).isEqualTo("ar")
    }

    @Test
    fun `resolveLocale returns device locale for system`() {
        val deviceLocale = Locale("fr")
        assertThat(manager.resolveLocale(deviceLocale)).isEqualTo(deviceLocale)
    }

    @Test
    fun `resolveLocale returns explicit locale for override`() {
        manager.setLanguage("de")
        assertThat(manager.resolveLocale(Locale("fr")).language).isEqualTo("de")
    }

    @Test
    fun `applyLocale returns context unchanged for system`() {
        val context = mockk<Context>()
        assertThat(manager.applyLocale(context)).isSameInstanceAs(context)
    }

    @Test
    fun `applyLocale wraps context for explicit language`() {
        manager.setLanguage("ar")
        val context = mockk<Context>()
        val config = Configuration()
        every { context.resources } returns mockk()
        every { context.resources.configuration } returns config
        every { context.createConfigurationContext(any()) } returns mockk()
        val result = manager.applyLocale(context)
        assertThat(result).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests="com.kutluoglu.namazvakitleri.locale.LocaleManagerTest"`
Expected: FAIL — compilation error, `LocaleManager` unresolved

- [ ] **Step 3: Implement `LocaleManager`**

Create `app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import org.koin.core.annotation.Single

@Single
class LocaleManager {

    @Volatile
    var languageCode: String = SYSTEM_LANGUAGE
        private set

    fun setLanguage(code: String) {
        languageCode = code
    }

    fun resolveLocale(deviceLocale: Locale = Locale.getDefault()): Locale {
        return if (languageCode == SYSTEM_LANGUAGE) {
            deviceLocale
        } else {
            Locale.forLanguageTag(languageCode)
        }
    }

    fun applyLocale(context: Context): Context {
        if (languageCode == SYSTEM_LANGUAGE) return context
        val locale = resolveLocale()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests="com.kutluoglu.namazvakitleri.locale.LocaleManagerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/locale/LocaleManager.kt app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerTest.kt
git commit -m "feat(locale): add LocaleManager for runtime language override"
```

---

## Task 5: Register `LocaleManager` in Koin

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt:25-51` (only if KSP does not auto-discover)
- Create: `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt`

`LocaleManager` is annotated `@Single`, so it should be picked up by the KSP-generated `configurationModules` (the app uses `@KoinApplication` + `configurationModules + appModule`). Verify with a Koin test.

- [ ] **Step 1: Write the Koin test**

Create `app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt`:

```kotlin
package com.kutluoglu.namazvakitleri.locale

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class LocaleManagerKoinTest : KoinTest {

    private val localeManager: LocaleManager by inject()

    @Test
    fun `LocaleManager is resolvable from Koin`() {
        startKoin { modules(emptyList()) }
        assertThat(localeManager).isNotNull()
        stopKoin()
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests="com.kutluoglu.namazvakitleri.locale.LocaleManagerKoinTest"`
Expected: PASS (KSP discovers the `@Single`).

If it FAILS with "No definition found", add to `AppModule.kt` inside the `module { }` block:

```kotlin
single { LocaleManager() }
```

and add the import `import com.kutluoglu.namazvakitleri.locale.LocaleManager`.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/kutluoglu/namazvakitleri/locale/LocaleManagerKoinTest.kt app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt
git commit -m "chore(locale): verify LocaleManager DI wiring"
```

---

## Task 6: Wire locale into Application and MainActivity

**Files:**
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt`

- [ ] **Step 1: Implement Application changes**

Replace the body of `NamazVakitleriApplication.kt` with:

```kotlin
package com.kutluoglu.namazvakitleri

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.*

@KoinApplication
class NamazVakitleriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriApplication)
            modules(configurationModules + appModule)
        }
        setupActivityLifecycleCallbacks()
    }

    private fun setupActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val displayProvider: DisplayProvider = get()
                displayProvider.setCurrentActivity(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                val displayProvider: DisplayProvider = get()
                displayProvider.setCurrentActivity(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
```

Note: `attachBaseContext` is intentionally NOT overridden on the Application. The locale is applied per-activity in `MainActivity.attachBaseContext` (Step 2), which reads the persisted language **synchronously** so a persisted override is applied correctly on cold start (no async race).

- [ ] **Step 2: Implement MainActivity changes**

Replace the body of `MainActivity.kt` with:

```kotlin
package com.kutluoglu.namazvakitleri

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kutluoglu.core.designsystem.theme.NamazVakitleriTheme
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localeManager = get<LocaleManager>()
        val settingsDataStore = get<SettingsDataStore>()
        val language = runBlocking { settingsDataStore.getSettings().language }
        localeManager.setLanguage(language)
        super.attachBaseContext(localeManager.applyLocale(newBase))
    }

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

Note: `runBlocking` here is a one-time blocking read of a small preferences file during activity creation (DataStore caches in memory after the first read). It is wrapped defensively by the `getSettings()` implementation; if it throws, the app crashes on launch — so if you prefer resilience, wrap it in `runCatching { }` and fall back to `"system"`:

```kotlin
val language = runCatching { runBlocking { settingsDataStore.getSettings().language } }
    .getOrDefault(LocaleManager.SYSTEM_LANGUAGE)
localeManager.setLanguage(language)
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kutluoglu/namazvakitleri/NamazVakitleriApplication.kt app/src/main/java/com/kutluoglu/namazvakitleri/MainActivity.kt
git commit -m "feat(locale): apply locale in MainActivity attachBaseContext"
```

---

## Task 7: Settings UI — "Follow system" entry + ViewModel default

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionScreen.kt:49-65`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModel.kt:29`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

In `LanguageSelectionViewModelTest.kt`:
- Change line 69 `assertThat(loadedState.languages).hasSize(15)` to `assertThat(loadedState.languages).hasSize(16)`.
- Change line 70 `assertThat(loadedState.selectedLanguage).isEqualTo("tr")` to `assertThat(loadedState.selectedLanguage).isEqualTo("system")`.

Add two new tests:

```kotlin
@Test
fun `languages should contain system entry first`() {
    val state = viewModel.uiState.value
    val loadedState = state as LanguageUiState.LanguagesLoaded
    assertThat(loadedState.languages.first().code).isEqualTo("system")
}

@Test
fun `selecting system entry persists system`() = runTest {
    val system = languages.first { it.code == "system" }
    viewModel.onEvent(LanguageEvent.SelectLanguage(system))
    coVerify { updateLanguageUseCase("system") }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.language.LanguageSelectionViewModelTest"`
Expected: FAIL — `system` not in list, size 15, selected `tr`

- [ ] **Step 3: Implement the "system" entry**

In `LanguageSelectionScreen.kt`, replace the `languages` list (lines 49-65) with:

```kotlin
val languages = listOf(
    Language("system", "System default", "System default"),
    Language("tr", "Turkish", "Türkçe"),
    Language("en", "English", "English"),
    Language("ar", "Arabic", "العربية"),
    Language("ur", "Urdu", "اردو"),
    Language("id", "Indonesian", "Bahasa Indonesia"),
    Language("ms", "Malay", "Bahasa Melayu"),
    Language("fa", "Persian", "فارسی"),
    Language("bn", "Bengali", "বাংলা"),
    Language("hi", "Hindi", "हिन्दी"),
    Language("ta", "Tamil", "தமிழ்"),
    Language("th", "Thai", "ไทย"),
    Language("ru", "Russian", "Русский"),
    Language("fr", "French", "Français"),
    Language("de", "German", "Deutsch"),
    Language("es", "Spanish", "Español")
)
```

- [ ] **Step 4: Implement the ViewModel default**

In `LanguageSelectionViewModel.kt:29`, change:

```kotlin
private var currentLanguageCode: String = "tr"
```

to:

```kotlin
private var currentLanguageCode: String = "system"
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.language.LanguageSelectionViewModelTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionScreen.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModel.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/language/LanguageSelectionViewModelTest.kt
git commit -m "feat(settings): add Follow system language entry"
```

---

## Task 8: Thread `onLanguageSelected` to MainAppScreen (apply + recreate)

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt:66-73`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt`

- [ ] **Step 1: Implement SettingsGraph change**

In `SettingsGraph.kt`, change the `settingsGraph` signature to accept an `onLanguageSelected` callback and pass it through to the language composable:

```kotlin
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onLanguageSelected: (String) -> Unit
) {
    composable(Screen.SettingsScreen.route) {
        SettingsRoute(
            onNavigateToMyLocations = {
                navController.navigate(Screen.MyLocationsScreen.route)
            },
            onNavigateToCalculationMethod = {
                navController.navigate(Screen.CalculationMethodScreen.route)
            },
            onNavigateToHijriAdjustment = {
                navController.navigate(Screen.HijriAdjustmentScreen.route)
            },
            onNavigateToLanguage = {
                navController.navigate(Screen.LanguageSelectionScreen.route)
            }
        )
    }

    // ... MyLocations, LocationSelection, CalculationMethod, HijriAdjustment composables unchanged ...

    composable(Screen.LanguageSelectionScreen.route) {
        LanguageSelectionRoute(
            onNavigateBack = { navController.popBackStack() },
            onLanguageSelected = { language ->
                navController.popBackStack()
                onLanguageSelected(language)
            }
        )
    }
}
```

- [ ] **Step 2: Implement MainAppScreen change**

In `MainAppScreen.kt`, add imports:

```kotlin
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import org.koin.android.ext.android.get
```

Inside `MainAppScreen()`, after `val currentGraph = ...`, add:

```kotlin
val context = LocalContext.current
val activity = context.findActivity()

fun applyLanguage(language: String) {
    context.get<LocaleManager>().setLanguage(language)
    activity?.recreate()
}
```

Pass the callback to the settings graph:

```kotlin
navigation(
    route = PrayerNestedGraph.SETTINGS,
    startDestination = Screen.SettingsScreen.route
) {
    settingsGraph(navController, onLanguageSelected = ::applyLanguage)
}
```

Add the helper at the bottom of the file (top-level):

```kotlin
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt
git commit -m "feat(locale): apply language and recreate activity on selection"
```

---

## Task 9: Translations — `core/designsystem` (add ur, ms, fa, bn, hi, ta, th, ru, es)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-designsystem-translations.md`.

- Create: `core/designsystem/src/main/res/values-ur/strings.xml`
- Create: `core/designsystem/src/main/res/values-ms/strings.xml`
- Create: `core/designsystem/src/main/res/values-fa/strings.xml`
- Create: `core/designsystem/src/main/res/values-bn/strings.xml`
- Create: `core/designsystem/src/main/res/values-hi/strings.xml`
- Create: `core/designsystem/src/main/res/values-ta/strings.xml`
- Create: `core/designsystem/src/main/res/values-th/strings.xml`
- Create: `core/designsystem/src/main/res/values-ru/strings.xml`
- Create: `core/designsystem/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 9 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file must contain the full designsystem string set (all 45 keys + the `prayers` string-array).

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/designsystem/src/main/res/values-ur core/designsystem/src/main/res/values-ms core/designsystem/src/main/res/values-fa core/designsystem/src/main/res/values-bn core/designsystem/src/main/res/values-hi core/designsystem/src/main/res/values-ta core/designsystem/src/main/res/values-th core/designsystem/src/main/res/values-ru core/designsystem/src/main/res/values-es
git commit -m "feat(designsystem): add ur, ms, fa, bn, hi, ta, th, ru, es translations"
```

---

## Task 10: Translations — `prayer_feature/settings` (all 15 languages)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-settings-translations.md`.

- Create: `prayer_feature/settings/src/main/res/values-tr/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-ar/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-ur/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-id/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-ms/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-fa/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-bn/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-hi/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-ta/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-th/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-ru/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-fr/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-de/strings.xml`
- Create: `prayer_feature/settings/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 14 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file must contain all 45 settings string keys.

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/settings/src/main/res/values-tr prayer_feature/settings/src/main/res/values-ar prayer_feature/settings/src/main/res/values-ur prayer_feature/settings/src/main/res/values-id prayer_feature/settings/src/main/res/values-ms prayer_feature/settings/src/main/res/values-fa prayer_feature/settings/src/main/res/values-bn prayer_feature/settings/src/main/res/values-hi prayer_feature/settings/src/main/res/values-ta prayer_feature/settings/src/main/res/values-th prayer_feature/settings/src/main/res/values-ru prayer_feature/settings/src/main/res/values-fr prayer_feature/settings/src/main/res/values-de prayer_feature/settings/src/main/res/values-es
git commit -m "feat(settings): add translations for all 15 languages"
```

---

## Task 11: Translations — `prayer_feature/home` (add 13 languages)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-home-translations.md`. Surah names are NOT included (they fall back to the transliterated `values/`).

- Create: `prayer_feature/home/src/main/res/values-ar/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-ur/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-id/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-ms/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-fa/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-bn/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-hi/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-ta/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-th/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-ru/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-fr/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-de/strings.xml`
- Create: `prayer_feature/home/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 13 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file contains the 12 non-surah home strings.

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/home/src/main/res/values-ar prayer_feature/home/src/main/res/values-ur prayer_feature/home/src/main/res/values-id prayer_feature/home/src/main/res/values-ms prayer_feature/home/src/main/res/values-fa prayer_feature/home/src/main/res/values-bn prayer_feature/home/src/main/res/values-hi prayer_feature/home/src/main/res/values-ta prayer_feature/home/src/main/res/values-th prayer_feature/home/src/main/res/values-ru prayer_feature/home/src/main/res/values-fr prayer_feature/home/src/main/res/values-de prayer_feature/home/src/main/res/values-es
git commit -m "feat(home): add translations for 13 languages"
```

---

## Task 12: Translations — `prayer_feature/qibla` (add 13 languages)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-qibla-translations.md`.

- Create: `prayer_feature/qibla/src/main/res/values-ar/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-ur/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-id/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-ms/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-fa/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-bn/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-hi/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-ta/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-th/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-ru/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-fr/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-de/strings.xml`
- Create: `prayer_feature/qibla/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 13 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file contains the 12 qibla strings.

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/res/values-ar prayer_feature/qibla/src/main/res/values-ur prayer_feature/qibla/src/main/res/values-id prayer_feature/qibla/src/main/res/values-ms prayer_feature/qibla/src/main/res/values-fa prayer_feature/qibla/src/main/res/values-bn prayer_feature/qibla/src/main/res/values-hi prayer_feature/qibla/src/main/res/values-ta prayer_feature/qibla/src/main/res/values-th prayer_feature/qibla/src/main/res/values-ru prayer_feature/qibla/src/main/res/values-fr prayer_feature/qibla/src/main/res/values-de prayer_feature/qibla/src/main/res/values-es
git commit -m "feat(qibla): add translations for 13 languages"
```

---

## Task 13: Translations — `prayer_feature/prayertimes` (add 13 languages)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-prayertimes-translations.md`.

- Create: `prayer_feature/prayertimes/src/main/res/values-ar/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-ur/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-id/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-ms/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-fa/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-bn/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-hi/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-ta/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-th/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-ru/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-fr/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-de/strings.xml`
- Create: `prayer_feature/prayertimes/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 13 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file contains the 5 prayertimes strings.

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_feature:prayertimes:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/prayertimes/src/main/res/values-ar prayer_feature/prayertimes/src/main/res/values-ur prayer_feature/prayertimes/src/main/res/values-id prayer_feature/prayertimes/src/main/res/values-ms prayer_feature/prayertimes/src/main/res/values-fa prayer_feature/prayertimes/src/main/res/values-bn prayer_feature/prayertimes/src/main/res/values-hi prayer_feature/prayertimes/src/main/res/values-ta prayer_feature/prayertimes/src/main/res/values-th prayer_feature/prayertimes/src/main/res/values-ru prayer_feature/prayertimes/src/main/res/values-fr prayer_feature/prayertimes/src/main/res/values-de prayer_feature/prayertimes/src/main/res/values-es
git commit -m "feat(prayertimes): add translations for 13 languages"
```

---

## Task 14: Translations — `prayer_navigation/core` (add 13 languages)

**Files:** Create each file below. Content comes from `docs/superpowers/plans/translations/2026-08-19-navigation-translations.md`.

- Create: `prayer_navigation/core/src/main/res/values-ar/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-ur/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-id/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-ms/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-fa/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-bn/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-hi/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-ta/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-th/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-ru/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-fr/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-de/strings.xml`
- Create: `prayer_navigation/core/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the 13 files**

For each language, copy the corresponding `<resources>...</resources>` block from the appendix into the target file path. Each file contains the 4 navigation strings.

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :prayer_navigation:core:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_navigation/core/src/main/res/values-ar prayer_navigation/core/src/main/res/values-ur prayer_navigation/core/src/main/res/values-id prayer_navigation/core/src/main/res/values-ms prayer_navigation/core/src/main/res/values-fa prayer_navigation/core/src/main/res/values-bn prayer_navigation/core/src/main/res/values-hi prayer_navigation/core/src/main/res/values-ta prayer_navigation/core/src/main/res/values-th prayer_navigation/core/src/main/res/values-ru prayer_navigation/core/src/main/res/values-fr prayer_navigation/core/src/main/res/values-de prayer_navigation/core/src/main/res/values-es
git commit -m "feat(navigation): add translations for 13 languages"
```

---

## Task 15: Final verification

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew allTests`
Expected: BUILD SUCCESSFUL (all unit tests + suites pass)

- [ ] **Step 2: Run a debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run gitnexus change detection**

Run: `gitnexus_detect_changes()` (or `npx gitnexus analyze` if the index is stale) to confirm the changes only affect the expected symbols and flows.

- [ ] **Step 4: Commit any remaining changes**

```bash
git status
git add -A
git commit -m "chore: final verification for language-agnostic app"
```
