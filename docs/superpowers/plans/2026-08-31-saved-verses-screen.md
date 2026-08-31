# Saved Verses Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Saved Verses" screen reachable from a bookmark icon on Home, where users can view, share, swipe-to-delete, and drag-to-reorder their bookmarked Quran verses.

**Architecture:** A new `SavedVersesScreen` sub-screen in the Home navigation graph, mirroring the existing `MyLocationsScreen` pattern (reorderable `LazyColumn` via `sh.calvin.reorderable`, swipe-to-delete, empty state, top bar). Data flows through new `GetSavedVersesUseCase` / `ReorderSavedVersesUseCase` → `IQuranRepository.getSavedVerses()` / `reorderSavedVerses()` → `SavedVersesStore` (DataStore). `SavedVersesStore.toggle()` is changed to **prepend** so the store order is newest-first, making display order == persisted order (stable after reorder).

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3), Navigation Compose, Koin, DataStore Preferences, `sh.calvin.reorderable`, kotlinx.serialization. Tests: JUnit 5, MockK, Turbine, Truth, Robolectric.

---

## File Structure

**Create:**
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt`
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt` (Route + stateless Screen + VerseRow)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseShare.kt`
- `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModelTest.kt`
- `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`

**Modify:**
- `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt` (prepend + reorder)
- `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`
- `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt`
- `prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeGraph.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt` (bookmark icon overlay)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt` (use shared `shareVerse`)
- `prayer_feature/home/src/main/res/values/strings.xml` (new strings)
- Tests: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt`, `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt`, `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

---

## Task 1: `SavedVersesStore` — prepend on save + `reorder()`

**Files:**
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these two tests to `SavedVersesStoreTest.kt` (after the existing `saved verses persist across store instances` test):

```kotlin
@Test
fun `toggle prepends new saves so newest is first`() = runBlocking {
    store.toggle(verse(1))
    store.toggle(verse(2))

    val saved = store.getSavedVerses()

    assertThat(saved.map { it.numberInSurah }).containsExactly(2, 1).inOrder()
}

@Test
fun `reorder rewrites the persisted order`() = runBlocking {
    store.toggle(verse(1))
    store.toggle(verse(2))
    store.toggle(verse(3))

    store.reorder(listOf(verse(1), verse(3), verse(2)))

    val saved = store.getSavedVerses()
    assertThat(saved.map { it.numberInSurah }).containsExactly(1, 3, 2).inOrder()
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest"`
Expected: FAIL — `reorder` is unresolved and `toggle` appends (order is `1, 2` not `2, 1`).

- [ ] **Step 3: Implement**

In `SavedVersesStore.kt`, change the add branch of `toggle` to prepend, and add `reorder`:

```kotlin
suspend fun toggle(verse: AyahData): Unit {
    dataStore.edit { prefs ->
        val raw = prefs[key]
        val current = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
        }
        val updated = if (current.any { it == verse }) {
            current.filterNot { it == verse }
        } else {
            listOf(verse) + current
        }
        prefs[key] = withContext(Dispatchers.Default) { json.encodeToString(updated) }
    }
}

suspend fun reorder(verses: List<AyahData>) {
    dataStore.edit { prefs ->
        prefs[key] = withContext(Dispatchers.Default) { json.encodeToString(verses) }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest"`
