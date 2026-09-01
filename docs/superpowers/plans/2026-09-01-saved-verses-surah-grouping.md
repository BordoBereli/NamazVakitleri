# Saved Verses Surah Grouping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Saved Verses screen to group verses by surah with collapsible headers, search, jump chips, and two-level reorder, backed by a nested `SavedVerseGroup` storage model.

**Architecture:** Replace the flat `List<AyahData>` saved-verses storage with a nested `List<SavedVerseGroup>` (surah + ordered verses). The repository exposes groups; the ViewModel stays thin (load, filter, collapse, forward reorder); the screen renders a grouped list with collapsible headers, a search field, surah jump chips, and two-level drag reorder.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Preferences DataStore, kotlinx.serialization, Koin, `sh.calvin.reorderable`, JUnit 5 + MockK + Turbine + Truth, Robolectric.

**Spec:** `docs/superpowers/specs/2026-09-01-saved-verses-surah-grouping-design.md`

---

## File Structure

**Create:**
- `prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/SavedVerseGroup.kt` — nested group model
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetCollapsedSurahsUseCase.kt`
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/SetCollapsedSurahsUseCase.kt`

**Modify:**
- `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt` — nested storage + migration + collapse prefs
- `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt` — group API
- `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt` — group API
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt`
- `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseFormatter.kt` — add `SurahInfo` overload
- `prayer_feature/home/src/main/res/values/strings.xml` — new strings
- Tests: `SavedVersesStoreTest`, `QuranRepositoryTest`, `SavedVersesViewModelTest`, `SavedVersesEndToEndTest`, `SavedVersesScreenWipeTest`, new `SavedVersesScreenTest`

**Design deviations from spec (flagged):**
1. **Headers are reorderable items, not sticky.** `sh.calvin.reorderable` requires `ReorderableItem` as a direct `LazyColumn` item; `stickyHeader` items are not tracked by the library. Headers are styled as distinct section headers and remain drag-reorderable. (Spec said "sticky" — accepted trade-off.)
2. **Search matches `englishName` + Arabic `name` + verse text.** The ViewModel has no `Context`, so the localized (Turkish) surah name isn't available for filtering. `englishName` serves as the searchable transliteration.

---

## Task 1: `SavedVerseGroup` model + grouping helper

**Files:**
- Create: `prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/SavedVerseGroup.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt` (add grouping test)

- [ ] **Step 1: Write the failing test for `groupBySurah`**

Add to `SavedVersesStoreTest.kt`:

```kotlin
@Test
fun `groupBySurah groups flat verses by surah preserving order`() = runBlocking {
    val surah1 = SurahInfo("Al-Fatihah", "الفاتحة", 1, 7)
    val surah36 = SurahInfo("Ya-Sin", "يس", 36, 83)
    val flat = listOf(
        AyahData("a", surah36, 1),
        AyahData("b", surah1, 1),
        AyahData("c", surah1, 2),
        AyahData("d", surah36, 2),
    )

    val groups = groupBySurah(flat)

    assertThat(groups.map { it.surah.number }).containsExactly(36, 1).inOrder()
    assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(1, 2).inOrder()
    assertThat(groups[1].verses.map { it.numberInSurah }).containsExactly(1, 2).inOrder()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest*"`
Expected: FAIL — `groupBySurah` unresolved.

- [ ] **Step 3: Create the model**

Create `prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/SavedVerseGroup.kt`:

```kotlin
package com.kutluoglu.prayer.model.quran

import kotlinx.serialization.Serializable

@Serializable
data class SavedVerseGroup(
    val surah: SurahInfo,
    val verses: List<AyahData>
)
```

- [ ] **Step 4: Add the `groupBySurah` helper**

Add to the bottom of `SavedVersesStore.kt` (top-level, `internal`):

```kotlin
internal fun groupBySurah(verses: List<AyahData>): List<SavedVerseGroup> {
    val order = LinkedHashMap<Int, SavedVerseGroup>()
    verses.forEach { verse ->
        val existing = order[verse.surah.number]
        order[verse.surah.number] = if (existing == null) {
            SavedVerseGroup(surah = verse.surah, verses = listOf(verse))
        } else {
            existing.copy(verses = existing.verses + verse)
        }
    }
    return order.values.toList()
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/SavedVerseGroup.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt
git commit -m "feat(quran): add SavedVerseGroup model and grouping helper"
```

---

## Task 2: `SavedVersesStore` nested storage + migration + collapse prefs

**Files:**
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt`

- [ ] **Step 1: Write failing tests**

Replace the tests in `SavedVersesStoreTest.kt` that reference `getSavedVerses()` / `reorder()` with group-based tests. Add:

```kotlin
@Test
fun `migrates legacy flat list to nested groups`() = runBlocking {
    val legacy = listOf(
        AyahData("a", SurahInfo("Ya-Sin", "يس", 36, 83), 2),
        AyahData("b", SurahInfo("Al-Fatihah", "الفاتحة", 1, 7), 1),
        AyahData("c", SurahInfo("Ya-Sin", "يس", 36, 83), 1),
    )
    dataStore.edit { it[stringPreferencesKey("saved_verses")] = json.encodeToString(legacy) }

    val groups = store.getSavedVerseGroups()

    assertThat(groups.map { it.surah.number }).containsExactly(36, 1).inOrder()
    assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2, 1).inOrder()
    assertThat(groups[1].verses.map { it.numberInSurah }).containsExactly(1).inOrder()
}

