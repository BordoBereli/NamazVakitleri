# Notifications Delivery Wiring

Date: 2026-08-24

## Overview

The `prayer_notifications` module (built 2026-08-22) provides the full notification *infrastructure* — channels, scheduler, manager methods, adhan player, receivers, settings UI, and persistence — but the **runtime delivery** of most features is still stubbed. `AlarmReceiver` currently posts a test notification for any prayer key, and daily-reminder / Jumu'ah / special-days / countdown are stored in settings but never scheduled or delivered.

This spec completes the end-to-end delivery: wire `AlarmReceiver` → real prayer notification + adhan, add scheduling for daily-reminder / Jumu'ah / special-days (incl. a one-day-ahead pre-special-day alert), and implement the periodic countdown updater.

## Problem

The code comment in `AlarmReceiver.kt:27` says *"Full prayer-time handling (post notification + adhan) is completed in Task 5.5 once use cases are wired"* — but Task 5.5 as written only covered use cases + Koin wiring. The actual delivery wiring was never scheduled, so:

- `AlarmReceiver` calls `showTestNotification()` for every prayer key (`AlarmReceiver.kt:29`).
- `showPrayerNotification` / `showCountdownNotification` exist but are never invoked.
- `AdhanPlayer.play()` is never called.
- `SchedulePlan` only emits prayer + pre-prayer alarms — daily reminder, Jumu'ah, and special days are never scheduled.
- The countdown notification has no updater.

## Goal

- Post a real prayer notification at each enabled prayer time, routed to the adhan channel when adhan is enabled, and play the adhan audio.
- Post a Jumu'ah notification on Fridays (reusing the Dhuhr alarm).
- Post a pre-prayer reminder (`X minutes before`) on the reminders channel.
- Post a daily reminder at the user-chosen time containing a summary of today's prayer times.
- Post special-day notifications (Ramadan start, Eid al-Fitr, Eid al-Adha, Laylat al-Qadr) plus a one-day-ahead pre-special-day alert.
- Run an always-on countdown to the next prayer, updated every minute, dismissible via a Stop action.
- Replace the silent `adhan.mp3` placeholder with a real, freely-licensed adhan recording.

## Approach

Extend the existing single-scheduler design (Approach A — chosen over separate per-feature schedulers and over a delivery-dispatcher layer). The `AlarmReceiver` becomes a thin dispatcher over typed alarms; `SchedulePlan` and `PrayerNotificationScheduler` grow to emit and schedule the additional alarm types. All logic stays testable with the existing pure-JVM + Robolectric setup.

## Design

### Alarm types

Add an `AlarmType` enum carried in the alarm intent extra:

```kotlin
enum class AlarmType { PRAYER, PRE_PRAYER, COUNTDOWN_TICK, DAILY_REMINDER, SPECIAL_DAY, PRE_SPECIAL_DAY }
```

`ScheduledAlarm` gains a `type: AlarmType` field. The receiver dispatches on it.

### Request codes

| Code | Purpose |
|---|---|
| 1000–1010 | Prayer + pre-prayer alarms (existing) |
| 2000 | Countdown tick (single, self-re-arming) |
| 2001 | Daily reminder |
| 2002 | Special day |
| 2003 | Pre-special day |

`PrayerNotificationScheduler.cancelAll()` extends to cancel all of these.

### `SchedulePlan` extension

`buildDailyAlarms(...)` additionally emits:

- **Daily reminder** — when `dailyReminderEnabled`, an alarm at today's chosen `hour:minute` (only if in the future), carrying a pre-built prayer-times summary string (`EXTRA_DAILY_SUMMARY`). The summary is formatted at schedule time as `Fajr 05:12 · Dhuhr 12:30 · Asr 15:45 · Maghrib 18:20 · Isha 19:35` (times use the app's existing `HH:mm` formatting).
- **Special day** — when today is a special day, an alarm at 08:00 (`SPECIAL_DAY`).
- **Pre-special day** — when tomorrow is a special day, an alarm at 08:00 (`PRE_SPECIAL_DAY`).
- **Jumu'ah** — no new alarm; the Friday Dhuhr alarm gets an `EXTRA_IS_JUMUAH = true` flag.
- **Next-prayer time** — every prayer alarm carries `EXTRA_NEXT_PRAYER_TIME` (epoch millis of the next prayer), so the receiver can start the countdown without recomputing prayer times.

### `SpecialDaysCalculator` (new, pure)

Maps a Hijri date + hijri adjustment to a special day:

```kotlin
enum class SpecialDay { RAMADAN_START, EID_AL_FITR, EID_AL_ADHA, LAYLAT_AL_QADR }

fun specialDayFor(date: LocalDate, hijriAdjustment: Int): SpecialDay?
```

