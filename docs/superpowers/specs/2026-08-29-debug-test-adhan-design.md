# Debug "Test Adhan" Trigger Design

**Status:** Approved 2026-08-29
**Branch:** none yet (new small feature off `main` after the Media3 migration)

## Problem

There is no way to trigger the adhan (Ezan) on demand for on-device verification. The
only button in Settings → Notifications ("Send test notification") posts a static
notification — it does not play audio. `AdhanService` and `AlarmReceiver` are both
`exported="false"` with no intent-filters, so neither ADB shell broadcasts nor
`adb am startforegroundservice` can reach them. Verifying the Media3 migration's manual
device checklist (Settings → Notifications plan, Task 3 Step 3) currently requires either
waiting for a real prayer time or a device-clock trick.

## Goal

Add a **debug-only** (release builds do not ship it) "Test Adhan" control in
Settings → Notifications that schedules a **real prayer alarm** at a user-chosen delay of
**0–15 minutes**, so the playback fires through the exact production path — `AlarmReceiver`
→ `AdhanService` → Media3 `AdhanPlayer` — and thus survives process death and Doze / idle,
exactly like a genuine prayer alarm.

## Scope

- **In scope:**
  - `prayer_notifications/.../scheduler/AlarmScheduler.kt` (interface: new method)
  - `prayer_notifications/.../scheduler/PrayerNotificationScheduler.kt` (implementation)
  - `prayer_feature/settings/.../notifications/NotificationsContract.kt` (new event)
  - `prayer_feature/settings/.../notifications/NotificationsViewModel.kt` (inject `AlarmScheduler`)
  - `prayer_feature/settings/.../notifications/NotificationsScreen.kt` (debug section, chips + button)
  - `prayer_feature/settings/build.gradle.kts` (enable `BuildConfig`)
  - `prayer_feature/settings/src/main/res/values/strings.xml` (debug strings; other locales fall back to default)
  - Tests: `PrayerNotificationSchedulerTest`, `NotificationsViewModelTest`, `NotificationsScreenTest`
- **Out of scope (untouched):** `AlarmReceiver.kt`, `AdhanService.kt`, `AdhanPlayer.kt`,
  `AdhanVolumeController.kt`, the data store, `NotificationDisplayer.kt`, release UIs.

## Decisions

1. **Faithful to the real prayer path (no force-play).** The test schedules an ordinary
   `AlarmType.PRAYER` alarm for `prayerKey = "Fajr"`. When it fires, `AlarmReceiver.handleAlarm`
   reads the data store exactly as for a real prayer:
   - if the Adhan toggle (`adhanEnabled`) is ON → `startForegroundService(AdhanService)` and the
     Ezan plays;
   - if OFF → a static prayer notification is shown instead (no audio).
   The debug UI surfaces the current Adhan-toggle state and warns when it is OFF so the test
   never fails silently.
2. **Real `AlarmManager` exact alarm.** `setExactAndAllowWhileIdle(RTC_WAKEUP)` (the scheduler's
   existing `setExactAlarm`, with its SDK-31+ `canScheduleExactAlarms()` guard) is reused. This is
   what makes the test valid for "app killed + phone idle" scenarios: the alarm survives process
   death and Doze. `delayMinutes = 0` fires immediately (the alarm is set for `now`).
3. **Dedicated request code outside the managed ranges.** `REQUEST_CODE_TEST_ADHAN = 900`. The
   daily-plan range is `1000..1020` and the misc range is `2000..2003`; `cancelAll()` only cancels
   those, so a scheduled test alarm is not wiped by normal rescheduling. A new test scheduling
   (any delay) first cancels any previous test alarm, so repeated tests do not stack.
4. **Delay presets, not free input.** FilterChip row: **Instant (0) / 1m / 5m / 10m / 15m**, then
   a "Schedule Adhan test" button. No keyboard, no validation path.
5. **Debug-only gate.** The section is rendered inside `if (BuildConfig.DEBUG)` in
   `NotificationsContent`. Enabling `buildFeatures.buildConfig = true` in
   `prayer_feature/settings/build.gradle.kts` generates `com.kutluoglu.prayer_feature.settings.BuildConfig`.
   Debug unit tests run against the debug variant, so the section is visible and testable;
   release builds exclude it at compile time.
6. **Exact-alarm-permission reuse.** If `canScheduleExactAlarms()` is false, the existing
   permission-dialog flow is reused: store the pending action
   `{ onEvent(ScheduleTestAdhan(delay)) }` in `pendingExactAlarmAction`, show `showExactAlarmDialog = true`,
   and the existing `ON_RESUME` observer fires the action once the user grants the permission.
