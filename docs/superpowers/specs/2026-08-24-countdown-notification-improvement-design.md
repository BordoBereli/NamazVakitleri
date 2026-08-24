# Persistent Countdown Notification Improvement

Date: 2026-08-24

## Overview

The persistent countdown notification (built as part of the notifications delivery wiring, 2026-08-24) currently shows terse, low-information content: title `Next prayer: Maghrib`, body `1h 30m`. It does not show the actual prayer time, and it disappears after the last prayer of the day (Isha) until the next day's schedule is built.

This spec makes the notification more meaningful: it shows the next prayer name **and** its clock time, a clearer countdown message, and a progress bar indicating how much of the gap between the previous and next prayer has elapsed. It also keeps the countdown alive overnight, targeting tomorrow's Fajr after Isha.

## Problem

- Wording is terse and not meaningful: `Next prayer: Maghrib` + `1h 30m` gives no sense of *when* the prayer is.
- The actual prayer clock time is not shown anywhere in the notification.
- After Isha (last prayer of the day), the countdown simply disappears until the daily reschedule worker rebuilds tomorrow's schedule — no "next: Fajr tomorrow" state.
- No sense of progress through the day's prayer cycle.

## Goal

- Show the next prayer name and its clock time in the notification title (e.g. `Maghrib · 18:45`).
- Show a meaningful countdown message in the body (e.g. `1h 30m remaining`).
- Show a progress bar representing elapsed time between the previous prayer and the next prayer.
- Keep the countdown alive overnight after Isha, targeting tomorrow's Fajr (e.g. `Fajr · 05:12`, `7h 23m remaining`).
- Preserve the existing 60-second refresh cadence (no seconds display) and the `Stop` action.

## Approach

Extend the existing single-scheduler design. The progress bar requires the *previous* prayer time, which is not currently tracked — thread it through `ScheduledAlarm` → `PrayerNotificationScheduler.scheduleAlarm` → `AlarmReceiver` → `updateCountdown` → `showCountdownNotification`, and carry it through the self-re-arming countdown tick intent so the bar keeps advancing. The overnight target is handled by pointing the last enabled prayer's `nextPrayer*` at tomorrow's Fajr.

## Design

### Notification content

```
Maghrib · 18:45          ← title: localized prayer name + clock time (derived from targetMillis in device timezone)
1h 30m remaining         ← body: localized countdown
[████████░░░░░░░░░░]     ← progress bar: elapsed between previous & next prayer
```

- **Overnight:** after Isha, shows `Fajr · 05:12` with the long countdown (e.g. `7h 23m remaining`), progress bar filling across the Isha→Fajr gap.
- **`Stop` action** stays as-is (cancels the countdown notification + tick chain).
- Clock time is derived from the target epoch millis using the device timezone — no new data needed for the time itself.

### Data flow changes

The progress bar gap starts at the *previous* prayer relative to the countdown target. This differs by start path:

- **At schedule time** the countdown targets the first upcoming prayer X; the gap starts at the prayer before X (X-1).
- **At fire time** prayer P fires and the countdown transitions to P's next prayer N; the gap starts at P itself (the prayer that just fired).

1. **`ScheduledAlarm`** — add `previousPrayerTimeMillis: Long?` = the trigger time of the prayer immediately before this one (P-1). Null for the first enabled prayer. Used at schedule time to start the progress bar from the last prayer that already happened.
2. **`SchedulePlan.buildDailyAlarms(...)`** — new parameter `nextDayFajrTimeMillis: Long?`. For each enabled prayer, set `previousPrayerTimeMillis` to the previous enabled prayer's trigger. For the **last** enabled prayer, set `nextPrayerTimeMillis` / `nextPrayerName` to **tomorrow's Fajr** (when `nextDayFajrTimeMillis` is provided).
3. **`PrayerNotificationScheduler.scheduleAlarm`** — put `triggerAtMillis` in the intent extras as `EXTRA_ALARM_TRIGGER_TIME` (the firing prayer's trigger = the start of the next countdown gap).
4. **`AlarmReceiver`** — on PRAYER, read `EXTRA_ALARM_TRIGGER_TIME` as the previous time and pass it to `updateCountdown(nextTime, nextName, previous)`.
5. **`PrayerNotificationScheduler.updateCountdown(targetMillis, prayerName, previousTimeMillis)`** — carry `previousTimeMillis` through the 60s countdown tick intent (`EXTRA_COUNTDOWN_PREVIOUS_TIME`) so the progress bar keeps advancing on each tick.
6. **`PrayerNotificationManager.showCountdownNotification(nextPrayerName, nextPrayerTimeMillis, previousPrayerTimeMillis, remainingMillis)`** — build title, body, and progress bar.

**Schedule-time countdown start** (`scheduleAllSuspending`): if the first upcoming prayer X exists, call `updateCountdown(X.triggerAtMillis, X.prayerKey, X.previousPrayerTimeMillis)`. If no prayer is upcoming today (all passed) and `nextDayFajrTimeMillis` is available, call `updateCountdown(nextDayFajrTimeMillis, "Fajr", lastEnabledPrayerTrigger)` so the countdown stays alive overnight.

### Progress bar

- `NotificationCompat.Builder.setProgress(max, progress, false)`.
- `max = nextPrayerTimeMillis - previousPrayerTimeMillis`; `progress = now - previousPrayerTimeMillis`.
- Overnight: previous = today's Isha, next = tomorrow's Fajr — works with the same formula.
- When `previousPrayerTimeMillis` is null (e.g. countdown started at the first prayer of the day with no prior prayer), omit the progress bar.

### Strings (all 15 locales: values, ar, ta, fa, es, ur, th, tr, fr, de, ru, bn, hi, id, ms)

- `notification_countdown_title` = `%1$s · %2$s` (prayer name · clock time).
- `notification_remaining` = `%1$s remaining` (wraps the existing `formatRemaining` output, e.g. `1h 30m remaining`).
- Remove `notification_next_prayer` (no longer used).
- Keep `notification_remaining_hours_minutes` / `notification_remaining_minutes` — still used by `formatRemaining`.

### Edge cases

- **First prayer of day / no previous:** progress bar hidden when `previousPrayerTimeMillis` is null.
- **Countdown reaches 0:** unchanged — cancels; the PRAYER alarm transitions to the next prayer.
- **Fajr disabled in prayer toggles:** overnight target still shows Fajr (the countdown is informational, not an alarm).
- **Overnight target computation:** the scheduler computes tomorrow's Fajr time from the prayer-times use case; if it fails, fall back to today's behavior (no overnight target).

## Testing

### Unit tests (pure JVM)

- `SchedulePlanTest` — `previousPrayerTimeMillis` populated per prayer; last enabled prayer's `nextPrayer*` points at tomorrow's Fajr.

### Robolectric tests

- `PrayerNotificationSchedulerTest` — `updateCountdown` new signature; previous time threaded through tick intent; overnight Fajr target.
- `AlarmReceiverTest` — reads `EXTRA_PREVIOUS_PRAYER_TIME` and passes it through.
- `PrayerNotificationManagerTest` — new `showCountdownNotification` signature; title format `Name · HH:mm`; body `Xh Ym remaining`; progress bar max/progress values; progress bar omitted when previous time is null.

## Out of scope

- Seconds display / sub-minute refresh cadence (deliberately kept at 60s for battery).
- Changing the `Stop` action behavior.
- Other notification types (prayer, adhan, reminders, special days) — unchanged.

## Docs

- Update `TODO.md` — mark the countdown notification improvement as done once implemented.
