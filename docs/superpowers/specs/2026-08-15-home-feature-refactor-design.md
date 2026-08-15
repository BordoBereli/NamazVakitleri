# Home Feature Refactor — Design

**Date:** 2026-08-15
**Status:** Approved
**Scope:** Whole home feature (`prayer_feature/home`)
**Goal:** Maintainability first — decompose monoliths, extract testable pure logic, fix hot-path performance smells. Preserve current visual/interaction behavior (open to minor UX tweaks only where they reduce complexity).

## Background

Commit `4f062fc` added pager↔chips scroll sync to the home screen. It grew `LocationChipsRow.kt` to 241 lines and `HomeScreen.kt` to 380 lines, mixing four responsibilities into the chips row and duplicating the responsive layout between `PrayerContent` and `LocationPagePreview`. The new pure math (`selectionProgressFor`, indicator interpolation, chip centering) has no unit tests.

## Pain Points

1. **`LocationChipsRow.kt` (241 lines)** — mixes chip rendering, sliding indicator geometry, pager scroll sync, and color interpolation. Indicator math is inline in composition and untestable.
2. **`HomeScreen.kt` (380 lines)** — pager setup, scroll-settle selection, and `LandscapeMode`/`PortraitMode` layout are duplicated between `PrayerContent` and `LocationPagePreview`.
3. **Untested pure logic** — `selectionProgressFor`, indicator interpolation, chip centering have zero coverage.
4. **Perf smells** — `onGloballyPositioned` writes to `mutableStateMapOf` on every layout pass; geometry recomputed on every chip recomposition; `animateScrollBy` in a `snapshotFlow` collector can fight user gestures on the chips row.

## Approach

**Approach A — Extract pure logic + split components.** Moderate churn (~6 new files), each unit small, single-purpose, testable. Pure logic extracted first and tested, minimizing risk. (Approach B — a custom `LocationPagerState` holder — was considered and kept in mind as a fallback if the pure-function extraction proves awkward, but is over-engineering for this scale.)

## Design

### 1. Pure logic extraction — `ChipsSelectionGeometry`

**New file:** `components/ChipsSelectionGeometry.kt` — pure, no Compose state, fully unit-testable.

```kotlin
data class ChipBounds(val offsetX: Float, val width: Float, val height: Float)
data class IndicatorBounds(val offsetX: Float, val width: Float, val height: Float)

object ChipsSelectionGeometry {
    fun selectionProgressFor(index, currentPage, currentPageOffsetFraction): Float
    fun indicatorBoundsFor(currentPage, currentPageOffsetFraction, chipBounds, lastIndex): IndicatorBounds?
    fun chipScrollTargetFor(page, layoutInfo): Float?   // centering delta
}
```

- `selectionProgressFor` — moved verbatim from `LocationChipsRow.kt:53`.
- `indicatorBoundsFor` — the `fromIndex`/`toIndex`/`fraction`/`targetOffsetX`/`targetWidth` math currently inline at `LocationChipsRow.kt:88-109`.
- `chipScrollTargetFor` — the centering delta at `LocationChipsRow.kt:125`.

### 2. Component split — decompose `LocationChipsRow`

**`LocationChipsRow.kt`** (slimmed to orchestration only, ~90 lines):
- Holds `listState`, tracks container coords + chip bounds.
- Computes geometry via `ChipsSelectionGeometry`.
- Renders `SlidingChipIndicator` + `LazyRow` of `LocationChip`s + `AddLocationChip`.
- Keeps the scroll-sync `LaunchedEffect` (pager → chips centering).

**New file:** `components/SlidingChipIndicator.kt` — the animated indicator `Box` (currently `LocationChipsRow.kt:140-150`). Takes `indicatorBounds: IndicatorBounds?` + the two `Animatable`s, renders the rounded pill.

**New file:** `components/LocationChip.kt` — extracted verbatim from `LocationChipsRow.kt:182-222` (text, GPS icon, color interpolation).

**New file:** `components/AddLocationChip.kt` — extracted verbatim from `LocationChipsRow.kt:224-241`.

### 3. HomeScreen decomposition