Expected: PASS (all 5 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt
git commit -m "feat(prayer:data): prepend saved verses and add reorder to store"
```

---

## Task 2: Repository — `getSavedVerses()` + `reorderSavedVerses()`

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

Append to `QuranRepositoryTest.kt`:

```kotlin
@Test
fun `getSavedVerses returns the store list`() = runTest {
    val saved = listOf(verse(1, 1), verse(1, 2))
    coEvery { savedVersesStore.getSavedVerses() } returns saved

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    val result = repository.getSavedVerses()

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo(saved)
}

@Test
fun `reorderSavedVerses persists the new order`() = runTest {
    val order = listOf(verse(1, 2), verse(1, 1))
    coEvery { savedVersesStore.reorder(order) } returns Unit

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    val result = repository.reorderSavedVerses(order)

    assertThat(result.isSuccess).isTrue()
    coVerify(exactly = 1) { savedVersesStore.reorder(order) }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*QuranRepositoryTest"`
Expected: FAIL — `getSavedVerses` / `reorderSavedVerses` are unresolved on `IQuranRepository`.

- [ ] **Step 3: Implement**

In `IQuranRepository.kt`, add:

```kotlin
suspend fun getSavedVerses(): Result<List<AyahData>>
suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit>
```

In `QuranRepository.kt`, add:

```kotlin
override suspend fun getSavedVerses(): Result<List<AyahData>> =
    runCatching { savedVersesStore.getSavedVerses() }

override suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit> =
    runCatching { savedVersesStore.reorder(verses) }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*QuranRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt
git commit -m "feat(prayer): expose getSavedVerses and reorderSavedVerses on repository"
```

---

## Task 3: Use cases — `GetSavedVersesUseCase` + `ReorderSavedVersesUseCase`

**Files:**
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt`
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt`

These are trivial delegations (same pattern as `ToggleSavedVerseUseCase`, which has no dedicated test). They are covered indirectly by the ViewModel test in Task 5.

- [ ] **Step 1: Create `GetSavedVersesUseCase.kt`**

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): Result<List<AyahData>> = repository.getSavedVerses()
}
```

- [ ] **Step 2: Create `ReorderSavedVersesUseCase.kt`**

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class ReorderSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(verses: List<AyahData>): Result<Unit> =
        repository.reorderSavedVerses(verses)
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer:domain:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt
git commit -m "feat(prayer:domain): add get and reorder saved verses use cases"
```

---

## Task 4: Navigation route — `SavedVersesScreen`

**Files:**
- Modify: `prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeGraph.kt`

- [ ] **Step 1: Add the route**

In `PrayerScreens.kt`, add to the `Screen` sealed class:

```kotlin
data object SavedVersesScreen: Screen("saved_verses")
```

- [ ] **Step 2: Register the composable in `HomeGraph.kt`**

Replace the body of `homeGraph` with:

```kotlin
fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable(Screen.HomeScreen.route) {
        HomeRoute(navController = navController)
    }
    composable(Screen.SavedVersesScreen.route) {
        SavedVersesRoute(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

Add the import: `import com.kutluoglu.prayer_feature.home.SavedVersesRoute`

Note: `SavedVersesRoute` is created in Task 7. This task will not compile until Task 7 lands; that is expected. If you are executing tasks in order, do not run the build here — proceed to Task 5 and 7, then verify.

- [ ] **Step 3: Commit**

```bash
git add prayer_navigation/core/src/main/java/com/kutluoglu/prayer_navigation/core/PrayerScreens.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeGraph.kt
git commit -m "feat(home): add saved verses navigation route"
```

---

## Task 5: `SavedVersesViewModel` + state + events

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `SavedVersesViewModelTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class SavedVersesViewModelTest {

    private val getSavedVersesUseCase: GetSavedVersesUseCase = mockk()
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Fatihah",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun `loads saved verses on init`() = runTest {
        val verses = listOf(verse(1), verse(2))
        coEvery { getSavedVersesUseCase() } returns Result.success(verses)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)

        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(verses))
    }

    @Test
    fun `emits error when load fails`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.failure(RuntimeException("boom"))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)

        assertThat(vm.uiState.value).isInstanceOf(SavedVersesUiState.Error::class.java)
    }

    @Test
    fun `remove toggles the verse and reloads`() = runTest {
        coEvery { getSavedVersesUseCase() } returnsMany listOf(
            Result.success(listOf(verse(1), verse(2))),
            Result.success(listOf(verse(2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1)) } returns Result.success(Unit)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1)))

        coVerify { toggleSavedVerseUseCase(verse(1)) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(2))))
    }

    @Test
    fun `reorder persists the new order`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1), verse(2)))
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnReorder(listOf(verse(2), verse(1))))

        coVerify { reorderSavedVersesUseCase(listOf(verse(2), verse(1))) }
    }

    @Test
    fun `select opens the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1)))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isEqualTo(verse(1))
        assertThat(state.isDetailVisible).isTrue()
    }

    @Test
    fun `dismiss closes the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1)))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))
        vm.onEvent(SavedVersesEvent.OnDismissDetail)

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isNull()
        assertThat(state.isDetailVisible).isFalse()
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesViewModelTest"`
Expected: FAIL — `SavedVersesViewModel`, `SavedVersesUiState`, `SavedVersesEvent` are unresolved.

- [ ] **Step 3: Implement**

Create `state/SavedVersesUiState.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.quran.AyahData

sealed class SavedVersesUiState {
    data object Loading : SavedVersesUiState()
    data class Success(
        val verses: List<AyahData>,
        val selectedVerse: AyahData? = null,
        val isDetailVisible: Boolean = false
    ) : SavedVersesUiState()
    data class Error(val message: String) : SavedVersesUiState()
}
```

