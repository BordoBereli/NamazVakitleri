# Prayer Times Month Navigation — Design

Date: 2026-08-11
Status: Approved (Approach A)

## Problem

`PrayerTimesViewModel.loadMonthlyPrayerTimes()` only computes the **current month**.
The header in `PrayerContainer.kt` already renders left/right arrow icons
(`btn_left` / `btn_right`) but they are decorative — there is no way to view
another month.

## Goals

- Navigate to any past/future month via the existing header arrows.
- A "Today" button to jump back to the current month.
- Cache loaded months so revisiting a month is instant.
- Keep the current content visible while a new month loads (no flash).
- Today highlight/auto-scroll only applies when viewing the current month.

## Non-Goals

- Swipe navigation (HorizontalPager) — noted as a future improvement (option C).
- Changing the prayer-time calculation logic itself.

## Design (Approach A)

### 1. State & Events

New `PrayerTimesEvent` sealed class:

```kotlin
sealed class PrayerTimesEvent {
    data object OnPreviousMonth : PrayerTimesEvent()
    data object OnNextMonth : PrayerTimesEvent()
    data object OnToday : PrayerTimesEvent()
}
```

`PrayerTimesViewModel` gains:

- `selectedMonth: YearMonth` — currently displayed month (default: current month).
- `monthCache: MutableMap<YearMonth, List<DailyPrayer>>` — loaded months.
- `isLoading: Boolean` — true while a month is being computed (keeps current
  content visible).
- `fun onEvent(event: PrayerTimesEvent)` dispatcher.
- `loadMonthlyPrayerTimes()` remains the initial load entry point.

### 2. UiState

`PrayerTimesUiState.Success` gains:

- `selectedMonth: YearMonth` — drives the header label.
- `isCurrentMonth: Boolean` — whether the displayed month equals the current
  month; gates the today highlight, auto-scroll, and the "Today" button.

`currentDayOfMonth` stays, but today-highlighting only applies when
`isCurrentMonth` is true.

### 3. Data flow & caching

`loadMonthlyPrayerTimes()` delegates to `loadMonth(month: YearMonth)`:

- If `monthCache[month]` exists → set `Success` immediately (no recompute).
- Else → compute each day of the month via `getPrayerTimesUseCase`, store the
  result in `monthCache`, then set `Success`.
- Keep current content while loading: do not set `Loading`; only swap on success.
- `selectedMonth` updates immediately on navigation so the header label changes
  right away.
- `OnToday` → set `selectedMonth` to the current month and load if not cached.

### 4. UI changes

`PrayerContainer`:

- `TitleHeader` gains `onPrevious`, `onNext`, `onToday` callbacks plus
  `selectedMonthLabel` and `isCurrentMonth`:
  - Left arrow → `onPrevious`; right arrow → `onNext`.
  - A "Today" button appears when `!isCurrentMonth`.
  - Header label shows the selected month (e.g., "August 2026").
- `PrayerList` auto-scrolls to today only when `isCurrentMonth`.
- Today highlight: `isToday = dayOfMonth == currentDayOfMonth && isCurrentMonth`.

`PrayerTimesRoute`:

- `LaunchedEffect(Unit) { viewModel.loadMonthlyPrayerTimes() }` for the initial
  load.
- Pass `onEvent = viewModel::onEvent` through to the screen.

`PayerTimesScreen`:

- Thread `onEvent` through to `PrayerContainer`.

### 5. Testing

New `PrayerTimesViewModelTest` (JUnit 5 + MockK + Turbine + runTest). Test deps
must be added to `prayer_feature:prayertimes/build.gradle.kts`.

Cases:

- Initial load computes the current month (28–31 days).
- `OnNextMonth` / `OnPreviousMonth` load adjacent months.
- `OnToday` returns to the current month.
- Caching: revisiting a month does not recompute (verify use-case call count).
- `isCurrentMonth` is correct for current vs other months.
- Today highlight only applies in the current month.

## Future (option C)

Swap the `LazyColumn` for a `HorizontalPager` keyed by month, reusing the same
ViewModel state and cache. Not part of this change.