@Test
fun `toggle adds a verse to an existing group and removes it`() = runBlocking {
    store.toggle(verse(1))
    store.toggle(verse(2))

    var groups = store.getSavedVerseGroups()
    assertThat(groups).hasSize(1)
    assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2, 1).inOrder()

    store.toggle(verse(1))
    groups = store.getSavedVerseGroups()
    assertThat(groups[0].verses.map { it.numberInSurah }).containsExactly(2).inOrder()
}

@Test
fun `toggle creates a new group when the surah is new`() = runBlocking {
    store.toggle(verse(1))
    val otherSurah = AyahData(
        text = "T",
        surah = SurahInfo("Ya-Sin", "يس", 36, 83),
        numberInSurah = 1
    )
    store.toggle(otherSurah)

    val groups = store.getSavedVerseGroups()
    assertThat(groups.map { it.surah.number }).containsExactly(1, 36).inOrder()
}

@Test
fun `removing the last verse drops the group`() = runBlocking {
    store.toggle(verse(1))
    store.toggle(verse(1))

    assertThat(store.getSavedVerseGroups()).isEmpty()
}

@Test
fun `saveGroups persists the nested order`() = runBlocking {
    store.toggle(verse(1))
    store.toggle(verse(2))
    val groups = store.getSavedVerseGroups()
    val reversed = groups.map { it.copy(verses = it.verses.reversed()) }

    store.saveGroups(reversed)

    assertThat(store.getSavedVerseGroups()[0].verses.map { it.numberInSurah })
        .containsExactly(1, 2).inOrder()
}

@Test
fun `collapse state persists across store instances`() = runBlocking {
    store.setCollapsedSurahs(setOf(1, 36))

    val reloaded = SavedVersesStore(dataStore)
    assertThat(reloaded.getCollapsedSurahs()).containsExactly(1, 36)
}
```

Add imports to the test file: `androidx.datastore.preferences.core.edit`, `androidx.datastore.preferences.core.stringPreferencesKey`, `kotlinx.serialization.json.Json`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest*"`
Expected: FAIL — `getSavedVerseGroups`, `saveGroups`, `getCollapsedSurahs`, `setCollapsedSurahs` unresolved; `getSavedVerses`/`reorder` removed.

- [ ] **Step 3: Rewrite `SavedVersesStore`**

Replace the body of `SavedVersesStore.kt` with:

```kotlin
package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Persists user-bookmarked verses as a JSON list of [SavedVerseGroup] (surah + ordered
 * verses). Backed by the same quranStore Preferences DataStore as [QuranSurahCache].
 * Migrates the legacy flat `saved_verses` list to the nested format on first access.
 */
@Single
class SavedVersesStore(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val keyGroups = stringPreferencesKey("saved_verse_groups")
    private val keyLegacy = stringPreferencesKey("saved_verses")
    private val keyCollapsed = stringPreferencesKey("saved_verses_collapsed")

    suspend fun getSavedVerseGroups(): List<SavedVerseGroup> {
        migrateIfNeeded()
        val raw = dataStore.data.map { it[keyGroups] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<SavedVerseGroup>>(raw) }.getOrDefault(emptyList())
        }
    }

    suspend fun isSaved(verse: AyahData): Boolean =
        getSavedVerseGroups().any { group -> group.verses.any { it.sameVerse(verse) } }

    suspend fun toggle(verse: AyahData) {
        migrateIfNeeded()
        dataStore.edit { prefs ->
            val current = decodeGroups(prefs[keyGroups])
            val updated = if (current.any { group -> group.verses.any { it.sameVerse(verse) } }) {
                current.mapNotNull { group ->
                    val remaining = group.verses.filterNot { it.sameVerse(verse) }
                    if (remaining.isEmpty()) null else group.copy(verses = remaining)
                }
            } else {
                val existing = current.firstOrNull { it.surah.number == verse.surah.number }
                if (existing != null) {
                    current.map { group ->
                        if (group.surah.number == verse.surah.number) {
                            group.copy(verses = listOf(verse) + group.verses)
                        } else group
                    }
                } else {
                    listOf(SavedVerseGroup(surah = verse.surah, verses = listOf(verse))) + current
                }
            }
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(updated) }
        }
    }

    suspend fun saveGroups(groups: List<SavedVerseGroup>) {
        migrateIfNeeded()
        dataStore.edit { prefs ->
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(groups) }
        }
    }

    suspend fun getCollapsedSurahs(): Set<Int> {
        val raw = dataStore.data.map { it[keyCollapsed] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun setCollapsedSurahs(surahs: Set<Int>) {
        dataStore.edit { prefs ->
            prefs[keyCollapsed] = surahs.sorted().joinToString(",")
        }
    }

    private suspend fun migrateIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[keyGroups] != null) return@edit
            val raw = prefs[keyLegacy] ?: return@edit
            if (raw.isBlank()) return@edit
            val flat = runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
            val groups = groupBySurah(flat)
            prefs[keyGroups] = withContext(Dispatchers.Default) { json.encodeToString(groups) }
            prefs.remove(keyLegacy)
        }
    }

    private fun decodeGroups(raw: String?): List<SavedVerseGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<SavedVerseGroup>>(raw) }.getOrDefault(emptyList())
    }

    private fun AyahData.sameVerse(other: AyahData): Boolean =
        surah.number == other.surah.number && numberInSurah == other.numberInSurah
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*SavedVersesStoreTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt
git commit -m "feat(quran): nested saved-verses storage with migration and collapse prefs"
```

