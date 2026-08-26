# Force & Optional Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add force-update (non-dismissible blocking dialog) and optional-update (dismissible, re-nag on next launch) features driven by Firebase Remote Config, working for both Play Store and sideloaded installs.

**Architecture:** New `:app_update` module following the project's Clean Architecture conventions. `domain` holds the model, repository interface, and `CheckForUpdateUseCase`; `data` holds the Remote Config source, repository impl, install-source detector, and version-code provider; `ui` holds the ViewModel, UI state, URL opener, and Compose dialogs. `MainAppScreen` hosts the ViewModel and dialogs so they overlay the whole app. Version comparison uses `versionCode` (int).

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3), Koin (KSP annotations), Firebase Remote Config (`firebase-config-ktx`), JUnit 5 + MockK + Turbine + Truth, Robolectric.

**Spec:** `docs/superpowers/specs/2026-08-25-force-optional-update-design.md`

---

## File Structure

**New module `:app_update`** (namespace `com.kutluoglu.app_update`):

| File | Responsibility |
|------|----------------|
| `app_update/build.gradle.kts` | Module build config |
| `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateInfo.kt` | Update metadata data class |
| `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateDecision.kt` | Sealed decision (NoUpdate/ForceUpdate/OptionalUpdate) |
| `app_update/src/main/java/com/kutluoglu/app_update/domain/repository/UpdateRepository.kt` | Repository interface |
| `app_update/src/main/java/com/kutluoglu/app_update/domain/usecase/CheckForUpdateUseCase.kt` | Version comparison logic |
| `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateConfigSource.kt` | Remote Config abstraction (interface) |
| `app_update/src/main/java/com/kutluoglu/app_update/data/FirebaseUpdateConfigSource.kt` | Firebase Remote Config impl |
| `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateInfoRemoteDataSource.kt` | Maps config → UpdateInfo |
| `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateRepositoryImpl.kt` | Repository impl |
| `app_update/src/main/java/com/kutluoglu/app_update/data/InstallSourceDetector.kt` | Play Store vs sideload detection + URLs |
| `app_update/src/main/java/com/kutluoglu/app_update/data/VersionCodeProvider.kt` | Reads installed versionCode |
| `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUiState.kt` | Sealed UI state |
| `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUrlOpener.kt` | Opens update URL, returns success |
| `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateViewModel.kt` | Orchestrates check + dialog actions |
| `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateDialogs.kt` | Force + optional AlertDialogs |
| `app_update/src/main/java/com/kutluoglu/app_update/di/AppUpdateModule.kt` | Koin module (wires use case) |
| `app_update/src/main/res/values*/strings.xml` | 15 locale string files |

**Modified files:**
- `gradle/libs.versions.toml` — add `firebase-config-ktx`
- `settings.gradle.kts` — add `include(":app_update")`
- `app/build.gradle.kts` — add `implementation(project(":app_update"))`
- `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt` — host ViewModel + dialogs, launch/resume check

**Test files** (in `app_update/src/test/java/com/kutluoglu/app_update/...`):
- `domain/model/UpdateModelTest.kt`
- `data/UpdateInfoRemoteDataSourceTest.kt`
- `data/UpdateRepositoryImplTest.kt`
- `domain/usecase/CheckForUpdateUseCaseTest.kt`
- `data/InstallSourceDetectorTest.kt`
- `data/VersionCodeProviderTest.kt`
- `ui/UpdateUrlOpenerTest.kt`
- `ui/UpdateViewModelTest.kt`
- `di/AppUpdateModuleTest.kt`

---

### Task 1: Create `:app_update` module skeleton

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `app_update/build.gradle.kts`

- [ ] **Step 1: Add the Remote Config dependency to the version catalog**

In `gradle/libs.versions.toml`, add a version and library. In the `[versions]` block add:

```toml
firebaseConfig = "21.6.2"
```

In the `[libraries]` block add (near the other firebase entries):

```toml
firebase-config-ktx = { module = "com.google.firebase:firebase-config-ktx", version.ref = "firebaseConfig" }
```

- [ ] **Step 2: Register the module in settings**

In `settings.gradle.kts`, add after the `include(":prayer_notifications")` line:

```kotlin
include(":app_update")
```

- [ ] **Step 3: Create the module build file**

Create `app_update/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.app_update"
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
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.firebase.config.ktx)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation("androidx.test:core:1.7.0")
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

- [ ] **Step 4: Verify the module compiles**

Run: `./gradlew :app_update:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts app_update/build.gradle.kts
git commit -m "feat(update): scaffold app_update module"
```

---

### Task 2: Domain model (`UpdateInfo`, `UpdateDecision`)

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateInfo.kt`
- Create: `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateDecision.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/domain/model/UpdateModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/domain/model/UpdateModelTest.kt`:

```kotlin
package com.kutluoglu.app_update.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UpdateModelTest {

    @Test
    fun `UpdateInfo holds update metadata`() {
        val info = UpdateInfo(
            latestVersionCode = 200,
            minVersionCode = 150,
            latestVersionName = "2.0",
            releaseNotes = "notes",
            directDownloadUrl = "https://example.com/app.apk",
        )
        assertThat(info.latestVersionCode).isEqualTo(200)
        assertThat(info.minVersionCode).isEqualTo(150)
        assertThat(info.latestVersionName).isEqualTo("2.0")
        assertThat(info.releaseNotes).isEqualTo("notes")
        assertThat(info.directDownloadUrl).isEqualTo("https://example.com/app.apk")
    }

    @Test
    fun `UpdateDecision ForceUpdate holds info`() {
        val info = UpdateInfo(200, 150, "2.0", "", "")
        val decision = UpdateDecision.ForceUpdate(info)
        assertThat(decision.info).isEqualTo(info)
    }

    @Test
    fun `UpdateDecision NoUpdate is a singleton`() {
        assertThat(UpdateDecision.NoUpdate).isSameInstanceAs(UpdateDecision.NoUpdate)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.domain.model.UpdateModelTest"`
