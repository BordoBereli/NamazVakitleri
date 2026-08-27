# Ezan Reliability, Volume Control & Easy Stop

Date: 2026-08-27

## Overview

Fix the Ezan (adhan) so it plays **exactly** at the prayer time, lets the user adjust its volume with the hardware volume buttons while it plays, and can be stopped easily (notification Stop / swipe, or volume-down to 0) while continuing to play in the background. The next prayer must always fire again.

## Problem

1. **Ezan fires late or not at all.** `PrayerNotificationScheduler.setExactAlarm()` silently falls back to the *inexact* `setAndAllowWhileIdle` when the exact-alarm permission is missing (denied by default on Android 13+). Inexact alarms are batched/deferred by the OS (Doze), so the Ezan fires late — and on Android 12+ the `startForegroundService(AdhanService)` call from a deferred alarm can be blocked entirely (`ForegroundServiceStartNotAllowedException`).
2. **Some days have no alarms at all.** `SchedulePlan.buildDailyAlarms()` only schedules today's prayers that are after `now`. The `DailyRescheduleWorker` runs at an OS-chosen time; if it runs after the last prayer, the next day has zero alarms.
3. **Volume buttons stop instead of adjust.** `AdhanService`'s `ContentObserver` stops the Ezan on *any* alarm-volume decrease (the "quick-stop" feature), conflicting with the desired behavior where volume buttons adjust the Ezan loudness.
4. **Disabling notifications doesn't stop a playing Ezan.** `cancelAll()` cancels alarms and the countdown but not the `AdhanService`.

## Goal

- The Ezan plays exactly at each prayer time (requires the exact-alarm permission).
- Volume up/down while the Ezan plays adjusts its loudness (via the alarm stream); pressing volume-down to 0 stops it.
- The Ezan keeps playing when the app is in the background; the user can stop it easily (notification Stop button, swiping the notification, or volume-down to 0).
- The next prayer always fires again (24-hour alarm coverage).
- When the exact-alarm permission is missing, block scheduling and prompt the user to grant it (Notifications screen only).

## Approach

- **Exact-alarm permission:** remove the silent inexact fallback; block all scheduling when the permission is missing on Android 12+; show an `AlertDialog` in the Notifications screen with a **Grant** action opening `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Keep `SCHEDULE_EXACT_ALARM` (user-grantable, Play-policy safe). The foreground-service permission (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) is already declared and auto-granted — no change needed.
- **24-hour coverage:** `SchedulePlan.buildDailyAlarms()` schedules today's remaining prayers **plus** tomorrow's full day, so there is never a gap regardless of when scheduling runs.
- **Volume behavior:** `AdhanService`'s `ContentObserver` stops only when the alarm-stream volume reaches 0; otherwise it just tracks the value. Because `AdhanPlayer` uses `USAGE_ALARM`, the `MediaPlayer` follows the alarm stream automatically, so volume buttons adjust the Ezan in real time. The `adhanVolume` slider keeps scaling it (slider% × alarm volume).
- **Easy stop + background play:** keep the foreground service (plays in background); stop via notification Stop/swipe or volume-down to 0; `cancelAll()` also stops the `AdhanService`.

## Design

### 1. Exact-alarm permission — block scheduling (PrayerNotificationScheduler)

- `setExactAlarm(triggerAtMillis, pendingIntent)`:
  - If `Build.VERSION.SDK_INT >= S && !alarmManager.canScheduleExactAlarms()` → `Log.w("PrayerNotificationScheduler", "Exact alarm permission missing; skipping alarm")` and return without scheduling.
  - Else → `alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pendingIntent)`.
  - The `setAndAllowWhileIdle` fallback is removed.
- `scheduleAllSuspending()`: after the `settings.enabled` check, add:
  - If `Build.VERSION.SDK_INT >= S && !alarmManager.canScheduleExactAlarms()` → `cancelAll()` + `cancelDailyReschedule()` + return early (nothing scheduled).
- No manifest change (`SCHEDULE_EXACT_ALARM` already declared).

### 2. Exact-alarm permission — UI prompt (NotificationsScreen)

- Add `var showExactAlarmDialog by remember { mutableStateOf(false) }`.
- Show the dialog when:
  - The user enables **Notifications** or **Ezan** while `!canScheduleExactAlarms` (before/alongside the toggle action), and
  - The screen loads with `settings.enabled` true but `!canScheduleExactAlarms` (returning users).
- Dialog content (localized): title + body explaining the Ezan needs the "Alarms & reminders" permission to play exactly on time.
  - **Grant** → `context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}") })`; dismiss dialog.
  - **Not now** → dismiss dialog.
- Keep the existing `PermissionHintRow` as a persistent banner when `!canScheduleExactAlarms`.
- Re-check `canScheduleExactAlarms` on `ON_RESUME` (already implemented).