7. **`AlarmScheduler` is the seam.** The ViewModel depends on the existing `AlarmScheduler`
   interface (Koin `@Single` bound to `PrayerNotificationScheduler`, same injection pattern as the
   ViewModel's existing `NotificationDisplayer`), keeping the feature layer free of `AlarmManager`.

## Target Architecture

### `AlarmScheduler` (interface, `prayer_notifications`)

```kotlin
/**
 * Schedules a single debug "Test Adhan" prayer alarm [delayMinutes] from now.
 * Delays outside 0..15 are clamped. Scheduling again cancels any prior test alarm.
 */
fun scheduleTestAdhan(delayMinutes: Int)
```

### `PrayerNotificationScheduler` (implementation)

```kotlin
companion object {
    const val REQUEST_CODE_TEST_ADHAN = 900
}

override fun scheduleTestAdhan(delayMinutes: Int) {
    val delay = delayMinutes.coerceIn(0, 15) * 60_000L
    // cancel prior test alarm (code 900, FLAG_NO_CREATE, bare Intent)
    pendingIntent?.let { alarmManager.cancel(it) }
    scheduleAlarm(
        ScheduledAlarm(
            prayerKey = "Fajr",
            triggerAtMillis = System.currentTimeMillis() + delay,
            requestCode = REQUEST_CODE_TEST_ADHAN,
            type = AlarmType.PRAYER
        )
    )
}
```

`scheduleAlarm`/`setExactAlarm` are existing private helpers (identical extras as a real prayer:
`EXTRA_ALARM_TYPE`, `EXTRA_PRAYER_KEY`, `EXTRA_IS_JUMUAH`, trigger-time extras — all already present).

### `NotificationsContract`

```kotlin
data class ScheduleTestAdhan(val delayMinutes: Int) : NotificationsEvent()
```

### `NotificationsViewModel`

Constructor gains `private val alarmScheduler: AlarmScheduler`. `onEvent` handles
`ScheduleTestAdhan(delayMinutes) -> alarmScheduler.scheduleTestAdhan(delayMinutes)`.

### `NotificationsScreen` (debug-only section, after the "Send test notification" button)

```
HorizontalDivider
"Test Adhan" (bodyLarge)
FilterChip row: Instant / 1m / 5m / 10m / 15m   (selected = remember { mutableStateOf(default 5m) })
Button "Schedule Adhan test"
If !settings.adhanEnabled: subtitle "Adhan toggle is OFF — the test will show a notification but will NOT play sound"
```

On click:
1. If `checkExactAlarmPermission()` is false → `pendingExactAlarmAction = { onEvent(ScheduleTestAdhan(selected)) }`; `showExactAlarmDialog = true`; return.
2. Else `onEvent(ScheduleTestAdhan(selected))`.

No snackbar/confirmation is added: the scheduled alarm is one-shot and the user performs the
on-device verification themselves (the delay IS the confirmation).

## Error Handling

- **Exact-alarm permission missing** (SDK 31+): reuses the existing dialog + `pendingExactAlarmAction`
  resume flow. If the user declines, the action is dropped (`pendingExactAlarmAction = null` on dismiss),
  matching the existing dialog behavior.
- **Adhan toggle OFF**: no error — the test legitimately exercises the prayer-notification branch. The
  UI warns beforehand (Decision 1).
- **Schedule failure**: `setExactAlarm` already logs-and-skips when exact alarms are unavailable; no new
  failure path is introduced.
- **Release builds**: the whole section is compiled out; no release resource/string bloat beyond the
  single default-locale strings.

## Testing

1. **`PrayerNotificationSchedulerTest` (Robolectric)** — `scheduleTestAdhan`:
   - `scheduleTestAdhan(5)` sets one `setExactAndAllowWhileIdle` alarm with `RTC_WAKEUP`, trigger ≈
     `now + 300_000`, request code `900`, and extras `extra_alarm_type=PRAYER`, `extra_prayer_key=Fajr`.
   - `scheduleTestAdhan(0)` sets trigger ≈ now.
   - `scheduleTestAdhan(20)` clamps to 15 minutes.
   - scheduling twice cancels the first alarm (only one live alarm for code 900).
2. **`NotificationsViewModelTest`** — `ScheduleTestAdhan(5)` calls `alarmScheduler.scheduleTestAdhan(5)`;
   constructor updated with `mockk<AlarmScheduler>()`.
3. **`NotificationsScreenTest` (Compose + Robolectric)** — debug section renders (adhan off → warning
   visible); tapping "Instant" then the button emits `ScheduleTestAdhan(0)`; tapping "5m" emits
   `ScheduleTestAdhan(5)`.
4. **Full regression** — `./gradlew assembleDebug testDebugUnitTest` and `:prayer_feature:settings:lintDebug`
   must pass; `gitnexus_detect_changes()` must show only the expected symbols.

## Acceptance Criteria

- A debug build of Settings → Notifications shows a "Test Adhan" section; a release build does not.
- Choosing a preset and tapping "Schedule Adhan test" fires the Ezan (or a prayer notification) at
  `now + delay` even after the app process is killed and the phone is idle/Doze, via the unchanged
  production `AlarmReceiver → AdhanService → Media3 AdhanPlayer` path.
- Delay options are exactly 0, 1, 5, 10, 15 minutes; scheduling again replaces the previous test alarm.
- The Adhan-toggle warning is shown when playback would not sound.