Create `SavedVersesEvent.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData

sealed class SavedVersesEvent {
    data class OnRemove(val verse: AyahData) : SavedVersesEvent()
    data class OnReorder(val verses: List<AyahData>) : SavedVersesEvent()
    data class OnSelect(val verse: AyahData) : SavedVersesEvent()
    data object OnDismissDetail : SavedVersesEvent()
}
```

Create `SavedVersesViewModel.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SavedVersesViewModel(
    private val getSavedVersesUseCase: GetSavedVersesUseCase,
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase,
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedVersesUiState>(SavedVersesUiState.Loading)
    val uiState: StateFlow<SavedVersesUiState> = _uiState.asStateFlow()

    init {
        loadSavedVerses()
    }

    fun onEvent(event: SavedVersesEvent) {
        when (event) {
            is SavedVersesEvent.OnRemove -> removeVerse(event.verse)
            is SavedVersesEvent.OnReorder -> reorder(event.verses)
            is SavedVersesEvent.OnSelect -> selectVerse(event.verse)
            SavedVersesEvent.OnDismissDetail -> dismissDetail()
        }
    }

    private fun loadSavedVerses() {
        viewModelScope.launch {
            _uiState.value = SavedVersesUiState.Loading
            getSavedVersesUseCase()
                .onSuccess { verses ->
                    _uiState.value = SavedVersesUiState.Success(verses)
                }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to load saved verses -> ${it.message}")
                    _uiState.value = SavedVersesUiState.Error(
                        it.message ?: "Saved verses could not be loaded."
                    )
                }
        }
    }

    private fun removeVerse(verse: AyahData) {
        viewModelScope.launch {
            toggleSavedVerseUseCase(verse)
                .onSuccess {
                    dismissDetail()
                    loadSavedVerses()
                }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to remove saved verse -> ${it.message}")
                }
        }
    }

    private fun reorder(verses: List<AyahData>) {
        viewModelScope.launch {
            reorderSavedVersesUseCase(verses)
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder saved verses -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun selectVerse(verse: AyahData) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = verse, isDetailVisible = true)
    }

    private fun dismissDetail() {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = null, isDetailVisible = false)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModelTest.kt
git commit -m "feat(home): add saved verses view model with load/remove/reorder/select"
```

---

## Task 6: Extract shared `shareVerse` helper

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseShare.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt`

- [ ] **Step 1: Create `QuranVerseShare.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun shareVerse(verse: AyahData, verseFormatter: QuranVerseFormatter, context: Context) {
    val localizedSurahName = verseFormatter.getLocalizedNameOf(verse, context)
    val verseInfo = "($localizedSurahName - $verse)"
    val appName = context.getString(R.string.app_name)
    val sharedApp = "\n\n${context.getString(R.string.shared_from_app, appName)}"
    val fullTextToShare = "\"${verse.text}\" - $verseInfo $sharedApp"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_TEXT, fullTextToShare)
        val iconUri = getIconUri(context)
        iconUri?.let {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_verse))
    )
}

