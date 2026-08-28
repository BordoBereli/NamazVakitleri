# Ezan Reliability — Battery Optimization & Audio Focus

Date: 2026-08-28

## Overview

Two remaining Ezan (adhan) reliability gaps:

1. **Ezan does not play when the phone is locked / screen off.** The scheduler already uses exact alarms (`setExactAndAllowWhileIdle`) with 24-hour coverage, but the app never handles battery optimization. On most devices (especially OEMs like Samsung, Xiaomi, Huawei), when the screen is off the OS defers or blocks the exact alarm unless the app is exempted from battery optimization. The app neither requests the exemption nor guides the user to disable it.
2. **The hardware volume buttons do not adjust the Ezan while it plays.** `AdhanPlayer` plays on `USAGE_ALARM` (alarm stream) but never requests audio focus, so the system does not reliably treat the Ezan as the active alarm audio source and the volume keys are not routed to the alarm stream.

## Goal

- Guide the user to disable battery optimization for the app so the Ezan fires exactly on time even when the phone is locked (via the Play-safe system settings screen, with a clear localized explanation).
- Request audio focus in `AdhanPlayer` so the system routes the hardware volume buttons to the alarm stream, letting the user adjust the Ezan loudness while it plays (volume-down to 0 still stops it).

## Approach

- **Battery optimization:** detect `PowerManager.isIgnoringBatteryOptimizations(packageName)`; if not exempt, show a banner + dialog in the Notifications screen (same pattern as the exact-alarm permission). The dialog's **Open settings** action launches `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (no permission needed, Play-policy safe). The dialog text clearly guides the user to find the app in the list and set it to "Don't optimize".
- **Audio focus:** `AdhanPlayer` requests `AUDIOFOCUS_GAIN_TRANSIENT` with `USAGE_ALARM` attributes before playback and abandons it on stop/completion. On permanent focus loss (`AUDIOFOCUS_LOSS`) it stops playback and notifies the service via a new callback; transient loss/duck is ignored (alarm semantics).

## Design

### 1. Battery optimization — detection & UI (NotificationsScreen)

- New `checkBatteryOptimization()`:
  ```kotlin
  fun checkBatteryOptimization(): Boolean =
      context.getSystemService(PowerManager::class.java)
          ?.isIgnoringBatteryOptimizations(context.packageName) == true
  ```
- New state: `var ignoresBatteryOptimization by remember { mutableStateOf(checkBatteryOptimization()) }`.
- Re-check in the existing `ON_RESUME` observer (alongside `canScheduleExactAlarms`).
- New `var showBatteryDialog by remember { mutableStateOf(false) }` and `var batteryDialogDismissed by remember { mutableStateOf(false) }`.
- Show the dialog when the user enables **Notifications** or **Ezan** while `!ignoresBatteryOptimization` (before/alongside the toggle action), and when the screen loads with `settings.enabled` true but `!ignoresBatteryOptimization` (returning users).
- New `PermissionHintRow` banner below the exact-alarm banner when `!ignoresBatteryOptimization`:
  - Text: `battery_optimization_hint` ("Ezan'ın zamanında çalması için pil optimizasyonunu kapat").
  - Action: **Open settings** → `Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)`.
- Dialog content (localized):
  - Title: `battery_optimization_dialog_title`.
  - Body: `battery_optimization_dialog_body` — explains the Ezan needs battery optimization disabled to play on time when the phone is locked, and instructs: tap **Open settings**, find **Namaz Vakitleri** in the list, and choose **Don't optimize**.
  - **Open settings** → launch `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; dismiss dialog.
  - **Not now** → dismiss dialog.
- No manifest permission added (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` requires none).

### 2. Audio focus (AdhanPlayer)

- Inject `AudioManager` into `AdhanPlayer` (via constructor, alongside `Context`).
- `play(prayerKey, volumePercent)`:
  - Before creating the player, request audio focus:
    ```kotlin
    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(focusChangeListener)
        .build()
    audioManager.requestAudioFocus(focusRequest)
    ```
  - The `focusChangeListener`:
    - `AUDIOFOCUS_LOSS` → `stop()` + invoke a new `onFocusLoss` callback.
    - `AUDIOFOCUS_LOSS_TRANSIENT` / `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` → ignore (keep playing; alarm semantics).
- New `fun setOnFocusLossListener(listener: () -> Unit)` — the service registers it in `onCreate` and calls `stopSelf()`.
- `stop()`: abandon audio focus (`audioManager.abandonAudioFocusRequest(focusRequest)`).
- Completion: abandon audio focus (in the `setOnCompletionListener`).
- Keep `setVolume(volumePercent/100f)` scaling unchanged (slider% × alarm stream).

### 3. AdhanService

- In `onCreate`, register the focus-loss listener:
  ```kotlin
  adhanPlayer.setOnFocusLossListener { stopSelf() }
  ```
- No other changes (volume observer already stops only at 0).

### 4. Strings (all 15 locales: values, ar, ta, fa, es, ur, th, tr, fr, de, ru, bn, hi, id, ms)

- `battery_optimization_hint`
- `battery_optimization_dialog_title`
- `battery_optimization_dialog_body`
- `battery_optimization_open_settings`
- `battery_optimization_not_now`
- Reuse existing `open_settings` where appropriate.

## Testing

- **AdhanPlayerTest**:
  - `play` requests audio focus (verify via `shadowOf(audioManager).getLastAudioFocusRequest()` or similar).
  - `stop` abandons audio focus.
  - Focus loss (`AUDIOFOCUS_LOSS`) stops playback and invokes the focus-loss listener.
  - Transient focus loss does not stop playback.
- **AdhanServiceTest**:
  - Focus-loss listener stops the service.
- **NotificationsScreenTest**:
  - Banner shown when battery optimization is not ignored.
  - Dialog appears when enabling Notifications/Ezan without battery optimization exemption.
  - **Open settings** launches `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.
  - **Not now** dismisses the dialog.

## Out of scope

- Requesting `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Play-restricted) — rejected; using the settings screen instead.
- OEM-specific battery management instructions (Samsung "Sleeping apps", Xiaomi "Autostart", etc.) — the generic settings screen is the Play-safe entry point.
- Changing the `adhanVolume` slider model.
- Volume control when the screen is locked (device/OEM dependent; audio focus is the app-side enabler).