Expected: FAIL — compilation error, `UpdateInfo` / `UpdateDecision` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateInfo.kt`:

```kotlin
package com.kutluoglu.app_update.domain.model

data class UpdateInfo(
    val latestVersionCode: Int,
    val minVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val directDownloadUrl: String,
)
```

Create `app_update/src/main/java/com/kutluoglu/app_update/domain/model/UpdateDecision.kt`:

```kotlin
package com.kutluoglu.app_update.domain.model

sealed interface UpdateDecision {
    data object NoUpdate : UpdateDecision
    data class ForceUpdate(val info: UpdateInfo) : UpdateDecision
    data class OptionalUpdate(val info: UpdateInfo) : UpdateDecision
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.domain.model.UpdateModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/domain/model app_update/src/test/java/com/kutluoglu/app_update/domain/model
git commit -m "feat(update): add UpdateInfo model and UpdateDecision"
```

---

### Task 3: `UpdateConfigSource` + `FirebaseUpdateConfigSource` + `UpdateInfoRemoteDataSource`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateConfigSource.kt`
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/FirebaseUpdateConfigSource.kt`
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateInfoRemoteDataSource.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/data/UpdateInfoRemoteDataSourceTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/data/UpdateInfoRemoteDataSourceTest.kt`:

```kotlin
package com.kutluoglu.app_update.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateInfoRemoteDataSourceTest {

    private val configSource = mockk<UpdateConfigSource>()
    private val dataSource = UpdateInfoRemoteDataSource(configSource)

    @Test
    fun `maps config values to UpdateInfo`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.latestVersionCode).isEqualTo(200)
        assertThat(info.minVersionCode).isEqualTo(100)
        assertThat(info.latestVersionName).isEqualTo("2.0")
        assertThat(info.releaseNotes).isEqualTo("New features")
        assertThat(info.directDownloadUrl).isEqualTo("https://example.com/app.apk")
    }

    @Test
    fun `returns null when latest version code is missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 0L
        every { configSource.getLong("update_min_version_code") } returns 100L

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }

    @Test
    fun `returns null when min version code is missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 0L

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }

    @Test
    fun `returns null when fetch fails`() = runTest {
        coEvery { configSource.fetchAndActivate() } throws RuntimeException("network error")

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.UpdateInfoRemoteDataSourceTest"`
Expected: FAIL — compilation error, `UpdateConfigSource` / `UpdateInfoRemoteDataSource` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateConfigSource.kt`:

```kotlin
package com.kutluoglu.app_update.data

interface UpdateConfigSource {
    suspend fun fetchAndActivate(): Boolean
    fun getLong(key: String): Long
    fun getString(key: String): String
}
```

Create `app_update/src/main/java/com/kutluoglu/app_update/data/FirebaseUpdateConfigSource.kt`:

```kotlin
package com.kutluoglu.app_update.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.activate
import com.google.firebase.remoteconfig.ktx.fetch
import org.koin.core.annotation.Single

@Single
class FirebaseUpdateConfigSource(
    private val remoteConfig: FirebaseRemoteConfig,
) : UpdateConfigSource {

    override suspend fun fetchAndActivate(): Boolean {
        remoteConfig.fetch(0)
        return remoteConfig.activate()
    }

    override fun getLong(key: String): Long = remoteConfig.getLong(key)

    override fun getString(key: String): String = remoteConfig.getString(key)
}
```

Create `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateInfoRemoteDataSource.kt`:

```kotlin
package com.kutluoglu.app_update.data

import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.koin.core.annotation.Single