Runs inside the existing daily `DailyRescheduleWorker` (no new worker). The scheduler checks today and tomorrow.

### `PrayerNotificationScheduler` changes

- Builds the extended plan (daily reminder, special day, pre-special day, next-prayer extras).
- If countdown enabled, starts the countdown chain immediately: posts the countdown to the next upcoming prayer and schedules the first `COUNTDOWN_TICK` alarm.
- `cancelAll()` covers the new request codes.

### `AlarmReceiver` dispatch

Becomes a thin dispatcher using `goAsync()` + a coroutine (reads settings via the injected `NotificationSettingsDataStore` without blocking the main thread):

| Alarm type | Action |
|---|---|
| `PRAYER` | Post prayer notification (+ adhan if enabled). If `EXTRA_IS_JUMUAH` → post Jumu'ah notification on reminders channel instead. If countdown enabled → start countdown to `EXTRA_NEXT_PRAYER_TIME`. |
| `PRE_PRAYER` | Post pre-prayer reminder ("Fajr in 15 minutes") on reminders channel. |
| `COUNTDOWN_TICK` | Recompute remaining to target, update the ongoing notification, re-arm the next tick — or stop when the prayer time is reached. |
| `DAILY_REMINDER` | Post the summary notification, then re-arm tomorrow's alarm (self-sustaining). |
| `SPECIAL_DAY` / `PRE_SPECIAL_DAY` | Post "Today is X" / "Tomorrow is X" on reminders channel. |
| `STOP_COUNTDOWN` | Cancel countdown notification + pending tick alarms. |

### Countdown chain

Self-re-arming exact alarm:

- Started at schedule time (if enabled) and restarted after each prayer, targeting the next prayer's time.
- Each tick re-arms itself at +1 min until the target is reached, then stops (the prayer alarm takes over).
- After the last prayer of the day there is no next prayer, so the chain stops until tomorrow's schedule restarts it.
- "Stop" action cancels the chain; the settings toggle re-enables it (re-schedules via `UpdateNotificationSettingsUseCase`).

### `PrayerNotificationManager` API tweak

`showPrayerNotification` / `showCountdownNotification` change from taking a `Prayer` object to taking `prayerName: String` (they only used `prayer.name`). This keeps the receiver from reconstructing a `Prayer`. Low risk — no current callers (verified via impact analysis).

New builder methods:
- `showJumuahNotification()` — reminders channel.
- `showPrePrayerNotification(prayerName, minutes)` — reminders channel.
- `showDailyReminderNotification(summary)` — reminders channel.
- `showSpecialDayNotification(day)` / `showPreSpecialDayNotification(day)` — reminders channel.

### Adhan

On `PRAYER`, if `adhanEnabled`, call `adhanPlayer.play()`. Playback failure already falls back to the channel's default sound (`AdhanPlayer` try/catch). The silent placeholder `adhan.mp3` is replaced with a real, freely-licensed recording (see Assets).

### Error handling

- Exact-alarm permission revoked → existing fallback to inexact alarms.
- Missing/empty location → scheduler no-ops (existing).
- Settings read failure in receiver → skip notification gracefully.
- Special-day / daily-reminder time already passed at schedule time → skip for today (daily worker re-checks tomorrow).

## Assets

- **Adhan audio**: research freely-licensed / public-domain adhan recordings; present 2–3 options (source + license) for the user to choose. Replace `res/raw/adhan.mp3`.

## Testing

### Unit tests (pure JVM)

- `SpecialDaysCalculatorTest` — Hijri date → special-day mapping (all 4 days), incl. hijri-adjustment edge cases.
- `SchedulePlanTest` — extended: daily-reminder alarm, special-day + pre-special-day alarms, `isJumuah` flag on Friday Dhuhr, next-prayer-time extras.

### Robolectric tests

- `AlarmReceiverTest` — dispatch per alarm type (prayer, pre-prayer, countdown tick, daily reminder, special day, pre-special day, stop).
- `PrayerNotificationManagerTest` — updated signatures; new Jumu'ah / pre-prayer / special-day / daily-reminder builders.
- `PrayerNotificationSchedulerTest` — countdown chain start, daily-reminder + special-day scheduling, extended `cancelAll()`.
- `AdhanPlayerTest` — unchanged.

## Out of scope

- Real adhan audio content selection (user picks from research; wiring is in scope).
- Widgets, Room, or other unrelated tech debt.
- Comprehensive settings sub-screen UI tests (deferred in the original spec).

## Docs

- Update `TODO.md` — mark the delivery wiring as done once implemented.