private fun getIconUri(context: Context): Uri? {
    try {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val originalBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            ).also {
                val canvas = android.graphics.Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "app_icon.png")
        FileOutputStream(imageFile).use {
            originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}
```

- [ ] **Step 2: Simplify `VerseDetailSheetContent.kt`**

Remove the private `shareVerse(...)` and `getIconUri(...)` functions and the now-unused imports (`Intent`, `Bitmap`, `BitmapDrawable`, `Uri`, `File`, `FileOutputStream`, `IOException`, `FileProvider`, `createBitmap`). Replace the body of the share `IconButton`'s `onClick`:

```kotlin
IconButton(
    onClick = { shareVerse(verse, verseFormatter, context) }
) {
    Icon(
        Icons.Default.Share,
        contentDescription = context.getString(R.string.share_verse)
    )
}
```

Add the import: `import com.kutluoglu.prayer_feature.home.common.shareVerse`

Also delete the now-unused `fullTextToShare` / `sharedApp` / `appName` locals at the top of `VerseDetailSheetContent` (they were only used by the old share function).

- [ ] **Step 3: Verify compilation and existing tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: PASS (the sheet's share button is not exercised by these tests, but the module must compile).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseShare.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt
git commit -m "refactor(home): extract shared shareVerse helper"
```

---

## Task 7: `SavedVersesScreen` UI + strings

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`
- Modify: `prayer_feature/home/src/main/res/values/strings.xml`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`

- [ ] **Step 1: Add string resources**

Append to `prayer_feature/home/src/main/res/values/strings.xml` (before `</resources>`):

```xml
<string name="saved_verses">Saved Verses</string>
<string name="no_saved_verses">No saved verses yet. Bookmark a verse from the home screen.</string>
<string name="back">Back</string>
<string name="delete">Delete</string>
<string name="reorder">Reorder</string>
```

- [ ] **Step 2: Write the failing Robolectric test**

Create `SavedVersesScreenTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavedVersesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Fatihah",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `renders empty state when no saved verses`() {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(emptyList()),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No saved verses yet. Bookmark a verse from the home screen.")
            .assertIsDisplayed()
    }

    @Test
    fun `renders saved verses list`() {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1), verse(2))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Text 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun `tapping a verse fires OnSelect`() {
        val events = mutableListOf<SavedVersesEvent>()
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Text 1").performClick()
        composeTestRule.waitForIdle()
        assertThat(events).contains(SavedVersesEvent.OnSelect(verse(1)))
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesScreenTest"`
Expected: FAIL — `SavedVersesScreen` is unresolved.

- [ ] **Step 4: Implement `SavedVersesScreen.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.EmptyStateContent
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.common.shareVerse
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.snapshotFlow
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SavedVersesRoute(
    onNavigateBack: () -> Unit,
    viewModel: SavedVersesViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    SavedVersesScreen(
        state = state,
        verseFormatter = verseFormatter,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedVersesScreen(
    state: SavedVersesUiState,
    verseFormatter: QuranVerseFormatter,
    onNavigateBack: () -> Unit,
    onEvent: (SavedVersesEvent) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val entries = remember { mutableStateListOf<AyahData>() }
    val context = LocalContext.current
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = entries.indexOfFirst { it == from.key }
        val toIndex = entries.indexOfFirst { it == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            entries.add(toIndex, entries.removeAt(fromIndex))
        }
    }

    LaunchedEffect(state) {
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        val success = state as? SavedVersesUiState.Success ?: return@LaunchedEffect
        if (entries.toList() != success.verses) {
            entries.clear()
            entries.addAll(success.verses)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .distinctUntilChanged()
            .filter { !it }
            .collect { onEvent(SavedVersesEvent.OnReorder(entries.toList())) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_verses)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            SavedVersesUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { LoadingIndicator() }

            is SavedVersesUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.message) }

            is SavedVersesUiState.Success -> {
                if (state.verses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateContent(
                            icon = Icons.Outlined.BookmarkBorder,
                            text = stringResource(R.string.no_saved_verses)
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        items(entries, key = { it }) { verse ->
                            ReorderableItem(state = reorderableState, key = verse) { isDragging ->
                                SwipeToDismissBox(
                                    state = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value != SwipeToDismissBoxValue.Settled) {
                                                entries.remove(verse)
                                                onEvent(SavedVersesEvent.OnRemove(verse))
                                            }
                                            true
                                        }
                                    ),
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                ) {
                                    VerseRow(
                                        verse = verse,
                                        verseFormatter = verseFormatter,
                                        isDragging = isDragging,
                                        onSelect = { onEvent(SavedVersesEvent.OnSelect(verse)) },
                                        onShare = { shareVerse(verse, verseFormatter, context) },
                                        dragHandle = {
                                            IconButton(
                                                modifier = Modifier.draggableHandle(),
                                                onClick = {}
                                            ) {
                                                Icon(
                                                    Icons.Rounded.DragHandle,
                                                    contentDescription = stringResource(R.string.reorder)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val success = state as? SavedVersesUiState.Success
    CustomBottomSheet(
        isVisible = success?.isDetailVisible == true,
        onDismiss = { onEvent(SavedVersesEvent.OnDismissDetail) }
    ) {
        success?.selectedVerse?.let { verse ->
            VerseDetailSheetContent(
                verse = verse,
                verseFormatter = verseFormatter,
                isSaved = true,
                onToggleSaved = { onEvent(SavedVersesEvent.OnRemove(verse)) }
            )
        }
    }
}

@Composable
private fun VerseRow(
    verse: AyahData,
    verseFormatter: QuranVerseFormatter,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val localizedSurahName = verseFormatter.getLocalizedNameOf(verse, context)
    val verseInfo = "($localizedSurahName - $verse)"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = verseInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share_verse)
                )
            }
            dragHandle?.invoke()
        }
    }
}
```

Note: the swipe background is cosmetic — the functional delete happens in `confirmValueChange`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesScreenTest"`
Expected: PASS.

- [ ] **Step 6: Verify the whole home module compiles (Task 4 route now resolves)**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt prayer_feature/home/src/main/res/values/strings.xml prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt
git commit -m "feat(home): add saved verses screen with reorder and swipe-to-delete"
```

---

## Task 8: Home bookmark icon entry point

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `HomeScreenTest.kt`:

```kotlin
@Test
fun `bookmark icon fires onNavigateToSavedVerses`() {
    var navigated = false
    composeTestRule.setContent {
        HomeScreen(
            navController = mockk<NavController>(relaxed = true),
            uiState = HomeUiState.Empty,
            locationsState = LocationsState(),
            prayerDataByLocation = emptyMap(),
            activeLocationId = null,
            quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
            onNavigateToSavedVerses = { navigated = true },
            onEvent = {}
        )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Saved Verses").performClick()
    composeTestRule.waitForIdle()
    assertThat(navigated).isTrue()
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: FAIL — `onNavigateToSavedVerses` is unresolved and no "Saved Verses" node exists.

- [ ] **Step 3: Implement**

In `HomeScreen.kt`:
- Add parameter `onNavigateToSavedVerses: () -> Unit = {}` (default keeps existing call sites compiling).
- Add imports: `androidx.compose.foundation.layout.statusBarsPadding`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.outlined.BookmarkBorder`, `androidx.compose.material3.Icon`, `androidx.compose.material3.IconButton`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.res.stringResource`, `com.kutluoglu.prayer_feature.home.R`.
- In the top-level `Box`, after `PermissionHandler(...)`, add the overlay:

```kotlin
IconButton(
    onClick = onNavigateToSavedVerses,
    modifier = Modifier
        .align(Alignment.TopEnd)
        .statusBarsPadding()
        .padding(8.dp)
) {
    Icon(
        Icons.Outlined.BookmarkBorder,
        contentDescription = stringResource(R.string.saved_verses)
    )
}
```

In `HomeRoute.kt`, pass the navigation callback to `HomeScreen`:

```kotlin
HomeScreen(
    navController = navController,
    uiState = uiState,
    locationsState = locations,
    prayerDataByLocation = prayerData,
    activeLocationId = activeLocationId,
    quranVerseFormatter = verseFormatter,
    onNavigateToSavedVerses = {
        navController.navigate(Screen.SavedVersesScreen.route)
    },
    onEvent = { event -> viewModel.onEvent(event) }
)
```

Add the import: `import com.kutluoglu.prayer_navigation.core.Screen`

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*HomeScreenTest"`
Expected: PASS (all existing + new test).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/navigation/HomeRoute.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "feat(home): add bookmark icon entry point to saved verses"
```

---

## Task 9: Full regression + impact check

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Check affected scope**

Run: `gitnexus_detect_changes()`
Expected: only the expected symbols — `SavedVersesStore`, `QuranRepository`, `IQuranRepository`, `GetSavedVersesUseCase`, `ReorderSavedVersesUseCase`, `SavedVersesViewModel`, `SavedVersesScreen`, `HomeScreen`, `HomeRoute`, `HomeGraph`, `VerseDetailSheetContent`.

- [ ] **Step 4: Manual smoke test (device/emulator)**

1. Open Home → bookmark icon top-right → Saved Verses screen opens.
2. Empty state shows when no verses saved.
3. Save a verse from the verse-of-the-day sheet → reopen Saved Verses → verse appears at top.
4. Tap a verse → detail sheet opens; unsave removes it and closes the sheet.
5. Swipe a verse → it is removed and persists after restart.
6. Drag a verse → order persists after restart.
7. Share from a row opens the share sheet.

---

## Self-Review Notes

- **Spec coverage:** All 5 goal items map to tasks — view (Task 7), tap-to-detail (Task 7 + Task 5), swipe-delete (Task 7), share-from-list (Task 7 + Task 6), drag-reorder (Task 7 + Task 1/2/3). Entry point (Task 8). Persistence (Task 1). Tests (Tasks 1, 2, 5, 7, 8).
- **Ordering fix:** The spec was updated so `SavedVersesStore.toggle()` prepends; display order == persisted order, so reorder survives reload.
- **Type consistency:** `getSavedVerses(): Result<List<AyahData>>` and `reorderSavedVerses(verses: List<AyahData>): Result<Unit>` are used identically across repository, use cases, and ViewModel.
