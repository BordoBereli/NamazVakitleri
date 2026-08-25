# Ezan Volume Control Feature

Date: 2026-08-25

## Overview

Add a volume control for the Ezan (adhan) sound in the notification settings. A slider (0–100%) scales the Ezan's playback volume via `MediaPlayer.setVolume()`. The default volume is 30%. The slider is visible only while the Ezan toggle is enabled.

## Problem

The Ezan currently plays at the full alarm-stream volume with no way to adjust it. Users want to control how loud the Ezan is without changing the system alarm volume.

## Goal

- Add an Ezan volume slider (0–100%) in the notification settings screen.
- Default volume is 30% for new users.
- The slider is visible only when the Ezan toggle is on.
- The volume applies only to the Ezan playback (via `MediaPlayer.setVolume`), not to the system alarm stream.

## Approach

- Add `adhanVolume: Int = 30` to `NotificationSettings`.
- Persist it via `NotificationSettingsDataStore` (new `ADHAN_VOLUME` int key).
- `AdhanPlayer.play(prayerKey, volumePercent)` applies `MediaPlayer.setVolume(v, v)` where `v = volumePercent / 100f`.
- `AdhanService` injects `NotificationSettingsDataStore`, reads the volume itself (architecture: the service owns its settings), and passes it to `play()`.
- `NotificationsViewModel` handles a new `SetAdhanVolume` event.
- `NotificationsScreen` shows a `Slider` below the Ezan toggle when `adhanEnabled` is true.

## Design

### 1. Model

`NotificationSettings` gains `adhanVolume: Int = 30`.

### 2. DataStore

- New key `ADHAN_VOLUME = intPreferencesKey("adhan_volume")`.
- New `suspend fun updateAdhanVolume(volume: Int)`.
- `toSettings()` reads `adhanVolume = this[Keys.ADHAN_VOLUME] ?: 30`.

### 3. Use case

`UpdateNotificationSettingsUseCase` persists `adhanVolume` via `dataStore.updateAdhanVolume(settings.adhanVolume)`.

### 4. AdhanPlayer

`play(prayerKey: String, volumePercent: Int)`:
- In the `player.apply { ... }` block, after `start()`, call `setVolume(volumePercent / 100f, volumePercent / 100f)`.
- `volumePercent` is clamped to 0..100 defensively.

### 5. AdhanService

- Inject `NotificationSettingsDataStore` (via `by inject()`).
- Add a `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`.
- `onStartCommand`:
  - Read `EXTRA_PRAYER_KEY`; if null → `stopSelf()` + `START_NOT_STICKY`.
  - Call `startForeground(...)` synchronously (satisfies the 5-second rule for `startForegroundService()`).
  - In `serviceScope.launch { ... }`: read `dataStore.getSettings().adhanVolume` and call `adhanPlayer.play(prayerKey, volume)`.
  - Record alarm-volume baseline and register the volume observer (unchanged).
  - Return `START_NOT_STICKY`.
- `onDestroy`: cancel `serviceScope` (in addition to existing cleanup).

### 6. ViewModel

New event `NotificationsEvent.SetAdhanVolume(volume: Int)` → `update { it.copy(adhanVolume = volume) }`.

### 7. UI (NotificationsScreen)

Below the Ezan toggle, when `settings.adhanEnabled` is true, show:
- A label (`adhan_volume` string) and the current percentage (e.g. `30%`).
- A `Slider` with `value = settings.adhanVolume.toFloat()`, `valueRange = 0f..100f`, `onValueChange = { onEvent(NotificationsEvent.SetAdhanVolume(it.toInt())) }`.

### 8. Strings

Add `adhan_volume` to all 15 locale files (`values`, `values-ar`, `values-ta`, `values-fa`, `values-es`, `values-ur`, `values-th`, `values-tr`, `values-fr`, `values-de`, `values-ru`, `values-bn`, `values-hi`, `values-id`, `values-ms`).

## Testing

- **AdhanPlayerTest**: `play("Fajr", 30)` sets the shadow player volume to `0.3f` (`shadowOf(player).getLeftVolume()` / `getRightVolume()`).
- **NotificationSettingsDataStoreTest**: default `adhanVolume == 30`; `updateAdhanVolume` persists.
- **NotificationSettingsTest**: `NotificationSettings().adhanVolume == 30`.
- **NotificationUseCasesTest**: `UpdateNotificationSettingsUseCase` persists `adhanVolume`.
- **NotificationsViewModelTest**: `SetAdhanVolume` updates the state.
- **NotificationsScreenTest**: slider is shown when `adhanEnabled` is true (and hidden when false); adjusting it emits `SetAdhanVolume`.
- **AdhanServiceTest**: `onStartCommand` reads the volume from the mocked DataStore and calls `adhanPlayer.play("Fajr", volume)`.

## Out of scope

- Adjusting the system alarm-stream volume.
- Volume control for other notification sounds (countdown, reminders, etc.).
- A "test/play preview" button for the volume.