---

## Task 3: Repository + domain interface + use cases

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt`
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt`
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetCollapsedSurahsUseCase.kt`
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/SetCollapsedSurahsUseCase.kt`
- Test: `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

Update `QuranRepositoryTest.kt`. Replace the `getSavedVerses` / `reorderSavedVerses` tests with group-based ones:

```kotlin
@Test
fun `getSavedVerses returns the store groups`() = runTest {
    val groups = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 1), verse(1, 2))))
    coEvery { savedVersesStore.getSavedVerseGroups() } returns groups
    coEvery { quranSurahCache.getSurah(any(), any()) } returns listOf(verse(1, 1), verse(1, 2))

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    val result = repository.getSavedVerses("tr")

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo(groups)
}

@Test
fun `getSavedVerses re-localizes verses in the requested language`() = runTest {
    val stored = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 1).copy(text = "Türkçe metin"))))
    val localized = listOf(verse(1, 1).copy(text = "English text"))
    coEvery { savedVersesStore.getSavedVerseGroups() } returns stored
    coEvery { quranSurahCache.getSurah(any(), any()) } returns null
    coEvery { quranSurahCache.putSurah(any(), any(), any()) } returns Unit
    coEvery { quranDataSource.getSurah(any(), "en") } returns Result.success(localized)

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    val result = repository.getSavedVerses("en")

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()[0].verses).isEqualTo(localized)
}

@Test
fun `reorderSavedVerses persists the nested groups`() = runTest {
    val groups = listOf(SavedVerseGroup(verse(1, 1).surah, listOf(verse(1, 2), verse(1, 1))))
    coEvery { savedVersesStore.saveGroups(groups) } returns Unit

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    val result = repository.reorderSavedVerses(groups)

    assertThat(result.isSuccess).isTrue()
    coVerify(exactly = 1) { savedVersesStore.saveGroups(groups) }
}

@Test
fun `collapse state delegates to the store`() = runTest {
    coEvery { savedVersesStore.getCollapsedSurahs() } returns setOf(1)
    coEvery { savedVersesStore.setCollapsedSurahs(any()) } returns Unit

    val repository = QuranRepository(quranDataSource, quranSurahCache, savedVersesStore)
    assertThat(repository.getCollapsedSurahs()).containsExactly(1)
    repository.setCollapsedSurahs(setOf(36))
    coVerify(exactly = 1) { savedVersesStore.setCollapsedSurahs(setOf(36)) }
}
```

Add import: `com.kutluoglu.prayer.model.quran.SavedVerseGroup`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*QuranRepositoryTest*"`
Expected: FAIL — `getSavedVerseGroups`/`saveGroups`/`getCollapsedSurahs`/`setCollapsedSurahs` unresolved on the mock; `getSavedVerses` returns `List<AyahData>` not groups.

- [ ] **Step 3: Update `IQuranRepository`**

```kotlin
package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup

interface IQuranRepository {
    suspend fun getRandomVerse(language: String): Result<AyahData>
    suspend fun getVerse(surahNumber: Int, numberInSurah: Int, language: String): Result<AyahData>
    suspend fun isVerseSaved(verse: AyahData): Boolean
    suspend fun toggleSavedVerse(verse: AyahData): Result<Unit>
    suspend fun getSavedVerses(language: String): Result<List<SavedVerseGroup>>
    suspend fun reorderSavedVerses(groups: List<SavedVerseGroup>): Result<Unit>
    suspend fun getCollapsedSurahs(): Set<Int>
    suspend fun setCollapsedSurahs(surahs: Set<Int>)
}
```

- [ ] **Step 4: Update `QuranRepository`**

Replace the `getSavedVerses` / `reorderSavedVerses` overrides and add collapse methods:

```kotlin
override suspend fun getSavedVerses(language: String): Result<List<SavedVerseGroup>> = runCatching {
    savedVersesStore.getSavedVerseGroups().map { group ->
        group.copy(verses = group.verses.map { verse ->
            getVerse(verse.surah.number, verse.numberInSurah, language).getOrElse { verse }
        })
    }
}

override suspend fun reorderSavedVerses(groups: List<SavedVerseGroup>): Result<Unit> =
    runCatching { savedVersesStore.saveGroups(groups) }

override suspend fun getCollapsedSurahs(): Set<Int> = savedVersesStore.getCollapsedSurahs()

override suspend fun setCollapsedSurahs(surahs: Set<Int>) {
    savedVersesStore.setCollapsedSurahs(surahs)
}
```

Add import: `com.kutluoglu.prayer.model.quran.SavedVerseGroup`.

- [ ] **Step 5: Update the use cases**

`GetSavedVersesUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(language: String): Result<List<SavedVerseGroup>> =
        repository.getSavedVerses(language)
}
```

`ReorderSavedVersesUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class ReorderSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(groups: List<SavedVerseGroup>): Result<Unit> =
        repository.reorderSavedVerses(groups)
}
```

Create `GetCollapsedSurahsUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetCollapsedSurahsUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): Set<Int> = repository.getCollapsedSurahs()
}
```

Create `SetCollapsedSurahsUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class SetCollapsedSurahsUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(surahs: Set<Int>) {
        repository.setCollapsedSurahs(surahs)
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="*QuranRepositoryTest*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetSavedVersesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ReorderSavedVersesUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/GetCollapsedSurahsUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/SetCollapsedSurahsUseCase.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt
git commit -m "feat(quran): expose saved-verse groups and collapse state through repository"
```

---

## Task 4: ViewModel + UI state + events

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

Rewrite `SavedVersesViewModelTest.kt` to the new model. Replace the file body with:

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.SetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(ExecutionMode.SAME_THREAD)
class SavedVersesViewModelTest {

    private val getSavedVersesUseCase: GetSavedVersesUseCase = mockk()
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()
    private val getCollapsedSurahsUseCase: GetCollapsedSurahsUseCase = mockk()
    private val setCollapsedSurahsUseCase: SetCollapsedSurahsUseCase = mockk()
    private val languageProvider: LanguageProvider = mockk()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo("Surah $surahNumber", "سورة", surahNumber, 10),
        numberInSurah = numberInSurah
    )

    private fun group(surahNumber: Int, vararg numbers: Int) = SavedVerseGroup(
        surah = verse(surahNumber, 1).surah,
        verses = numbers.map { verse(surahNumber, it) }
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getCollapsedSurahsUseCase() } returns emptySet()
        coEvery { setCollapsedSurahsUseCase(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads groups on init`() = runTest {
        val groups = listOf(group(1, 1, 2), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(groups)
        assertThat(state.filteredGroups).isEqualTo(groups)
    }

    @Test
    fun `emits error when load fails`() = runTest {
        coEvery { getSavedVersesUseCase(any()) } returns Result.failure(RuntimeException("boom"))

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(SavedVersesUiState.Error::class.java)
    }

    @Test
    fun `search filters groups by surah name and verse text`() = runTest {
        val groups = listOf(group(1, 1), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSearch("36"))
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.query).isEqualTo("36")
        assertThat(state.filteredGroups.map { it.surah.number }).containsExactly(36)
    }

    @Test
    fun `toggle collapse updates state and persists`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnToggleCollapse(1))
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.collapsedSurahs).containsExactly(1)
        coVerify { setCollapsedSurahsUseCase(setOf(1)) }
    }

    @Test
    fun `reorder groups persists the new group order`() = runTest {
        val groups = listOf(group(1, 1), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        val reordered = listOf(group(36, 1), group(1, 1))
        vm.onEvent(SavedVersesEvent.OnReorderGroups(reordered))
        advanceUntilIdle()

        coVerify { reorderSavedVersesUseCase(reordered) }
        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(reordered)
    }

    @Test
    fun `reorder within group persists the new verse order`() = runTest {
        val groups = listOf(group(1, 1, 2))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnReorderWithinGroup(1, listOf(verse(1, 2), verse(1, 1))))
        advanceUntilIdle()

        coVerify { reorderSavedVersesUseCase(listOf(group(1, 2, 1))) }
    }

    @Test
    fun `remove toggles the verse and reloads`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returnsMany listOf(
            Result.success(listOf(group(1, 1, 2))),
            Result.success(listOf(group(1, 2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1, 1)) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1, 1)))
        advanceUntilIdle()

        coVerify { toggleSavedVerseUseCase(verse(1, 1)) }
        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(listOf(group(1, 2)))
    }

    @Test
    fun `select opens the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1, 1)))

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isEqualTo(verse(1, 1))
        assertThat(state.isDetailVisible).isTrue()
    }

    @Test
    fun `dismiss closes the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1, 1)))
        vm.onEvent(SavedVersesEvent.OnDismissDetail)

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isNull()
        assertThat(state.isDetailVisible).isFalse()
    }

    private fun viewModel() = SavedVersesViewModel(
        getSavedVersesUseCase,
        reorderSavedVersesUseCase,
        toggleSavedVerseUseCase,
        getCollapsedSurahsUseCase,
        setCollapsedSurahsUseCase,
        languageProvider
    )
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesViewModelTest*"`
Expected: FAIL — new `SavedVersesViewModel` constructor and events don't exist yet.