### 3. Strings

Add to all 15 locale files (`values`, `values-ar`, `values-ta`, `values-fa`, `values-es`, `values-ur`, `values-th`, `values-tr`, `values-fr`, `values-de`, `values-ru`, `values-bn`, `values-hi`, `values-id`, `values-ms`):

- `exact_alarm_dialog_title`
- `exact_alarm_dialog_body`
- `exact_alarm_grant`
- `exact_alarm_not_now`

### 4. 24-hour coverage (SchedulePlan)

- `buildDailyAlarms` gains `tomorrowPrayers: List<Prayer>`.
- Build alarms for:
  - Today's enabled prayers whose trigger is after `now` (existing behavior).
  - Tomorrow's full day of enabled prayers (all triggers are in the future).
- Per-prayer date handling:
  - `isJumuah` = `jumuahEnabled && prayer.name == "Dhuhr" && <prayer's own date>.dayOfWeek == FRIDAY`.
  - `previous`/`next` chaining across midnight: today's last enabled prayer's `nextPrayerTimeMillis`/`nextPrayerName` = tomorrow's Fajr trigger/name; tomorrow's Fajr's `previousPrayerTimeMillis` = today's last enabled prayer's trigger.
- Request codes: sequential from 1000 across both days (up to 10 prayer + 10 pre-prayer alarms). `PrayerNotificationScheduler.REQUEST_CODE_END` expands from 1010 to 1020.

### 5. 24-hour coverage (PrayerNotificationScheduler)

- Fetch today's prayers and tomorrow's prayers (two `getPrayerTimesUseCase` calls; tomorrow's with `persistDailyCache = false`).
- Pass both to `buildDailyAlarms`.
- Remove the `nextDayFajrTimeMillis` countdown special-case (the `else if` branch and the `nextDayFajrTimeMillis` computation): with 24-hour coverage, `alarms.firstOrNull { it.type == AlarmType.PRAYER }` always finds the next prayer.
- `REQUEST_CODE_END = 1020`.
- `DailyRescheduleWorker` and `BootReceiver` unchanged (they call `scheduleAll`/`scheduleAllSuspending`, which now covers 24 hours).

### 6. Volume behavior (AdhanService)

- `volumeObserver.onChange`:
  - Read `current = audioManager.getStreamVolume(STREAM_ALARM)`.
  - If `current <= 0` → `stopSelf()`.
  - Update `lastAlarmVolume = current`.
  - No stop on non-zero decreases (the `MediaPlayer` follows the alarm stream automatically via `USAGE_ALARM`).
- `AdhanPlayer.play(prayerKey, adhanVolume)` scaling unchanged (slider% × alarm stream).
- Edge case: if the alarm stream is already 0 when the Ezan starts, it plays silently; the user can raise the volume. Documented, not special-cased.

### 7. Easy stop + background play

- `PrayerNotificationScheduler.cancelAll()`: add `context.stopService(Intent(context, AdhanService::class.java))` so disabling notifications stops a playing Ezan.
- Stop paths: notification **Stop** action, swiping the notification (`deleteIntent`), volume-down to 0.
- Background play: unchanged (foreground service keeps playing when the app is backgrounded).

## Testing

- **SchedulePlanTest**:
  - Schedules today's remaining prayers plus tomorrow's full day.
  - Jumuah flag on tomorrow's Friday Dhuhr.
  - `previous`/`next` chaining across midnight (today's Isha → tomorrow's Fajr).
  - Request-code range covers both days.
- **PrayerNotificationSchedulerTest**:
  - `scheduleAll without exact alarm permission schedules nothing` (verify Robolectric `ShadowAlarmManager.canScheduleExactAlarms()` behavior on SDK 35; stub via `shadowOf(alarmManager)` if needed).
  - `cancelAll stops the adhan service`.
  - Update existing tests for the new `buildDailyAlarms` signature (mock tomorrow's prayers).
  - Update countdown tests (remove `nextDayFajrTimeMillis` special-case).
- **AdhanServiceTest**:
  - Volume decrease to a non-zero value does NOT stop the service.
  - Volume to 0 stops the service.
- **NotificationsScreenTest**:
  - Dialog appears when enabling Notifications/Ezan without the exact-alarm permission.
  - **Grant** launches the `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` intent.
  - **Not now** dismisses the dialog.
- **AlarmReceiverTest**: unchanged.

## Out of scope

- Switching to `USE_EXACT_ALARM` (auto-granted) — rejected due to Play-policy risk; staying with `SCHEDULE_EXACT_ALARM` + prompt.
- App-launch permission prompt — Notifications screen only.
- Stopping the Ezan when the app is swiped from Recents — the Ezan intentionally keeps playing in the background.
- Changing the `adhanVolume` slider model.
