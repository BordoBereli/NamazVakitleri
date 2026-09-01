# Saved Verses Redesign — Surah Grouping Design

**Status:** Approved 2026-09-01
**Branch:** none yet (off `main`)

## Problem

The Saved Verses screen is a single flat, reorderable `LazyColumn` of all bookmarked verses
(`SavedVersesScreen.kt`). As the collection grows past one screenful it becomes hard to use:

- **No scannability** — a long wall of verse text is hard to skim; finding a specific verse means
  scrolling everything.
- **No grouping** — verses from the same surah are scattered unless manually reordered.
- **Global reorder is impractical** — drag-to-reorder across 50+ items is unwieldy.
- **No search** — no way to jump to a surah or find by text.

## Goal

Redesign the Saved Verses screen so it scales to large collections:

1. Group saved verses by surah, with collapsible (sticky) surah headers.
2. Two-level reorder: reorder surah groups, and reorder verses within a surah.
3. Search within saved verses (by surah name or verse text).
4. Surah jump chips that scroll to a group (horizontal scrollable row + edge fade).
5. Persist collapse state across restarts.

## Scope

- **In scope:**
  - `:prayer:model` — new `SavedVerseGroup` data class
  - `:prayer:data` — `SavedVersesStore` nested storage + migration; `QuranRepository` group API
  - `:prayer:domain` — `IQuranRepository` group API; `GetSavedVersesUseCase` /
    `ReorderSavedVersesUseCase` signatures
  - `:prayer_feature:home` — `SavedVersesViewModel` / `SavedVersesUiState` / `SavedVersesEvent`;
    grouped `SavedVersesScreen` (sticky collapsible headers, search, jump chips, two-level reorder);
    collapse-state persistence
  - Tests for all of the above
- **Out of scope:** bundled offline verses, API provider change, changing the save/bookmark toggle
  itself, bottom-nav changes, verse detail sheet redesign.

## Decisions

1. **Nested storage model (Approach 2).** Saved verses are persisted as `List<SavedVerseGroup>`
   (surah + ordered verses) instead of a flat `List<AyahData>`. The domain model matches reality,
   reorder semantics are natural, and the ViewModel stays thin. The app is still in development
   (no production users), so the one-time migration is free to run now.
2. **Single grouped list + jump index (Option A).** One scrollable list with sticky surah headers,
   a search bar, and a horizontal scrollable row of surah jump chips with an edge fade. Rejected
   the two-level (surah list → verses) navigation as adding a step.
3. **Collapsible surah groups.** Tapping a surah header toggles collapse/expand. Collapse state is
   persisted (set of collapsed surah numbers) so it survives restarts.
4. **Two-level reorder.** Dragging a surah header reorders the group (moves its block); dragging a
   verse handle reorders within the surah. Both funnel into `ReorderSavedVersesUseCase` with the
   full nested list.
5. **Search filters groups live.** A group matches if the localized surah name or any verse text
   contains the query (case-insensitive). While searching, jump chips hide and collapsed groups
   auto-expand so matches are visible. Empty results show a distinct "no matches" state.
6. **Jump chips overflow = horizontal scroll + edge fade.** Chips scroll sideways with a fade on
   the right edge signaling more. Rejected FlowRow wrap (pushes list down) and a single "Surahs"
   dropdown sheet (extra step) for the default case.
7. **`SurahInfo` stored explicitly in each group** (not derived from the first verse) to keep
   header rendering simple and avoid empty-group edge cases.

## Target Architecture

### `:prayer:model` — new data class

```kotlin
@Serializable
data class SavedVerseGroup(
    val surah: SurahInfo,        // name, englishName, number, numberOfAyahs
    val verses: List<AyahData>   // ordered within the surah
)
```

### `:prayer:data` — `SavedVersesStore`

- New preference key `saved_verse_groups` storing `List<SavedVerseGroup>` JSON.
- **Migration:** read the old flat `saved_verses` list, group by surah number preserving order,
  write nested, delete the old key. Pure function, easily tested.