- [ ] **Step 3: Update `SavedVersesUiState`**

```kotlin
package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup

sealed class SavedVersesUiState {
    data object Loading : SavedVersesUiState()
    data class Success(
        val groups: List<SavedVerseGroup>,
        val filteredGroups: List<SavedVerseGroup>,
        val collapsedSurahs: Set<Int>,
        val query: String = "",
        val selectedVerse: AyahData? = null,
        val isDetailVisible: Boolean = false
    ) : SavedVersesUiState()
    data class Error(val message: String) : SavedVersesUiState()
}
```

- [ ] **Step 4: Update `SavedVersesEvent`**

```kotlin
package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup

sealed class SavedVersesEvent {
    data class OnRemove(val verse: AyahData) : SavedVersesEvent()
    data class OnReorderGroups(val groups: List<SavedVerseGroup>) : SavedVersesEvent()
    data class OnReorderWithinGroup(val surahNumber: Int, val verses: List<AyahData>) : SavedVersesEvent()
    data class OnToggleCollapse(val surahNumber: Int) : SavedVersesEvent()
    data class OnSearch(val query: String) : SavedVersesEvent()
    data class OnSelect(val verse: AyahData) : SavedVersesEvent()
    data object OnDismissDetail : SavedVersesEvent()
}
```

- [ ] **Step 5: Rewrite `SavedVersesViewModel`**

```kotlin
package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.usecases.quran.GetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.SetCollapsedSurahsUseCase
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
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase,
    private val getCollapsedSurahsUseCase: GetCollapsedSurahsUseCase,
    private val setCollapsedSurahsUseCase: SetCollapsedSurahsUseCase,
    private val languageProvider: LanguageProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedVersesUiState>(SavedVersesUiState.Loading)
    val uiState: StateFlow<SavedVersesUiState> = _uiState.asStateFlow()

    init {
        loadSavedVerses()
    }

    fun onEvent(event: SavedVersesEvent) {
        when (event) {
            is SavedVersesEvent.OnRemove -> removeVerse(event.verse)
            is SavedVersesEvent.OnReorderGroups -> reorderGroups(event.groups)
            is SavedVersesEvent.OnReorderWithinGroup -> reorderWithinGroup(event.surahNumber, event.verses)
            is SavedVersesEvent.OnToggleCollapse -> toggleCollapse(event.surahNumber)
            is SavedVersesEvent.OnSearch -> search(event.query)
            is SavedVersesEvent.OnSelect -> selectVerse(event.verse)
            SavedVersesEvent.OnDismissDetail -> dismissDetail()
        }
    }

    fun reload() {
        loadSavedVerses()
    }

    private fun loadSavedVerses() {
        viewModelScope.launch {
            _uiState.value = SavedVersesUiState.Loading
            val language = languageProvider.getLanguageCode()
            val collapsed = getCollapsedSurahsUseCase()
            getSavedVersesUseCase(language)
                .onSuccess { groups ->
                    _uiState.value = SavedVersesUiState.Success(
                        groups = groups,
                        filteredGroups = filterGroups(groups, ""),
                        collapsedSurahs = collapsed
                    )
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
                    loadSavedVerses()
                }
        }
    }

    private fun reorderGroups(groups: List<SavedVerseGroup>) {
        viewModelScope.launch {
            reorderSavedVersesUseCase(groups)
                .onSuccess { updateGroups(groups) }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder groups -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun reorderWithinGroup(surahNumber: Int, verses: List<AyahData>) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val groups = current.groups.map { group ->
                if (group.surah.number == surahNumber) group.copy(verses = verses) else group
            }
            reorderSavedVersesUseCase(groups)
                .onSuccess { updateGroups(groups) }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder verses -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun toggleCollapse(surahNumber: Int) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val collapsed = if (surahNumber in current.collapsedSurahs) {
                current.collapsedSurahs - surahNumber
            } else {
                current.collapsedSurahs + surahNumber
            }
            _uiState.value = current.copy(collapsedSurahs = collapsed)
            setCollapsedSurahsUseCase(collapsed)
        }
    }

    private fun search(query: String) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(
            query = query,
            filteredGroups = filterGroups(current.groups, query)
        )
    }

    private fun updateGroups(groups: List<SavedVerseGroup>) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(
            groups = groups,
            filteredGroups = filterGroups(groups, current.query)
        )
    }

    private fun selectVerse(verse: AyahData) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = verse, isDetailVisible = true)
    }

    private fun dismissDetail() {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = null, isDetailVisible = false)
    }

    private fun filterGroups(groups: List<SavedVerseGroup>, query: String): List<SavedVerseGroup> {
        if (query.isBlank()) return groups
        val q = query.trim().lowercase()
        return groups.mapNotNull { group ->
            val surahMatches = group.surah.englishName.lowercase().contains(q) ||
                group.surah.name.lowercase().contains(q)
            val matchingVerses = group.verses.filter { it.text.lowercase().contains(q) }
            when {
                surahMatches && matchingVerses.isNotEmpty() -> group.copy(verses = matchingVerses)
                surahMatches -> group
                matchingVerses.isNotEmpty() -> group.copy(verses = matchingVerses)
                else -> null
            }
        }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesViewModelTest*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/SavedVersesUiState.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesEvent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModel.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesViewModelTest.kt
git commit -m "feat(home): group-aware saved verses view model with search and collapse"
```