**New file:** `LocationPager.kt` (root package) — extracts the pager + chips + scroll-settle logic from `HomeScreen.kt:72-141`:
- `rememberPagerState` + the `LaunchedEffect(selectedId)` animate-to-page.
- The scroll-settle `snapshotFlow` (deferred `OnLocationSelected`).
- Renders `LocationChipsRow` + `HorizontalPager` with `beyondViewportPageCount = 1`.
- Takes `entries`, `selectedId`, `activeLocationId`, `prayerDataByLocation`, `uiState`, and callbacks.

**New file:** `layout/HomeResponsiveLayout.kt` — extracts the duplicated `BoxWithConstraints` → `isLandscape` → `CompositionLocalProvider(LocalIsLandscape)` → `LandscapeMode`/`PortraitMode` dispatch that appears in both `PrayerContent` (`HomeScreen.kt:235-292`) and `LocationPagePreview` (`HomeScreen.kt:170-200`).

```kotlin
@Composable
fun HomeResponsiveLayout(
    innerPadding: PaddingValues,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
)
```

`LandscapeMode`/`PortraitMode` move into `layout/` as private helpers. Both `PrayerContent` and `LocationPagePreview` now call `HomeResponsiveLayout`.

**New file:** `components/HomeErrorContent.kt` — extracts `ErrorContent` (`HomeScreen.kt:357-380`).

**`HomeScreen.kt`** slims from 380 → ~120 lines: `PermissionHandler` wrapper + `LocationPager` + `PrayerContent` (which uses `HomeResponsiveLayout`).

### 4. Performance fixes

Targeted, conservative — no behavior change, just eliminating hot-path waste:

1. **Bounds writes** (`LocationChipsRow.kt:164-172`): guard each `onGloballyPositioned` write — only update the map when `offsetX`/`width`/`height` actually changed. Prevents redundant recomposition churn.
2. **Geometry recomputation**: wrap `fromBounds`/`toBounds`/`targetOffsetX`/`targetWidth` derivation in `derivedStateOf` so it only recomputes when the pager offset or bounds actually change.
3. **Indicator animation**: keep `snapTo` for scroll-tracking (correct — tracks the finger without lag). No change beyond the `derivedStateOf` above.
4. **Scroll-sync gesture conflict** (`LocationChipsRow.kt:117-131`): skip the centering scroll while `listState.isScrollInProgress` (user actively dragging chips). Pager-driven changes still center correctly.

### 5. Testing

**New test file:** `components/ChipsSelectionGeometryTest.kt` — unit tests for the extracted pure logic:
- `selectionProgressFor` — current page (1→0 as you swipe away), next page (0→1 swiping forward), prev page (0→1 swiping back), non-adjacent pages (always 0).
- `indicatorBoundsFor` — interpolation between from/to bounds, clamping at edges (first/last chip), null when no bounds.
- `chipScrollTargetFor` — centering delta, null when chip not visible.

**Existing tests:** `HomeViewModelTest`, `HomeUiStateMergerTest`, domain tests — must all stay green (no ViewModel/domain changes in this refactor).

**Verification:** `./gradlew :prayer_feature:home:testDebugUnitTest` + full `./gradlew testDebugUnitTest` before commit.

## Out of Scope

- `HomeViewModel`, `HomeEvent`, `HomeRoute`, `HomeGraph` — unchanged.
- `PrayerCard`, `DailyPrayers`, `HomeTopContainer`, `BottomContainer` — unchanged.
- Domain loaders (`PrayerTimesLoader`, `CountdownEngine`, `QuranVerseLoader`, `LocationCoordinator`) — unchanged.
- State files (`HomeUiStates`, `HomeScreenGate`, `HomeUiStateMerger`, `CountdownUiState`, `QuranUiState`) — unchanged.
- No UX changes beyond the scroll-sync gesture guard (which preserves behavior).

## Success Criteria

- All existing tests pass.
- New `ChipsSelectionGeometryTest` covers the extracted math.
- `LocationChipsRow.kt` < ~100 lines; `HomeScreen.kt` < ~150 lines.
- No behavior change: pager↔chips sync, indicator, color interpolation, and scroll-settle selection all work identically.