- New methods:
  - `getSavedVerseGroups(): List<SavedVerseGroup>`
  - `saveGroups(groups: List<SavedVerseGroup>)`
  - `toggle(verse: AyahData)` — adds to the right group (or removes); new saves prepend within
    their surah group
  - `isSaved(verse: AyahData): Boolean`

### `:prayer:data` — `QuranRepository`

```kotlin
override suspend fun getSavedVerses(language: String): Result<List<SavedVerseGroup>> =
    runCatching {
        savedVersesStore.getSavedVerseGroups().map { group ->
            group.copy(verses = group.verses.map { verse ->
                getVerse(verse.surah.number, verse.numberInSurah, language).getOrElse { verse }
            })
        }
    }

override suspend fun reorderSavedVerses(groups: List<SavedVerseGroup>): Result<Unit> =
    runCatching { savedVersesStore.saveGroups(groups) }
```

### `:prayer:domain` — `IQuranRepository`

```kotlin
suspend fun getSavedVerses(language: String): Result<List<SavedVerseGroup>>
suspend fun reorderSavedVerses(groups: List<SavedVerseGroup>): Result<Unit>
```

`GetSavedVersesUseCase` / `ReorderSavedVersesUseCase` update their signatures accordingly;
`ToggleSavedVerseUseCase` / `isVerseSaved` are unchanged.

### `:prayer_feature:home` — ViewModel & UI state

```kotlin
sealed class SavedVersesUiState {
    data object Loading : SavedVersesUiState()
    data class Success(
        val groups: List<SavedVerseGroup>,
        val collapsedSurahs: Set<Int>,
        val query: String = "",
        val selectedVerse: AyahData? = null,
        val isDetailVisible: Boolean = false
    ) : SavedVersesUiState()
    data class Error(val message: String) : SavedVersesUiState()
}
```

`SavedVersesViewModel` stays thin — it loads groups, applies the search filter, toggles collapse
(persisted), and forwards reorder/remove/select events. The screen only renders state and emits
events; no grouping or filtering logic in composables.

### `:prayer_feature:home` — Screen

- `TopAppBar` (back arrow + title) + search field below.
- Horizontal scrollable row of surah jump chips (edge fade), hidden while searching; tapping a chip
  scrolls to that group's header.
- `LazyColumn` with sticky surah headers (tappable: collapse chevron, localized name, verse count,
  drag handle for group reorder) and verse rows (text, "Surah - n:m" ref, share, swipe-to-delete,
  drag handle for within-group reorder).
- Collapsed group renders header only.
- Empty state (no saved verses) and "no matches" (search) states via `EmptyStateContent`.
- Tap verse → existing `VerseDetailSheetContent` bottom sheet.

## Error Handling

- Load failure → `SavedVersesUiState.Error` with message; retry on re-entry.
- Reorder / delete / toggle failure → logged; UI state unchanged (consistent with
  `QuranVerseLoader`).
- Corrupt saved-verses JSON → `runCatching` returns empty list (existing behavior).
- Migration failure → fall back to empty list; old key left intact for retry.

## Testing

1. `SavedVersesStoreTest`: migration (flat→nested grouping, order preserved); toggle add/remove
   across groups; `saveGroups` round-trip.
2. `QuranRepositoryTest`: `getSavedVerses` returns re-localized groups; `reorderSavedVerses`
   persists nested list.
3. `SavedVersesViewModelTest` (MockK + Turbine + Truth): search filtering, collapse toggle,
   group reorder, within-group reorder, remove.
4. Robolectric screen test: renders groups, collapse/expand, search filters, jump chip scroll.
5. Update existing `SavedVersesEndToEndTest` / `SavedVersesScreenWipeTest` for the nested model.
6. Full regression: `./gradlew assembleDebug testDebugUnitTest`;
   `gitnexus_detect_changes()` shows only the expected symbols.

## Acceptance Criteria

- Saved verses appear grouped by surah with sticky, collapsible headers.
- Dragging a header reorders groups; dragging a verse handle reorders within a surah; both persist.
- Search filters by surah name and verse text; jump chips scroll to a group; collapse state
  persists across restarts.
- Existing flat saved-verses data migrates to the nested format on first load.
- All existing tests pass; home verse-of-the-day flow unchanged.