---

## Task 5: `QuranVerseFormatter` `SurahInfo` overload + string resources

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseFormatter.kt`
- Modify: `prayer_feature/home/src/main/res/values/strings.xml`

- [ ] **Step 1: Add the `SurahInfo` overload**

In `QuranVerseFormatter.kt`, add an overload and delegate the existing one:

```kotlin
fun getLocalizedNameOf(quranVerse: AyahData, context: Context): String =
    getLocalizedNameOf(quranVerse.surah, context)

fun getLocalizedNameOf(surah: SurahInfo, context: Context): String {
    return try {
        getLocalizedSurahName(context, surah.number, surah.englishName)
    } catch (e: Exception) {
        Log.e("QuranVerseFormatter", "Surah ${surah.number} is got error with ${e.message}")
        surah.englishName
    }
}
```

Add import: `com.kutluoglu.prayer.model.quran.SurahInfo`.

- [ ] **Step 2: Add string resources**

Add to `strings.xml` (before `</resources>`):

```xml
<string name="search_saved_verses">Search saved verses...</string>
<string name="no_matching_verses">No saved verses match your search.</string>
<string name="clear">Clear</string>
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/common/QuranVerseFormatter.kt prayer_feature/home/src/main/res/values/strings.xml
git commit -m "feat(home): surah-info localization overload and saved-verses search strings"
```

---

## Task 6: Rewrite `SavedVersesScreen` — grouped list, collapse, search, chips, two-level reorder

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt` (new)

- [ ] **Step 1: Write failing screen tests**

Create `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
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

    private val formatter = QuranVerseFormatter()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Verse $surahNumber:$numberInSurah",
        surah = SurahInfo("Surah $surahNumber", "سورة", surahNumber, 10),
        numberInSurah = numberInSurah
    )

    private fun group(surahNumber: Int, vararg numbers: Int) = SavedVerseGroup(
        surah = verse(surahNumber, 1).surah,
        verses = numbers.map { verse(surahNumber, it) }
    )

    private fun setContent(state: SavedVersesUiState, onEvent: (SavedVersesEvent) -> Unit = {}) {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = state,
                verseFormatter = formatter,
                onNavigateBack = {},
                onEvent = onEvent
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders group headers and verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1, 2), group(36, 1)),
                filteredGroups = listOf(group(1, 1, 2), group(36, 1)),
                collapsedSurahs = emptySet()
            )
        )
        composeTestRule.onNodeWithText("Surah 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Surah 36").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 1:1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 36:1").assertIsDisplayed()
    }

    @Test
    fun `collapsed group hides its verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1, 2)),
                filteredGroups = listOf(group(1, 1, 2)),
                collapsedSurahs = setOf(1)
            )
        )
        composeTestRule.onNodeWithText("Surah 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 1:1").assertDoesNotExist()
    }

    @Test
    fun `search filters the displayed groups`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1), group(36, 1)),
                filteredGroups = listOf(group(36, 1)),
                collapsedSurahs = emptySet(),
                query = "36"
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Surah 36").assertIsDisplayed()
        composeTestRule.onNodeWithText("Surah 1").assertDoesNotExist()
    }

    @Test
    fun `typing in search emits OnSearch`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNode(hasSetTextAction()).performTextInput("36")
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnSearch("36"))
    }

    @Test
    fun `tapping a header emits OnToggleCollapse`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Surah 1").performClick()
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnToggleCollapse(1))
    }

    @Test
    fun `shows empty state when there are no saved verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = emptyList(),
                filteredGroups = emptyList(),
                collapsedSurahs = emptySet()
            )
        )
        composeTestRule.onNodeWithText("No saved verses yet. Bookmark a verse from the home screen.")
            .assertIsDisplayed()
    }

    @Test
    fun `shows no-matches state when search finds nothing`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = emptyList(),
                collapsedSurahs = emptySet(),
                query = "zzz"
            )
        )
        composeTestRule.onNodeWithText("No saved verses match your search.").assertIsDisplayed()
    }

    companion object {
        private fun assertThat(actual: Any?) = com.google.common.truth.Truth.assertThat(actual)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesScreenTest*"`
