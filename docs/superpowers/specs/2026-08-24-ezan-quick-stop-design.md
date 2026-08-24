# Ezan Quick-Stop Feature

Date: 2026-08-24

## Overview

When the Ezan (adhan) sound plays at prayer time, the user currently has no way to stop it quickly — it plays until the audio file finishes. This spec adds three ways to stop the Ezan instantly:

1. A **Stop** action button on the prayer notification.
2. **Swiping away** the notification.
3. Pressing the **volume-down** button.

The feature is implemented by moving Ezan playback from the `BroadcastReceiver` into a new foreground service (`AdhanService`), which is the only component that can reliably intercept the physical volume-down key.

## Problem

- The Ezan sound is played by `AdhanPlayer` directly from `AlarmReceiver` (a `BroadcastReceiver`). The prayer notification has no action buttons, and swiping it away does not stop the sound.
- A `BroadcastReceiver` cannot receive key events, so the volume-down button cannot be intercepted without a foreground service.
- The Ezan plays to completion (~minutes) with no way to interrupt it.

## Goal

- Let the user stop the Ezan instantly via a notification **Stop** button.
- Let the user stop the Ezan by **swiping away** the notification.
- Let the user stop the Ezan by pressing **volume-down** (the event is consumed so the volume does not also decrease).
- When Ezan is enabled, the service's notification **replaces** the current plain prayer notification (e.g. "İmsak vakti geldi") — one notification at prayer time.
- When Ezan is disabled, existing behavior is unchanged.

## Approach

Introduce a foreground service `AdhanService` that owns Ezan playback while it is playing. `AlarmReceiver` starts the service instead of calling `adhanPlayer.play()` directly. The service:

- Plays the Ezan via the existing `AdhanPlayer`.
- Shows a foreground notification (prayer name + "Ezan çalıyor" + Stop action) on the existing `adhan` channel.
- Overrides `onKeyDown` to stop on `KEYCODE_VOLUME_DOWN`.
- Uses `setDeleteIntent` so swiping the notification stops the service.
- Auto-stops when the audio completes (via a new completion callback on `AdhanPlayer`).

This follows the existing `ACTION_STOP_COUNTDOWN` broadcast pattern already used by the countdown notification.

## Design

### 1. New `AdhanService` (foreground service)

Location: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt`

- Implements `KoinComponent` and injects `AdhanPlayer` and `PrayerNotificationManager` via `by inject()` — the same pattern `AlarmReceiver` already uses (module uses `@ComponentScan`, so `@Single` classes are auto-provided).
- `onStartCommand(intent, flags, startId)`:
  - Reads `EXTRA_PRAYER_KEY` from the intent.
  - Calls `adhanPlayer.play(prayerKey)` (which internally stops any current playback first).
  - Calls `startForeground(NOTIFICATION_ID_ADHAN, notification)` with the notification built by `PrayerNotificationManager.buildAdhanNotification(prayerKey)`.
  - Returns `START_NOT_STICKY`.
- `onKeyDown(keyCode, event)`:
  - If `keyCode == KeyEvent.KEYCODE_VOLUME_DOWN` → `stopSelf()` and return `true` (consume the event so volume does not decrease).
  - Otherwise return `super.onKeyDown(keyCode, event)`.
- `onDestroy()`:
  - Calls `adhanPlayer.stop()`.
  - Calls `stopForeground(STOP_FOREGROUND_REMOVE)`.
- Auto-stop on completion: `AdhanPlayer` exposes a completion callback; the service registers it and calls `stopSelf()` when the audio finishes.

### 2. Modified `AlarmReceiver`

- New action constant `ACTION_STOP_ADHAN = "STOP_ADHAN"`.
- In `handleAlarm`, `AlarmType.PRAYER` branch:
  - If `settings.adhanEnabled` → start `AdhanService` with `EXTRA_PRAYER_KEY` (replaces `adhanPlayer.play()` + `showPrayerNotification()`). Applies to regular prayers **and** Jumuah (Jumuah currently also plays the Ezan when enabled).
  - Else → existing behavior (`showPrayerNotification` / `showJumuahNotification`), no service.
- In `onReceive`, handle `ACTION_STOP_ADHAN` → `context.stopService(Intent(context, AdhanService::class.java))`.

### 3. Modified `AdhanPlayer`

- Add a completion callback so the service can auto-stop:
  - `fun setOnCompletionListener(listener: () -> Unit)` — registers a `MediaPlayer.setOnCompletionListener` on the current player; re-applied on each `play()`.
  - `stop()` already exists and is idempotent; it should also clear the completion listener reference.

### 4. Modified `PrayerNotificationManager`

- Add `buildAdhanNotification(prayerName: String): Notification`:
  - Channel: `CHANNEL_ADHAN` (existing, silent — the sound is played by `MediaPlayer`).
  - Title: localized prayer name (reuse `localizedPrayerName`).
  - Text: `notification_adhan_playing` (new string).
  - Action: "Stop" (`notification_stop`) → `PendingIntent.getBroadcast` with `ACTION_STOP_ADHAN`.
  - `setOngoing(false)` so it is swipeable; `setDeleteIntent` → same `ACTION_STOP_ADHAN` broadcast (swipe stops the service).
  - `setAutoCancel(true)`, `setPriority(PRIORITY_HIGH)`, small icon `ic_notification`.
- Add constant `NOTIFICATION_ID_ADHAN = 1009`.

### 5. Manifest & permissions

In `prayer_notifications/src/main/AndroidManifest.xml`:

- Add permissions:
  - `android.permission.FOREGROUND_SERVICE`
  - `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- Add service declaration:
  - `<service android:name=".scheduler.AdhanService" android:exported="false" android:foregroundServiceType="mediaPlayback" />`

