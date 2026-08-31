# Saved Verses Screen Design

**Status:** Approved 2026-08-31
**Branch:** none yet (new feature off `main`)

## Problem

Users can bookmark Quran verses (save/unsave) from the verse detail sheet on the Home screen, and
saved verses persist locally in `SavedVersesStore`. But there is **no UI to view, manage, or
revisit** those saved verses — the repository doesn't even expose `getSavedVerses()`.

## Goal

Add a secondary "Saved Verses" screen where users can:

1. View all bookmarked verses (most recently saved first).
2. Tap a verse to open the existing detail bottom sheet (share / unsave).
3. Swipe to delete a verse.
4. Share a verse directly from the list.
5. Drag-and-drop to reorder the list (persisted).

## Scope

- **In scope:**
  - `:prayer_navigation:core` — new `SavedVersesScreen` route
  - `:prayer:data` — `SavedVersesStore.reorder()`; `QuranRepository.getSavedVerses()` /
    `reorderSavedVerses()`
  - `:prayer:domain` — `IQuranRepository` gains `getSavedVerses` / `reorderSavedVerses`; new
    `GetSavedVersesUseCase` / `ReorderSavedVersesUseCase`
  - `:prayer_feature:home` — bookmark icon in Home top bar; new `SavedVersesScreen` +
    `SavedVersesViewModel` + `SavedVersesUiState` + `SavedVersesEvent`; verse row component;
    reuse `VerseDetailSheetContent`; register route in `HomeGraph`
  - Tests for all of the above
- **Out of scope:** bundled offline verses, API provider change, changing the save/bookmark toggle
  itself, bottom-nav changes.

## Decisions

1. **Secondary screen in the Home graph.** `SavedVersesScreen` is a sub-screen of the Home nested
   graph (same pattern as Settings → My Locations). The bookmark icon in the Home top bar
   navigates to it; the back arrow pops back to Home. No new bottom-nav tab.
2. **Entry point: bookmark icon in Home top bar.** Always visible on Home, one tap from where
   verses are discovered.
3. **Mirror `MyLocationsScreen`.** Reuse the proven pattern: `LazyColumn` +
   `sh.calvin.reorderable` (`ReorderableItem` + drag handle), `SwipeToDismissBox` for delete,
   `EmptyStateContent` for the empty state, `TopAppBar` with back arrow.
4. **Reuse `VerseDetailSheetContent`.** Tapping a saved verse opens the existing detail bottom
   sheet (share + unsave), so behavior is consistent with the verse-of-the-day.
5. **Order = save order (newest first).** `SavedVersesStore.toggle()` **prepends** new saves to
   the front of the JSON list, so the store order is already newest-first. The screen displays the
   store order directly (no reversal), and reordering rewrites the persisted list in the displayed
   order. This keeps display order and persisted order identical, so a reload after reorder is
   stable.
6. **Data via use cases.** `GetSavedVersesUseCase` / `ReorderSavedVersesUseCase` follow the
   existing use-case pattern; the ViewModel exposes a sealed `SavedVersesUiState`.

## Target Architecture

### `:prayer_navigation:core`

```kotlin
sealed class Screen(val route: String) {
    // ...
    data object SavedVersesScreen : Screen("saved_verses")
}
```

### `:prayer:data` — `SavedVersesStore`

`toggle` prepends new saves (newest first); `reorder` rewrites the whole list:

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

### `:prayer:data` — `QuranRepository`

```kotlin
override suspend fun getSavedVerses(): Result<List<AyahData>> =
    runCatching { savedVersesStore.getSavedVerses() }

override suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit> =
    runCatching { savedVersesStore.reorder(verses) }
```

### `:prayer:domain` — `IQuranRepository`

```kotlin
suspend fun getSavedVerses(): Result<List<AyahData>>
suspend fun reorderSavedVerses(verses: List<AyahData>): Result<Unit>
```

New use cases: `GetSavedVersesUseCase`, `ReorderSavedVersesUseCase` (same pattern as
`ToggleSavedVerseUseCase`).

### `:prayer_feature:home` — ViewModel

```kotlin
@KoinViewModel
class SavedVersesViewModel(
    private val getSavedVersesUseCase: GetSavedVersesUseCase,
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase,
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase
) : ViewModel() {
    // SavedVersesUiState (Loading / Success(verses) / Error)
    // SavedVersesEvent (OnRemove, OnReorder, OnSelect, OnShare)
}
```

### `:prayer_feature:home` — Screen

- `SavedVersesScreen` with `TopAppBar` (back arrow + title), `LazyColumn` of `VerseRow` cards.
- Each row: verse text preview, surah/ayah info, share icon, drag handle; swipe-to-dismiss for
  delete.
- Empty state via `EmptyStateContent`.
- Tap → `VerseDetailSheetContent` bottom sheet.

### `:prayer_feature:home` — Navigation

- `HomeGraph.kt` adds `composable(Screen.SavedVersesScreen.route) { SavedVersesRoute(...) }`.
- Home top bar bookmark icon navigates to `Screen.SavedVersesScreen.route`.

## Error Handling

- Load failure → `SavedVersesUiState.Error` with message; retry on re-entry.
- Reorder / delete / toggle failure → logged; UI state unchanged (consistent with
  `QuranVerseLoader`).
- Corrupt saved-verses JSON → `runCatching` returns empty list (existing behavior).

## Testing

1. `SavedVersesStoreTest`: reorder persists new order; round-trip.
2. `QuranRepositoryTest`: `getSavedVerses` returns store list; `reorderSavedVerses` persists.
3. `SavedVersesViewModelTest` (MockK + Turbine + Truth): load success/error; remove; reorder;
   toggle-saved reflects in list.
4. Robolectric screen test: renders list, empty state, swipe-to-delete, reorder.
5. Full regression: `./gradlew assembleDebug testDebugUnitTest`;
   `gitnexus_detect_changes()` shows only the expected symbols.

## Acceptance Criteria

- Bookmark icon on Home opens the Saved Verses screen.
- Saved verses appear newest-first; tapping opens the detail sheet; swipe deletes; drag reorders;
  share works from the row.
- All changes persist across app restarts.
- All existing tests pass; home verse-of-the-day flow unchanged.