Expected: FAIL — the new screen signature/behavior isn't implemented yet.

- [ ] **Step 3: Rewrite `SavedVersesScreen`**

Replace the entire contents of `SavedVersesScreen.kt` with:

```kotlin
package com.kutluoglu.prayer_feature.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.EmptyStateContent
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.common.shareVerse
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface SavedRow {
    val key: String
    data class Header(val group: SavedVerseGroup) : SavedRow {
        override val key: String = "h-${group.surah.number}"
    }
    data class Verse(val group: SavedVerseGroup, val verse: AyahData) : SavedRow {
        override val key: String = "v-${group.surah.number}-${verse.numberInSurah}"
    }
}

private fun List<SavedVerseGroup>.toRows(): List<SavedRow> = flatMap { group ->
    listOf(SavedRow.Header(group)) + group.verses.map { SavedRow.Verse(group, it) }
}

private fun rowsToGroups(rows: List<SavedRow>): List<SavedVerseGroup> =
    rows.filterIsInstance<SavedRow.Header>().map { it.group }

private fun moveGroup(groups: List<SavedVerseGroup>, from: Int, to: Int): List<SavedVerseGroup> {
    val mutable = groups.toMutableList()
    val group = mutable.removeAt(from)
    mutable.add(to.coerceIn(0, mutable.size), group)
    return mutable
}

private fun AyahData.samePosition(other: AyahData): Boolean =
    surah.number == other.surah.number && numberInSurah == other.numberInSurah

@Composable
fun SavedVersesRoute(
    onNavigateBack: () -> Unit,
    viewModel: SavedVersesViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val rows = remember { mutableStateListOf<SavedRow>() }
    var pendingReorder by remember { mutableStateOf<SavedVersesEvent?>(null) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        val fromRow = rows[fromIndex]
        when (fromRow) {
            is SavedRow.Header -> {
                val fromGroupIndex = rows.take(fromIndex).count { it is SavedRow.Header }
                val toGroupIndex = rows.take(toIndex).count { it is SavedRow.Header }
                val newGroups = moveGroup(rowsToGroups(rows), fromGroupIndex, toGroupIndex)
                rows.clear()
                rows.addAll(newGroups.toRows())
                pendingReorder = SavedVersesEvent.OnReorderGroups(newGroups)
            }
            is SavedRow.Verse -> {
                val toRow = rows[toIndex]
                if (toRow !is SavedRow.Verse || toRow.group.surah.number != fromRow.group.surah.number) {
                    return@rememberReorderableLazyListState
                }
                val group = rowsToGroups(rows).first { it.surah.number == fromRow.group.surah.number }
                val fromVerseIndex = group.verses.indexOfFirst { it.samePosition(fromRow.verse) }
                val toVerseIndex = group.verses.indexOfFirst { it.samePosition(toRow.verse) }
                if (fromVerseIndex == -1 || toVerseIndex == -1) return@rememberReorderableLazyListState
                val newVerses = group.verses.toMutableList().apply {
                    add(toVerseIndex, removeAt(fromVerseIndex))
                }
                val newGroups = rowsToGroups(rows).map { g ->
                    if (g.surah.number == group.surah.number) g.copy(verses = newVerses) else g
                }
                rows.clear()
                rows.addAll(newGroups.toRows())
                pendingReorder = SavedVersesEvent.OnReorderWithinGroup(group.surah.number, newVerses)
            }
        }
    }

    LaunchedEffect(state) {
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        val success = state as? SavedVersesUiState.Success ?: return@LaunchedEffect
        val searching = success.query.isNotBlank()
        val target = success.filteredGroups.toRows().filterNot { row ->
            !searching && row is SavedRow.Verse && row.group.surah.number in success.collapsedSurahs
        }
        if (rows.toList() != target) {
            rows.clear()
            rows.addAll(target)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .distinctUntilChanged()
            .drop(1)
            .filter { !it }
            .collect {
                pendingReorder?.let { onEvent(it) }
                pendingReorder = null
            }
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
                if (state.groups.isEmpty()) {
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
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        SearchField(
                            query = state.query,
                            onQueryChange = { onEvent(SavedVersesEvent.OnSearch(it)) }
                        )
                        if (state.query.isBlank()) {
                            SurahJumpChips(
                                groups = state.filteredGroups,
                                verseFormatter = verseFormatter,
                                context = context,
                                onJump = { surahNumber ->
                                    val index = rows.indexOfFirst {
                                        it is SavedRow.Header && it.group.surah.number == surahNumber
                                    }
                                    if (index != -1) scope.launch { lazyListState.scrollToItem(index) }
                                }
                            )
                        }
                        if (state.filteredGroups.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateContent(
                                    icon = Icons.Default.Search,
                                    text = stringResource(R.string.no_matching_verses)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                rows.forEach { row ->
                                    when (row) {
                                        is SavedRow.Header -> item(key = row.key) {
                                            ReorderableItem(state = reorderableState, key = row.key) { isDragging ->
                                                SurahHeader(
                                                    group = row.group,
                                                    isCollapsed = state.query.isBlank() &&
                                                        row.group.surah.number in state.collapsedSurahs,
                                                    verseFormatter = verseFormatter,
                                                    context = context,
                                                    onToggle = {
                                                        onEvent(SavedVersesEvent.OnToggleCollapse(row.group.surah.number))
                                                    },
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
                                        is SavedRow.Verse -> item(key = row.key) {
                                            ReorderableItem(state = reorderableState, key = row.key) { isDragging ->
                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { value ->
                                                        if (value != SwipeToDismissBoxValue.Settled) {
                                                            rows.remove(row)
                                                            onEvent(SavedVersesEvent.OnRemove(row.verse))
                                                        }
                                                        true
                                                    }
                                                )
                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    enableDismissFromStartToEnd = true,
                                                    enableDismissFromEndToStart = false,
                                                    backgroundContent = {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 16.dp, vertical = 4.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(
                                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = stringResource(R.string.delete),
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    VerseRow(
                                                        verse = row.verse,
                                                        verseFormatter = verseFormatter,
                                                        isDragging = isDragging,
                                                        onSelect = { onEvent(SavedVersesEvent.OnSelect(row.verse)) },
                                                        onShare = { shareVerse(row.verse, verseFormatter, context) },
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_saved_verses)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            }
        } else {
            null
        },
        singleLine = true
    )
}

@Composable
private fun SurahJumpChips(
    groups: List<SavedVerseGroup>,
    verseFormatter: QuranVerseFormatter,
    context: Context,
    onJump: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groups, key = { it.surah.number }) { group ->
            FilterChip(
                selected = false,
                onClick = { onJump(group.surah.number) },
                label = { Text(verseFormatter.getLocalizedNameOf(group.surah, context)) }
            )
        }
    }
}

@Composable
private fun SurahHeader(
    group: SavedVerseGroup,
    isCollapsed: Boolean,
    verseFormatter: QuranVerseFormatter,
    context: Context,
    onToggle: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = verseFormatter.getLocalizedNameOf(group.surah, context),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${group.verses.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        dragHandle()
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

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="*SavedVersesScreenTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt
git commit -m "feat(home): grouped saved-verses screen with collapse, search, chips and reorder"
```