### 6. Strings (all 15 locales: values, ar, ta, fa, es, ur, th, tr, fr, de, ru, bn, hi, id, ms)

- Add `notification_adhan_playing`:
  - `values` (en): `Adhan playing`
  - `values-tr`: `Ezan çalıyor`
  - Other locales: translated equivalents.
- Reuse existing `notification_stop` (already present in all locales).

### 7. Edge cases

- **New prayer while Ezan is playing:** `AdhanService.onStartCommand` calls `adhanPlayer.play()` which stops the current playback and starts the new one; the foreground notification is rebuilt with the new prayer name.
- **Process death while playing:** `START_NOT_STICKY` — the service is not restarted; the Ezan stops (acceptable; the notification is gone too).
- **Jumuah with adhan enabled:** the Ezan notification replaces the Jumuah notification (consistent with "replace prayer notification").
- **Countdown transition:** unchanged — the PRAYER alarm still transitions the countdown to the next prayer regardless of the service.
- **Volume-down consumed:** the event is consumed so the device volume does not decrease while the Ezan is playing.

## Testing

### Robolectric tests

- **New `AdhanServiceTest`**:
  - `onStartCommand` plays the Ezan and shows a foreground notification.
  - `onKeyDown` with `KEYCODE_VOLUME_DOWN` stops the service and returns `true`.
  - Completion callback stops the service.
  - Delete intent (swipe) broadcasts `ACTION_STOP_ADHAN` which stops the service.
- **Update `AlarmReceiverTest`**:
  - PRAYER alarm with `adhanEnabled = true` starts `AdhanService` (and does not call `showPrayerNotification` / `adhanPlayer.play` directly).
  - PRAYER alarm with `adhanEnabled = false` keeps existing behavior.
  - `ACTION_STOP_ADHAN` stops the service.
- **Update `AdhanPlayerTest`**:
  - Completion listener is invoked when playback completes (via `ShadowMediaPlayer`).
- **Update `PrayerNotificationManagerTest`**:
  - `buildAdhanNotification` builds a notification with the localized prayer name title, "Ezan çalıyor" text, a Stop action, and a delete intent.

## Out of scope

- Changing the Ezan audio files or playback volume behavior.
- Adding a stop control to the in-app UI (home screen) — notification + volume-down only.
- Other notification types (countdown, reminders, special days) — unchanged.
- Changing the countdown `Stop` action behavior.

## Docs

- Update `TODO.md` — mark the Ezan quick-stop feature as done once implemented.
