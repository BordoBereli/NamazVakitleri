# Design: Highlight Current Prayer on Android Auto Screens

**Date:** 2026-10-08
**Status:** Approved

## Problem

The Android Auto screens do not visually distinguish the *current* prayer.
`PrayerTimesScreen` marks only the next prayer with a "▶" prefix;
`NextPrayerScreen` shows only the next prayer and never displays the current one.

## Goal

Highlight the current prayer on both Auto screens using the Car App library's
supported styling: a text marker plus a `ForegroundCarColorSpan` (limited
palette — arbitrary colors are not supported on `Row` titles/texts).

## Decisions (user-approved)

- **Highlight style:** marker + color (both).
- **Marker:** `●` (filled circle) — visually distinct from the "▶" next-prayer cue.
- **Color:** `CarColor.PRIMARY` via `ForegroundCarColorSpan.create(...)` on the
  current prayer's time text.
- **Scope:** both Auto screens.

## Design

### Domain (small extension)

- Add `currentPrayerName: String? = null` to `SurfacePrayerData` (mirrors
  `nextPrayerName`). Defaulted field → existing constructor sites keep compiling.
- `PrayerSurfaceDataProvider.load()` populates it from `result.currentPrayer?.name`.

### PrayerTimesScreen

- The row whose prayer matches the current prayer (name == `currentPrayerName`,
  date == today) gets:
  - Title: `● <name>`
  - Time text: `CarText` with `ForegroundCarColorSpan.create(CarColor.PRIMARY)`
    over the whole time string.
- Next prayer keeps `▶`; the tomorrow-Imsak row is unchanged.

### NextPrayerScreen

- New row added to the pane **above** the next-prayer row:
  - Title: `● <currentPrayerName>`
  - Time text with PRIMARY color span.
- Rendered only when `currentPrayerName`/`currentPrayerLocalTime` are non-null
  (graceful skip otherwise).
- Next-prayer title, countdown, actions, and location line unchanged.

## Error Handling

If `currentPrayerName` is null (e.g. empty prayers list edge), no highlight and
no extra row — screens render exactly as today.

## Testing (TDD)

- Domain: provider populates `currentPrayerName` (1 new test in
  `PrayerSurfaceDataProviderTest`).
- `PrayerTimesScreenTest`: current row has `●` marker + span with
  `CarColor.PRIMARY` on its time; other rows unaffected.
- `NextPrayerScreenTest`: pane contains the current-prayer row with marker +
  colored time; absent when data lacks current prayer.

## Cost

~3 production files + 2 test files; additive only, no behavior change for
existing rows.