---

## Task 7: Update E2E / wipe tests + full regression

**Files:**
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesEndToEndTest.kt`
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenWipeTest.kt`

- [ ] **Step 1: Update `SavedVersesEndToEndTest`**

The `SavedVersesViewModel` constructor now takes 6 args. Update the construction and the state assertion:

```kotlin
val getCollapsedSurahsUseCase: GetCollapsedSurahsUseCase = GlobalContext.get().get()
val setCollapsedSurahsUseCase: SetCollapsedSurahsUseCase = GlobalContext.get().get()
val vm = SavedVersesViewModel(
    getSavedVersesUseCase,
    reorderSavedVersesUseCase,
    toggleSavedVerseUseCase,
    getCollapsedSurahsUseCase,
    setCollapsedSurahsUseCase,
    languageProvider
)
```

Replace the assertion:

```kotlin
val state = vm.uiState.value as SavedVersesUiState.Success
assertThat(state.groups).hasSize(1)
assertThat(state.groups[0].verses).contains(verse)
```

Add imports: `com.kutluoglu.prayer.usecases.quran.GetCollapsedSurahsUseCase`, `com.kutluoglu.prayer.usecases.quran.SetCollapsedSurahsUseCase`.

- [ ] **Step 2: Update `SavedVersesScreenWipeTest`**

Same constructor change (6 args) as above. Add the two imports.

- [ ] **Step 3: Run the full home module test suite**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all modules).

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Verify change scope**

Run: `gitnexus_detect_changes()`
Expected: only saved-verses related symbols and flows are affected.

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesEndToEndTest.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenWipeTest.kt
git commit -m "test(home): update saved-verses e2e and wipe tests for grouped model"
```

---

## Self-Review Notes

- **Spec coverage:** grouping (Tasks 1-2), nested storage + migration (Task 2), repository/use cases (Task 3), thin ViewModel with search/collapse/reorder (Task 4), grouped screen with collapsible headers + search + jump chips + two-level reorder (Task 6), collapse persistence (Task 2/4), tests (all tasks), E2E/wipe updates + regression (Task 7). All spec sections covered.
- **Deviations flagged:** non-sticky headers (reorderable library constraint); search matches englishName/Arabic name/text instead of localized name (no Context in ViewModel).
- **Type consistency:** `SavedVerseGroup`, `filteredGroups`, `OnReorderGroups`, `OnReorderWithinGroup`, `OnToggleCollapse`, `OnSearch`, `getCollapsedSurahs`/`setCollapsedSurahs` are used consistently across tasks.