@Single
class UpdateInfoRemoteDataSource(
    private val configSource: UpdateConfigSource,
) {

    suspend fun fetchUpdateInfo(): UpdateInfo? {
        return runCatching {
            configSource.fetchAndActivate()
            val latest = configSource.getLong(KEY_LATEST_VERSION_CODE)
            val min = configSource.getLong(KEY_MIN_VERSION_CODE)
            if (latest <= 0 || min <= 0) return null
            UpdateInfo(
                latestVersionCode = latest.toInt(),
                minVersionCode = min.toInt(),
                latestVersionName = configSource.getString(KEY_LATEST_VERSION_NAME),
                releaseNotes = configSource.getString(KEY_RELEASE_NOTES),
                directDownloadUrl = configSource.getString(KEY_DIRECT_DOWNLOAD_URL),
            )
        }.getOrNull()
    }

    private companion object {
        const val KEY_LATEST_VERSION_CODE = "update_latest_version_code"
        const val KEY_MIN_VERSION_CODE = "update_min_version_code"
        const val KEY_LATEST_VERSION_NAME = "update_latest_version_name"
        const val KEY_RELEASE_NOTES = "update_release_notes"
        const val KEY_DIRECT_DOWNLOAD_URL = "update_direct_download_url"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.UpdateInfoRemoteDataSourceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/data app_update/src/test/java/com/kutluoglu/app_update/data
git commit -m "feat(update): add Remote Config data source"
```

---

### Task 4: `UpdateRepository` interface + `UpdateRepositoryImpl`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/domain/repository/UpdateRepository.kt`
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateRepositoryImpl.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/data/UpdateRepositoryImplTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/data/UpdateRepositoryImplTest.kt`:

```kotlin
package com.kutluoglu.app_update.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateRepositoryImplTest {

    private val remoteDataSource = mockk<UpdateInfoRemoteDataSource>()
    private val repository = UpdateRepositoryImpl(remoteDataSource)

    @Test
    fun `getUpdateInfo delegates to remote data source`() = runTest {
        val info = UpdateInfo(200, 150, "2.0", "notes", "https://example.com/app.apk")
        coEvery { remoteDataSource.fetchUpdateInfo() } returns info

        assertThat(repository.getUpdateInfo()).isEqualTo(info)
        coVerify { remoteDataSource.fetchUpdateInfo() }
    }

    @Test
    fun `getUpdateInfo returns null when remote source returns null`() = runTest {
        coEvery { remoteDataSource.fetchUpdateInfo() } returns null

        assertThat(repository.getUpdateInfo()).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.UpdateRepositoryImplTest"`
Expected: FAIL — compilation error, `UpdateRepository` / `UpdateRepositoryImpl` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/domain/repository/UpdateRepository.kt`:

```kotlin
package com.kutluoglu.app_update.domain.repository

import com.kutluoglu.app_update.domain.model.UpdateInfo

interface UpdateRepository {
    suspend fun getUpdateInfo(): UpdateInfo?
}
```

Create `app_update/src/main/java/com/kutluoglu/app_update/data/UpdateRepositoryImpl.kt`:

```kotlin
package com.kutluoglu.app_update.data

import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import org.koin.core.annotation.Single

@Single
class UpdateRepositoryImpl(
    private val remoteDataSource: UpdateInfoRemoteDataSource,
) : UpdateRepository {

    override suspend fun getUpdateInfo(): UpdateInfo? = remoteDataSource.fetchUpdateInfo()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.UpdateRepositoryImplTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/domain/repository app_update/src/main/java/com/kutluoglu/app_update/data app_update/src/test/java/com/kutluoglu/app_update/data
git commit -m "feat(update): add UpdateRepository and impl"
```

---

### Task 5: `CheckForUpdateUseCase`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/domain/usecase/CheckForUpdateUseCase.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/domain/usecase/CheckForUpdateUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/domain/usecase/CheckForUpdateUseCaseTest.kt`:

```kotlin
package com.kutluoglu.app_update.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CheckForUpdateUseCaseTest {

    private val repository = mockk<UpdateRepository>()

    private fun info(
        latest: Int = 200,
        min: Int = 150,
    ) = UpdateInfo(
        latestVersionCode = latest,
        minVersionCode = min,
        latestVersionName = "2.0",
        releaseNotes = "notes",
        directDownloadUrl = "https://example.com/app.apk",
    )

    @Test
    fun `returns ForceUpdate when installed below minimum`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 100)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.ForceUpdate(info()))
    }

    @Test
    fun `returns OptionalUpdate when installed at minimum but below latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 150)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.OptionalUpdate(info()))
    }

    @Test
    fun `returns OptionalUpdate when installed between min and latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.OptionalUpdate(info()))
    }

    @Test
    fun `returns NoUpdate when installed at latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 200)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `returns NoUpdate when installed above latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 300)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `returns NoUpdate when repository returns null`() = runTest {
        coEvery { repository.getUpdateInfo() } returns null
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 100)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCaseTest"`
Expected: FAIL — compilation error, `CheckForUpdateUseCase` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/domain/usecase/CheckForUpdateUseCase.kt`:

```kotlin
package com.kutluoglu.app_update.domain.usecase

import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.repository.UpdateRepository

class CheckForUpdateUseCase(
    private val repository: UpdateRepository,
    private val currentVersionCode: Int,
) {

    suspend operator fun invoke(): UpdateDecision {
        val info = repository.getUpdateInfo() ?: return UpdateDecision.NoUpdate
        return when {
            currentVersionCode < info.minVersionCode -> UpdateDecision.ForceUpdate(info)
            currentVersionCode < info.latestVersionCode -> UpdateDecision.OptionalUpdate(info)
            else -> UpdateDecision.NoUpdate
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCaseTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/domain/usecase app_update/src/test/java/com/kutluoglu/app_update/domain/usecase
git commit -m "feat(update): add CheckForUpdateUseCase"
```

---

### Task 6: `InstallSourceDetector`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/InstallSourceDetector.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/data/InstallSourceDetectorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/data/InstallSourceDetectorTest.kt`:

```kotlin
package com.kutluoglu.app_update.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InstallSourceDetectorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `isPlayStoreInstall returns true for vending installer`() {
        shadowOf(context.packageManager)
            .setInstallerPackageName(context.packageName, "com.android.vending")

        val detector = InstallSourceDetector(context)

        assertThat(detector.isPlayStoreInstall()).isTrue()
    }

    @Test
    fun `isPlayStoreInstall returns false for null installer`() {
        shadowOf(context.packageManager)
            .setInstallerPackageName(context.packageName, null)

        val detector = InstallSourceDetector(context)

        assertThat(detector.isPlayStoreInstall()).isFalse()
    }

    @Test
    fun `getPlayStoreUrl returns market url for package`() {
        val detector = InstallSourceDetector(context)

        assertThat(detector.getPlayStoreUrl())
            .isEqualTo("market://details?id=${context.packageName}")
    }

    @Test
    fun `getPlayStoreWebUrl returns play web url for package`() {
        val detector = InstallSourceDetector(context)

        assertThat(detector.getPlayStoreWebUrl())
            .isEqualTo("https://play.google.com/store/apps/details?id=${context.packageName}")
    }

    @Test
    fun `getDirectDownloadUrl returns info url`() {
        val detector = InstallSourceDetector(context)
        val info = UpdateInfo(200, 150, "2.0", "", "https://example.com/app.apk")

        assertThat(detector.getDirectDownloadUrl(info)).isEqualTo("https://example.com/app.apk")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.InstallSourceDetectorTest"`
Expected: FAIL — compilation error, `InstallSourceDetector` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/data/InstallSourceDetector.kt`:

```kotlin
package com.kutluoglu.app_update.data

import android.content.Context
import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.koin.core.annotation.Factory

@Factory
class InstallSourceDetector(
    private val context: Context,
) {

    fun isPlayStoreInstall(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == PLAY_STORE_INSTALLER
    }

    fun getPlayStoreUrl(): String = "market://details?id=${context.packageName}"

    fun getPlayStoreWebUrl(): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun getDirectDownloadUrl(info: UpdateInfo): String = info.directDownloadUrl

    private companion object {
        const val PLAY_STORE_INSTALLER = "com.android.vending"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.InstallSourceDetectorTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/data app_update/src/test/java/com/kutluoglu/app_update/data
git commit -m "feat(update): add InstallSourceDetector"
```

---

### Task 7: `VersionCodeProvider`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/data/VersionCodeProvider.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/data/VersionCodeProviderTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/data/VersionCodeProviderTest.kt`:

```kotlin
package com.kutluoglu.app_update.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VersionCodeProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `getCurrentVersionCode returns a non-negative version code`() {
        val provider = VersionCodeProvider(context)

        assertThat(provider.getCurrentVersionCode()).isAtLeast(0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.VersionCodeProviderTest"`
Expected: FAIL — compilation error, `VersionCodeProvider` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/data/VersionCodeProvider.kt`:

```kotlin
package com.kutluoglu.app_update.data

import android.content.Context
import org.koin.core.annotation.Factory

@Factory
class VersionCodeProvider(
    private val context: Context,
) {

    @Suppress("DEPRECATION")
    fun getCurrentVersionCode(): Int {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionCode
        }.getOrDefault(0)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.data.VersionCodeProviderTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/data app_update/src/test/java/com/kutluoglu/app_update/data
git commit -m "feat(update): add VersionCodeProvider"
```

---

### Task 8: `UpdateUrlOpener`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUrlOpener.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/ui/UpdateUrlOpenerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/ui/UpdateUrlOpenerTest.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateUrlOpenerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `open returns false when no activity can handle the url`() {
        val opener = UpdateUrlOpener(context)

        val result = opener.open("market://details?id=com.kutluoglu.namazvakitleri")

        assertThat(result).isFalse()
    }

    @Test
    fun `open returns true when an activity can handle the url`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.browser"
                name = "com.example.browser.BrowserActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        val opener = UpdateUrlOpener(context)

        val result = opener.open("https://example.com")

        assertThat(result).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.ui.UpdateUrlOpenerTest"`
Expected: FAIL — compilation error, `UpdateUrlOpener` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUrlOpener.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.annotation.Factory

@Factory
class UpdateUrlOpener(
    private val context: Context,
) {

    fun open(url: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = context.packageManager.resolveActivity(intent, 0)
            if (resolved != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.ui.UpdateUrlOpenerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/ui app_update/src/test/java/com/kutluoglu/app_update/ui
git commit -m "feat(update): add UpdateUrlOpener"
```

---

### Task 9: `UpdateUiState` + `UpdateViewModel`

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUiState.kt`
- Create: `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateViewModel.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/ui/UpdateViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/ui/UpdateViewModelTest.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.data.InstallSourceDetector
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val checkForUpdateUseCase = mockk<CheckForUpdateUseCase>()
    private val installSourceDetector = mockk<InstallSourceDetector>()
    private val updateUrlOpener = mockk<UpdateUrlOpener>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun info() = UpdateInfo(
        latestVersionCode = 200,
        minVersionCode = 150,
        latestVersionName = "2.0",
        releaseNotes = "notes",
        directDownloadUrl = "https://example.com/app.apk",
    )

    private fun viewModel() = UpdateViewModel(
        checkForUpdateUseCase,
        installSourceDetector,
        updateUrlOpener,
    )

    @Test
    fun `checkForUpdate emits ForceUpdate when decision is force`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.ForceUpdate(info()))
    }

    @Test
    fun `checkForUpdate emits OptionalUpdate when decision is optional`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.OptionalUpdate(info()))
    }

    @Test
    fun `checkForUpdate emits NoUpdate when decision is none`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.NoUpdate
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.NoUpdate)
    }

    @Test
    fun `onOptionalUpdateDismissed resets to NoUpdate`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        val vm = viewModel()
        vm.checkForUpdate()
        assertThat(vm.uiState.value).isInstanceOf(UpdateUiState.OptionalUpdate::class.java)

        vm.onOptionalUpdateDismissed()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.NoUpdate)
    }

    @Test
    fun `onUpdateClicked opens play store url for play install`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns true
        every { installSourceDetector.getPlayStoreUrl() } returns "market://details?id=com.kutluoglu.namazvakitleri"
        every { updateUrlOpener.open(any()) } returns true
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("market://details?id=com.kutluoglu.namazvakitleri") }
    }

    @Test
    fun `onUpdateClicked falls back to web url when market url fails`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns true
        every { installSourceDetector.getPlayStoreUrl() } returns "market://details?id=com.kutluoglu.namazvakitleri"
        every { installSourceDetector.getPlayStoreWebUrl() } returns "https://play.google.com/store/apps/details?id=com.kutluoglu.namazvakitleri"
        every { updateUrlOpener.open(any()) } returns false
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("market://details?id=com.kutluoglu.namazvakitleri") }
        verify { updateUrlOpener.open("https://play.google.com/store/apps/details?id=com.kutluoglu.namazvakitleri") }
    }

    @Test
    fun `onUpdateClicked opens direct url for sideload install`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns false
        every { installSourceDetector.getDirectDownloadUrl(any()) } returns "https://example.com/app.apk"
        every { updateUrlOpener.open(any()) } returns true
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("https://example.com/app.apk") }
    }

    @Test
    fun `onUpdateClicked sets urlOpenFailed when all opens fail`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns false
        every { installSourceDetector.getDirectDownloadUrl(any()) } returns "https://example.com/app.apk"
        every { updateUrlOpener.open(any()) } returns false
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        val state = vm.uiState.value as UpdateUiState.ForceUpdate
        assertThat(state.urlOpenFailed).isTrue()
    }

    @Test
    fun `concurrent checkForUpdate calls are deduped`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { checkForUpdateUseCase() } coAnswers {
            delay(1000)
            UpdateDecision.NoUpdate
        }
        val vm = viewModel()

        vm.checkForUpdate()
        vm.checkForUpdate()

        advanceUntilIdle()

        verify(exactly = 1) { checkForUpdateUseCase() }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.ui.UpdateViewModelTest"`
Expected: FAIL — compilation error, `UpdateUiState` / `UpdateViewModel` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateUiState.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import com.kutluoglu.app_update.domain.model.UpdateInfo

sealed interface UpdateUiState {
    data object NoUpdate : UpdateUiState
    data class OptionalUpdate(val info: UpdateInfo) : UpdateUiState
    data class ForceUpdate(
        val info: UpdateInfo,
        val urlOpenFailed: Boolean = false,
    ) : UpdateUiState
}
```

Create `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateViewModel.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.app_update.data.InstallSourceDetector
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val installSourceDetector: InstallSourceDetector,
    private val updateUrlOpener: UpdateUrlOpener,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.NoUpdate)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var checkInFlight = false

    fun checkForUpdate() {
        if (checkInFlight) return
        checkInFlight = true
        viewModelScope.launch {
            val decision = checkForUpdateUseCase()
            _uiState.value = when (decision) {
                is UpdateDecision.ForceUpdate -> UpdateUiState.ForceUpdate(decision.info)
                is UpdateDecision.OptionalUpdate -> UpdateUiState.OptionalUpdate(decision.info)
                UpdateDecision.NoUpdate -> UpdateUiState.NoUpdate
            }
            checkInFlight = false
        }
    }

    fun onOptionalUpdateDismissed() {
        _uiState.value = UpdateUiState.NoUpdate
    }

    fun onUpdateClicked() {
        val info = (_uiState.value as? UpdateUiState.ForceUpdate)?.info
            ?: (_uiState.value as? UpdateUiState.OptionalUpdate)?.info
            ?: return
        val url = if (installSourceDetector.isPlayStoreInstall()) {
            installSourceDetector.getPlayStoreUrl()
        } else {
            installSourceDetector.getDirectDownloadUrl(info)
        }
        var opened = updateUrlOpener.open(url)
        if (!opened && installSourceDetector.isPlayStoreInstall()) {
            opened = updateUrlOpener.open(installSourceDetector.getPlayStoreWebUrl())
        }
        if (!opened) {
            val current = _uiState.value
            if (current is UpdateUiState.ForceUpdate) {
                _uiState.value = current.copy(urlOpenFailed = true)
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.ui.UpdateViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/ui app_update/src/test/java/com/kutluoglu/app_update/ui
git commit -m "feat(update): add UpdateViewModel and UI state"
```

---

### Task 10: `AppUpdateModule` (Koin) + wiring test

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/di/AppUpdateModule.kt`
- Test: `app_update/src/test/java/com/kutluoglu/app_update/di/AppUpdateModuleTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app_update/src/test/java/com/kutluoglu/app_update/di/AppUpdateModuleTest.kt`:

```kotlin
package com.kutluoglu.app_update.di

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.data.VersionCodeProvider
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AppUpdateModuleTest {

    @Test
    fun `provideCheckForUpdateUseCase wires current version code`() = runTest {
        val repository = mockk<UpdateRepository>()
        val versionCodeProvider = mockk<VersionCodeProvider>()
        every { versionCodeProvider.getCurrentVersionCode() } returns 100

        val useCase = AppUpdateModule.provideCheckForUpdateUseCase(
            repository,
            versionCodeProvider,
        )

        coEvery { repository.getUpdateInfo() } returns UpdateInfo(
            latestVersionCode = 200,
            minVersionCode = 150,
            latestVersionName = "2.0",
            releaseNotes = "",
            directDownloadUrl = "",
        )

        val decision = useCase()

        assertThat(decision).isInstanceOf(UpdateDecision.ForceUpdate::class.java)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.di.AppUpdateModuleTest"`
Expected: FAIL — compilation error, `AppUpdateModule` unresolved

- [ ] **Step 3: Write minimal implementation**

Create `app_update/src/main/java/com/kutluoglu/app_update/di/AppUpdateModule.kt`:

```kotlin
package com.kutluoglu.app_update.di

import com.kutluoglu.app_update.data.VersionCodeProvider
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provides

@Module
@Configuration
@ComponentScan("com.kutluoglu.app_update**")
object AppUpdateModule {

    @Provides
    fun provideCheckForUpdateUseCase(
        repository: UpdateRepository,
        versionCodeProvider: VersionCodeProvider,
    ): CheckForUpdateUseCase = CheckForUpdateUseCase(
        repository = repository,
        currentVersionCode = versionCodeProvider.getCurrentVersionCode(),
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app_update:testDebugUnitTest --tests="com.kutluoglu.app_update.di.AppUpdateModuleTest"`
Expected: PASS

- [ ] **Step 5: Verify the module compiles with KSP**

Run: `./gradlew :app_update:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/di app_update/src/test/java/com/kutluoglu/app_update/di
git commit -m "feat(update): add Koin AppUpdateModule"
```

---

### Task 11: `UpdateDialogs` + strings (15 locales)

**Files:**
- Create: `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateDialogs.kt`
- Create: `app_update/src/main/res/values/strings.xml`
- Create: `app_update/src/main/res/values-ar/strings.xml`
- Create: `app_update/src/main/res/values-bn/strings.xml`
- Create: `app_update/src/main/res/values-de/strings.xml`
- Create: `app_update/src/main/res/values-es/strings.xml`
- Create: `app_update/src/main/res/values-fa/strings.xml`
- Create: `app_update/src/main/res/values-fr/strings.xml`
- Create: `app_update/src/main/res/values-hi/strings.xml`
- Create: `app_update/src/main/res/values-id/strings.xml`
- Create: `app_update/src/main/res/values-ms/strings.xml`
- Create: `app_update/src/main/res/values-ru/strings.xml`
- Create: `app_update/src/main/res/values-ta/strings.xml`
- Create: `app_update/src/main/res/values-th/strings.xml`
- Create: `app_update/src/main/res/values-tr/strings.xml`
- Create: `app_update/src/main/res/values-ur/strings.xml`

- [ ] **Step 1: Write the dialog composables**

Create `app_update/src/main/java/com/kutluoglu/app_update/ui/UpdateDialogs.kt`:

```kotlin
package com.kutluoglu.app_update.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.app_update.R
import com.kutluoglu.app_update.domain.model.UpdateInfo

@Composable
fun ForceUpdateDialog(
    info: UpdateInfo,
    urlOpenFailed: Boolean,
    onUpdateClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.update_required_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_required_message))
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(info.releaseNotes)
                }
                if (urlOpenFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_open_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(stringResource(R.string.update_now))
            }
        },
    )
}

@Composable
fun OptionalUpdateDialog(
    info: UpdateInfo,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLaterClick,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_available_message))
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(info.releaseNotes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onLaterClick) {
                Text(stringResource(R.string.later))
            }
        },
    )
}
```

- [ ] **Step 2: Add the default (English) strings**

Create `app_update/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Update Required</string>
    <string name="update_required_message">A new version is required to continue.</string>
    <string name="update_available_title">Update Available</string>
    <string name="update_available_message">A new version is available.</string>
    <string name="update_now">Update</string>
    <string name="later">Later</string>
    <string name="update_open_failed">Couldn\'t open the update page. Please try again.</string>
</resources>
```

- [ ] **Step 3: Add the Turkish strings**

Create `app_update/src/main/res/values-tr/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Güncelleme Gerekli</string>
    <string name="update_required_message">Devam etmek için yeni bir sürüm gerekiyor.</string>
    <string name="update_available_title">Güncelleme Mevcut</string>
    <string name="update_available_message">Yeni bir sürüm mevcut.</string>
    <string name="update_now">Güncelle</string>
    <string name="later">Sonra</string>
    <string name="update_open_failed">Güncelleme sayfası açılamadı. Lütfen tekrar deneyin.</string>
</resources>
```

- [ ] **Step 4: Add the remaining 13 locale files**

Create each file below with the exact content shown. The 7 string names are identical in every file; only the values differ.

`app_update/src/main/res/values-ar/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">التحديث مطلوب</string>
    <string name="update_required_message">مطلوب إصدار جديد للمتابعة.</string>
    <string name="update_available_title">يتوفر تحديث</string>
    <string name="update_available_message">يتوفر إصدار جديد.</string>
    <string name="update_now">تحديث</string>
    <string name="later">لاحقًا</string>
    <string name="update_open_failed">تعذر فتح صفحة التحديث. يرجى المحاولة مرة أخرى.</string>
</resources>
```

`app_update/src/main/res/values-bn/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">আপডেট প্রয়োজন</string>
    <string name="update_required_message">চালিয়ে যেতে একটি নতুন সংস্করণ প্রয়োজন।</string>
    <string name="update_available_title">আপডেট উপলব্ধ</string>
    <string name="update_available_message">একটি নতুন সংস্করণ উপলব্ধ।</string>
    <string name="update_now">আপডেট</string>
    <string name="later">পরে</string>
    <string name="update_open_failed">আপডেট পৃষ্ঠা খোলা যায়নি। আবার চেষ্টা করুন।</string>
</resources>
```

`app_update/src/main/res/values-de/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Update erforderlich</string>
    <string name="update_required_message">Zum Fortfahren ist eine neue Version erforderlich.</string>
    <string name="update_available_title">Update verfügbar</string>
    <string name="update_available_message">Eine neue Version ist verfügbar.</string>
    <string name="update_now">Aktualisieren</string>
    <string name="later">Später</string>
    <string name="update_open_failed">Die Update-Seite konnte nicht geöffnet werden. Bitte versuchen Sie es erneut.</string>
</resources>
```

`app_update/src/main/res/values-es/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Actualización requerida</string>
    <string name="update_required_message">Se requiere una nueva versión para continuar.</string>
    <string name="update_available_title">Actualización disponible</string>
    <string name="update_available_message">Hay una nueva versión disponible.</string>
    <string name="update_now">Actualizar</string>
    <string name="later">Más tarde</string>
    <string name="update_open_failed">No se pudo abrir la página de actualización. Inténtelo de nuevo.</string>
</resources>
```

`app_update/src/main/res/values-fa/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">به‌روزرسانی الزامی است</string>
    <string name="update_required_message">برای ادامه، نسخه جدیدی لازم است.</string>
    <string name="update_available_title">به‌روزرسانی موجود است</string>
    <string name="update_available_message">نسخه جدید موجود است.</string>
    <string name="update_now">به‌روزرسانی</string>
    <string name="later">بعداً</string>
    <string name="update_open_failed">صفحه به‌روزرسانی باز نشد. لطفاً دوباره تلاش کنید.</string>
</resources>
```

`app_update/src/main/res/values-fr/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Mise à jour requise</string>
    <string name="update_required_message">Une nouvelle version est requise pour continuer.</string>
    <string name="update_available_title">Mise à jour disponible</string>
    <string name="update_available_message">Une nouvelle version est disponible.</string>
    <string name="update_now">Mettre à jour</string>
    <string name="later">Plus tard</string>
    <string name="update_open_failed">Impossible d\'ouvrir la page de mise à jour. Veuillez réessayer.</string>
</resources>
```

`app_update/src/main/res/values-hi/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">अपडेट आवश्यक है</string>
    <string name="update_required_message">जारी रखने के लिए एक नया संस्करण आवश्यक है।</string>
    <string name="update_available_title">अपडेट उपलब्ध है</string>
    <string name="update_available_message">एक नया संस्करण उपलब्ध है।</string>
    <string name="update_now">अपडेट करें</string>
    <string name="later">बाद में</string>
    <string name="update_open_failed">अपडेट पेज नहीं खोला जा सका। कृपया पुनः प्रयास करें।</string>
</resources>
```

`app_update/src/main/res/values-id/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Pembaruan Diperlukan</string>
    <string name="update_required_message">Versi baru diperlukan untuk melanjutkan.</string>
    <string name="update_available_title">Pembaruan Tersedia</string>
    <string name="update_available_message">Versi baru tersedia.</string>
    <string name="update_now">Perbarui</string>
    <string name="later">Nanti</string>
    <string name="update_open_failed">Tidak dapat membuka halaman pembaruan. Silakan coba lagi.</string>
</resources>
```

`app_update/src/main/res/values-ms/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Kemas Kini Diperlukan</string>
    <string name="update_required_message">Versi baharu diperlukan untuk meneruskan.</string>
    <string name="update_available_title">Kemas Kini Tersedia</string>
    <string name="update_available_message">Versi baharu tersedia.</string>
    <string name="update_now">Kemas Kini</string>
    <string name="later">Nanti</string>
    <string name="update_open_failed">Tidak dapat membuka halaman kemas kini. Sila cuba lagi.</string>
</resources>
```

`app_update/src/main/res/values-ru/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">Требуется обновление</string>
    <string name="update_required_message">Для продолжения требуется новая версия.</string>
    <string name="update_available_title">Доступно обновление</string>
    <string name="update_available_message">Доступна новая версия.</string>
    <string name="update_now">Обновить</string>
    <string name="later">Позже</string>
    <string name="update_open_failed">Не удалось открыть страницу обновления. Пожалуйста, попробуйте ещё раз.</string>
</resources>
```

`app_update/src/main/res/values-ta/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">புதுப்பிப்பு தேவை</string>
    <string name="update_required_message">தொடர ஒரு புதிய பதிப்பு தேவை.</string>
    <string name="update_available_title">புதுப்பிப்பு கிடைக்கிறது</string>
    <string name="update_available_message">புதிய பதிப்பு கிடைக்கிறது.</string>
    <string name="update_now">புதுப்பி</string>
    <string name="later">பின்னர்</string>
    <string name="update_open_failed">புதுப்பிப்பு பக்கத்தைத் திறக்க முடியவில்லை. மீண்டும் முயற்சிக்கவும்.</string>
</resources>
```

`app_update/src/main/res/values-th/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">ต้องอัปเดต</string>
    <string name="update_required_message">ต้องใช้เวอร์ชันใหม่เพื่อดำเนินการต่อ</string>
    <string name="update_available_title">มีอัปเดต</string>
    <string name="update_available_message">มีเวอร์ชันใหม่ให้ใช้งาน</string>
    <string name="update_now">อัปเดต</string>
    <string name="later">ทีหลัง</string>
    <string name="update_open_failed">ไม่สามารถเปิดหน้าอัปเดตได้ โปรดลองอีกครั้ง</string>
</resources>
```

`app_update/src/main/res/values-ur/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="update_required_title">اپ ڈیٹ درکار ہے</string>
    <string name="update_required_message">جاری رکھنے کے لیے نیا ورژن درکار ہے۔</string>
    <string name="update_available_title">اپ ڈیٹ دستیاب ہے</string>
    <string name="update_available_message">نیا ورژن دستیاب ہے۔</string>
    <string name="update_now">اپ ڈیٹ کریں</string>
    <string name="later">بعد میں</string>
    <string name="update_open_failed">اپ ڈیٹ صفحہ نہیں کھل سکا۔ براہ کرم دوبارہ کوشش کریں۔</string>
</resources>
```

- [ ] **Step 5: Verify the module compiles**

Run: `./gradlew :app_update:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app_update/src/main/java/com/kutluoglu/app_update/ui app_update/src/main/res
git commit -m "feat(update): add update dialogs and localized strings"
```

---

### Task 12: Wire into `MainAppScreen` + app build

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt`

- [ ] **Step 1: Add the module dependency to the app**

In `app/build.gradle.kts`, add to the `dependencies` block (near the other project deps):

```kotlin
    implementation(project(":app_update"))
```

- [ ] **Step 2: Host the ViewModel and dialogs in MainAppScreen**

Modify `app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt`:

Add these imports:

```kotlin
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kutluoglu.app_update.ui.ForceUpdateDialog
import com.kutluoglu.app_update.ui.OptionalUpdateDialog
import com.kutluoglu.app_update.ui.UpdateUiState
import com.kutluoglu.app_update.ui.UpdateViewModel
import org.koin.androidx.compose.koinViewModel
```

Note: `androidx.compose.runtime.getValue` is already imported in `MainAppScreen.kt` — do not add it again.

Inside `MainAppScreen()`, after the `val activity = context.findActivity()` line, add:

```kotlin
    val updateViewModel: UpdateViewModel = koinViewModel()
    val updateState by updateViewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateViewModel.checkForUpdate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
```

After the `Scaffold(...) { ... }` block closes (at the end of `MainAppScreen`), add:

```kotlin
    when (val state = updateState) {
        is UpdateUiState.ForceUpdate -> ForceUpdateDialog(
            info = state.info,
            urlOpenFailed = state.urlOpenFailed,
            onUpdateClick = updateViewModel::onUpdateClicked,
        )
        is UpdateUiState.OptionalUpdate -> OptionalUpdateDialog(
            info = state.info,
            onUpdateClick = updateViewModel::onUpdateClicked,
            onLaterClick = updateViewModel::onOptionalUpdateDismissed,
        )
        UpdateUiState.NoUpdate -> Unit
    }
```

- [ ] **Step 3: Verify the app compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/kutluoglu/namazvakitleri/MainAppScreen.kt
git commit -m "feat(update): wire update check and dialogs into app"
```

---

### Task 13: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run all module tests**

Run: `./gradlew :app_update:testDebugUnitTest`
Expected: all tests PASS (UpdateModelTest, UpdateInfoRemoteDataSourceTest, UpdateRepositoryImplTest, CheckForUpdateUseCaseTest, InstallSourceDetectorTest, VersionCodeProviderTest, UpdateUrlOpenerTest, UpdateViewModelTest, AppUpdateModuleTest)

- [ ] **Step 2: Run the full test suite**

Run: `./gradlew allTests`
Expected: `BUILD SUCCESSFUL` (all modules green)

- [ ] **Step 3: Verify the release build compiles**

Run: `./gradlew :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run GitNexus change detection**

Run: `gitnexus_detect_changes` (repo: `NamazVakitleri`)
Expected: only new `app_update` symbols + `MainAppScreen` changes; no unexpected affected processes.

---

## Remote Config setup (manual, post-implementation)

After the code lands, the following Remote Config parameters must be created in the Firebase console for the app's project:

| Key | Type | Example |
|-----|------|---------|
| `update_latest_version_code` | Long | `2` |
| `update_min_version_code` | Long | `2` |
| `update_latest_version_name` | String | `1.1.0` |
| `update_release_notes` | String | `Bug fixes and improvements` |
| `update_direct_download_url` | String | `https://example.com/namazvakitleri.apk` |
| `update_force_version_codes` | String | `2,3` (comma-separated) |
| `update_optional_version_codes` | String | `4,5` (comma-separated) |

### Targeting semantics

The decision is evaluated in this order:

1. Installed `versionCode` is in `update_force_version_codes` → **ForceUpdate**
2. Installed `versionCode` is in `update_optional_version_codes` → **OptionalUpdate**
3. Installed `versionCode` < `update_min_version_code` → **ForceUpdate**
4. Installed `versionCode` < `update_latest_version_code` → **OptionalUpdate**
5. Otherwise → **NoUpdate**

This supports three targeting styles:

- **Only one version**: set `update_force_version_codes = "2"` (or `update_optional_version_codes = "2"`) to target just version 2.
- **Between versions**: set `update_force_version_codes = "2,3"` (or `update_optional_version_codes = "2,3"`) to target versions 2–3.
- **All versions**: set `update_min_version_code` above the installed `versionCode` to force everyone below it, or keep `update_min_version_code` at/below the lowest installed version and raise `update_latest_version_code` to make all older versions optional.

The version-code lists are optional; if left empty, behavior falls back to the `update_min_version_code` / `update_latest_version_code` comparison.

## Out of scope

- Google Play In-App Updates API.
- Periodic background checks (launch + resume only).
- Manual "Check for updates" in Settings.
- Changelog/version history screen.
