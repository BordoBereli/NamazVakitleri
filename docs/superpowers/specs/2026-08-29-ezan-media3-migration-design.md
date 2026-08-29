# Ezan (Adhan) Jetpack Media3 Migration Design

**Status:** Approved 2026-08-29
**Branch:** `dev/feature/media3-migration`

## Problem

`prayer_notifications` uses deprecated Android media classes:

| File | Deprecated classes |
|------|--------------------|
| `AdhanPlayer.kt` | `android.media.MediaPlayer` |
| `AdhanVolumeController.kt` | `android.support.v4.media.session.MediaSessionCompat`, `android.support.v4.media.session.PlaybackStateCompat`, `androidx.media.VolumeProviderCompat` |
| `AdhanPlayerTest.kt` | Robolectric `ShadowMediaPlayer` (shadow of deprecated `MediaPlayer`) |

These are the only media-usage points in the project. Migrating them removes the
last deprecated media surface and consolidates adhan playback on Jetpack Media3.

## Goal

Replace the deprecated classes with **ExoPlayer + Media3 `MediaSession`** (native
Media3 device-volume flow — "Approach 1" plus a retained `AdhanVolumeController`
seam), preserving adhan behavior: playback on the ALARM stream, volume-% scaling,
audio-focus handling, completion cleanup, and mute-press-stops-adhan.

## Scope

- **In scope:**
  - `prayer_notifications/build.gradle.kts` + `gradle/libs.versions.toml` (dependency swap)
  - `prayer_notifications/.../manager/AdhanPlayer.kt`
  - `prayer_notifications/.../manager/AdhanVolumeController.kt`
  - `prayer_notifications/.../manager/AdhanPlayerTest.kt` and `AdhanVolumeControllerTest.kt`
  - `prayer_notifications/.../di/PrayerNotificationsModule.kt` (ExoPlayer Koin provider)
- **Out of scope (untouched):** `NotificationDisplayer.kt`,
  `AlarmReceiver.kt`, `PrayerNotificationScheduler.kt`, scheduler/data-store tests,
  and the framework audio-focus code (`AudioFocusRequest`/`AudioManager` are NOT deprecated).
  > **Post-migration deviation (2026-08-29):** `AdhanService.kt` was originally out of scope
  > but was modified by the mute-press regression fix — see "Post-Migration Fix" below.

## Decisions

1. **Full migration (ExoPlayer + Media3 session).** Replace both the playback layer
   (`MediaPlayer` → `ExoPlayer`) and the session layer
   (`MediaSessionCompat`/`VolumeProviderCompat`/`PlaybackStateCompat` → `Media3 MediaSession`).
2. **Manual audio focus retained.** `setAudioAttributes(attrs, handleAudioFocus = false)`;
   the existing `AudioFocusRequest` code and the
   `transient loss keeps playing / permanent loss stops + notifies` semantics are preserved.
   Focus APIs are not deprecated; delegating to ExoPlayer would change alarm semantics.
3. **Media3-native mute state.** `AdhanVolumeController` fires `onMuteRequested` when
   `Player.Listener.onDeviceVolumeChanged(volume, muted)` reports `muted || volume <= 0`.
   The exact "LOWER press at min-1 with no volume change" detection is not representable
   through Media3 player listeners and is replaced by the real mute/0 state. The existing
   `AdhanService` `volume_alarm_sound` ContentObserver (stop at volume 0) remains as backstop.
   > **Post-migration deviation (2026-08-29):** `onDeviceVolumeChanged` does NOT fire on
   > hardware volume-key presses (only on audio-device routing changes), so this decision
   > did not preserve mute-press-stops-adhan. The fix moved the mute detection to the
   > `AdhanService` ContentObserver, made min-aware — see "Post-Migration Fix" below.
4. **`AdhanVolumeController` = session lifecycle + mute listener.** It owns
   `MediaSession` create/release (`activate()`/`deactivate()`) and the device-volume listener.
   The 0–100 percent-scale mapping and `onAdjustVolume`/`onSetVolumeTo` plumbing are removed;
   Media3 displays native device volume.
5. **Test seam: injected `Player`, `FakePlayer` in tests.** `AdhanPlayer` takes
   `player: Player` via constructor (Koin provides an `ExoPlayer`). Unit tests inject
   `androidx.media3.test.utils.FakePlayer` (`media3-test-utils`), which can simulate
   `STATE_ENDED`, device volume and mute deterministically. Robolectric is retained for
   the focus/`AudioManager` assertions. `ShadowMediaPlayer` is deleted.
6. **Media3 version 1.11.0** (latest stable, Aug 2026). Requires the modern volume-flag
   API (`increaseDeviceVolume(flags)` etc.).

## Target Architecture

### `AdhanPlayer` (rewritten)

```kotlin
@Single
class AdhanPlayer(
    private val context: Context,
    private val player: Player,             // androidx.media3.common.Player (ExoPlayer in prod)
) {
    internal val volumeController = AdhanVolumeController(context, player)

    // constructor registers one Player.Listener:
    //   STATE_ENDED -> abandonAudioFocus(); volumeController.deactivate(); onCompletion?.invoke()
```

- `play(prayerKey, volumePercent)`:
  1. `stop()`
  2. `val resUri = Uri.parse("android.resource://${context.packageName}/$resId")`
     (resId mapping unchanged: Fajr/Dhuhr/Asr/Maghrib/Isha)
  3. `player.setMediaItem(MediaItem.fromUri(resUri))`
  4. `player.volume = volumePercent.coerceIn(0, 100) / 100f`
  5. `player.prepare()`; `player.playWhenReady = true`
  6. `requestAudioFocus()`; `volumeController.activate()`
  7. on failure (catch): `runCatching { player.stop() }`; `volumeController.deactivate()`;
     `Log.e(...)` (unchanged contract)
- `stop()`: `runCatching { player.stop() }`; `volumeController.deactivate()`;
  `abandonAudioFocus()`. Player is **reused** across plays (created once via Koin).
- `setOnCompletionListener` / `setOnFocusLossListener`: unchanged signatures.
- Mute wiring (unchanged shape): `controller.setOnMuteRequestedListener { stop(); onFocusLoss?.invoke() }`.
- Audio focus: `requestAudioFocus()`/`abandonAudioFocus()` and `focusChangeListener`
  (permanent `AUDIOFOCUS_LOSS` → `stop(); onFocusLoss?.invoke()`) copied verbatim.

### `AdhanVolumeController` (rewritten)

```kotlin
class AdhanVolumeController(
    private val context: Context,
    private val player: Player,
) {
    internal var mediaSession: MediaSession? = null        // androidx.media3.session.MediaSession
        private set
    private var onMuteRequested: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            if (muted || volume <= 0) onMuteRequested?.invoke()
        }
    }

    fun setOnMuteRequestedListener(listener: () -> Unit)
    fun activate()    // addListener(playerListener); mediaSession = MediaSession.Builder(context, player).setId(TAG).build()
    fun deactivate()  // release + null session; removeListener(playerListener); idempotent
}
```

`MediaSession.Builder(context, player)` requires a Media3 `Player`; `FakePlayer`
satisfies this in tests. `TAG = "AdhanVolumeController"`.

### Dependency injection

- `PrayerNotificationsModule` gains a Koin provider for the player:
  `single { ExoPlayer.Builder(androidXContext).build() }` (concrete mechanism finalized in plan).
- `AdhanPlayer` injects it as the `Player` dependency. Tests construct
  `AdhanPlayer(context, FakePlayer(...))` directly.

### Version catalog / module build file

`gradle/libs.versions.toml`:
- versions: `media3 = "1.11.0"`
- libraries:
  - `media3-common = androidx.media3:media3-common`
  - `media3-exoplayer = androidx.media3:media3-exoplayer`
  - `media3-session = androidx.media3:media3-session`
  - `media3-test-utils = androidx.media3:media3-test-utils`
- **remove** `androidx-media` (`androidx.media:media:1.8.0`)

`prayer_notifications/build.gradle.kts`:
- replace `implementation(libs.androidx.media)` with the three Media3 implementation deps
- add `testImplementation(libs.androidx.media3.test.utils)`

## Behavior Contract (parity)

| Behavior | Before (deprecated) | After (Media3) |
|----------|--------------------|----------------|
| Play on ALARM stream | `MediaPlayer` + `setAudioAttributes(USAGE_ALARM)` | `ExoPlayer.setAudioAttributes(USAGE_ALARM, false)` |
| Playback volume % | `setVolume(pct/100f)` | `player.volume = pct/100f` |
| Hardware keys → ALARM stream | `VolumeProviderCompat.onAdjustVolume` → `AudioManager.adjustStreamVolume(ALARM)` | `MediaSession` → `player.adjustDeviceVolume(flags)` → sink maps USAGE_ALARM→STREAM_ALARM |
| Completion cleanup + notify | `setOnCompletionListener` | `Player.Listener.STATE_ENDED` |
| Permanent focus loss stops + notifies | `OnAudioFocusChangeListener` | unchanged (manual focus) |
| Transient focus loss keeps playing | ignore transient | unchanged (manual focus) |
| Mute press stops adhan | LOWER at floor(1) → `onMuteRequested` | `AdhanService` ContentObserver, min-aware: `getStreamVolume(ALARM) <= getStreamMinVolume(ALARM)` → `stopSelf()` (post-migration fix; see below) |
| Volume-0 backstop stop | `AdhanService` ContentObserver | `AdhanService` ContentObserver, now min-aware (supersedes the `<= 0` backstop) |

## Testing Strategy

All new/changed tests are written RED first (TDD), then implemented to green.

**`AdhanVolumeControllerTest` (rewritten, FakePlayer + Robolectric):**
- `activate()` creates a non-null `mediaSession`; `deactivate()` releases it and nulls it
- double `activate()` keeps a single session; `deactivate()` idempotent; deactivate-before-activate safe
- mute listener fires on `onDeviceVolumeChanged(0, …)` and `(v, true)`; does NOT fire on `(85, false)`

**`AdhanPlayerTest` (rewritten, FakePlayer + Robolectric):**
- `play` sets one media item, applies `volume` fraction, sets `playWhenReady`, prepares
- `play` requests audio focus on alarm stream (`STREAM`/usage assert via Robolectric shadow)
- `play` activates the volume session; `stop` deactivates and abandons focus
- `stop` idempotent; repeated `play` replaces the media item and does not throw
- completion: drive FakePlayer into `STATE_ENDED` → completion listener fires + session released
- permanent focus loss → `onFocusLoss`; transient loss → keeps playing
- mute: fire `onDeviceVolumeChanged(0, true)` → `stop` + `onFocusLoss`
- non-mute: `onDeviceVolumeChanged(85, false)` → keeps playing

**Unchanged (verify still compile/pass):** `AdhanServiceTest`, `AlarmReceiverTest`,
`NotificationSettingsDataStoreTest`, scheduler + domain suites. If a test constructs
`AdhanPlayer` directly, update its constructor call to pass a `FakePlayer`.

## Risks & Mitigations

1. **Hardware volume-key capture by a bare `MediaSession`** (created directly, not inside a
   `MediaSessionService`). Media3 routes hardware keys to a session with a connected/active
   controller pathway; this app's `AdhanService` is a plain `Service`. **Mitigation:**
   on-device verification step is mandatory in the plan; the documented fallback is an
   in-process `MediaController` bridge (Approach 2) if native capture fails on device.
   > **Materialized (2026-08-29):** the bare `MediaSession` does not deliver volume-key
   > events, and `onDeviceVolumeChanged` never fires on key presses. Resolved without a
   > `MediaController` bridge by observing the ALARM stream via the min-aware
   > `AdhanService` ContentObserver — see "Post-Migration Fix" below.
2. **Device-volume → ALARM-stream mapping** relies on `USAGE_ALARM` audio attributes.
   Confirmed in Media3 audio-sink semantics; still verified on-device.
3. **FakePlayer under Robolectric + JUnit 5** threading quirks. Mitigation: pump the main
   looper (`shadowOf(Looper.getMainLooper()).idle()`) and configure FakePlayer with a
   main-thread executor in tests.
4. **OEM variance** on volume-panel UI (pre-existing, carried forward unchanged).

## Migration Phases (implementation plan will detail)

1. Create branch `dev/feature/media3-migration` (done)
2. Dependency swap (Media3 1.11.0 in, compat out); verify `:prayer_notifications:compileDebugKotlin`
3. `AdhanVolumeController` rewrite, RED → GREEN
4. Koin `ExoPlayer` provider; `AdhanPlayer` rewrite, RED → GREEN
5. Remove `ShadowMediaPlayer`/compat remnants; run full module suite;
   `gitnexus_detect_changes`; manual device checklist

---

## Post-Migration Fix (2026-08-29): mute-press-stops-adhan regression

**Status:** Fixed.

**Symptom:** After the Media3 migration, pressing the hardware volume key down to the ALARM
floor (0 or 1) muted the adhan but no longer stopped it.

**Root cause:** In Media3/ExoPlayer, `Player.Listener.onDeviceVolumeChanged` is dispatched only
when the audio output device routing changes (headphones/Bluetooth), via
`AudioTrack.OnRoutingChangedListener` in `DefaultAudioSink`. It is **not** fired on hardware
volume-key presses. The pre-migration `VolumeProviderCompat.onAdjustVolume` (which received
volume-key events through the active `MediaSessionCompat`) was what detected the "LOWER press
at floor" gesture; that mechanism is gone.

**Why the obvious fix is unavailable:** `AudioManager.AudioVolumeGroupCallback` +
`registerAudioVolumeGroupCallback` (API 30+) — the natural "observe the ALARM stream volume"
API — are `@SystemApi` and not callable from a regular app (verified against the public
`android.jar`; the compiler rejects them).

**Fix (public API):** The existing `AdhanService` `volume_alarm_sound` ContentObserver is the
public-API way to observe ALARM-stream volume changes. It previously stopped only at
`getStreamVolume(STREAM_ALARM) <= 0`; on devices where the ALARM minimum is 1, "down to 1"
never stopped the adhan. The observer now stops when the volume reaches the stream floor:
`getStreamVolume(stream) <= getStreamMinVolume(stream)`, extracted as
`isStreamAtFloor(audioManager, streamType)` in `AdhanService.kt` (API-guarded for 26–27).

**Follow-up (same day): observe the Media/Music stream too.** On-device testing on a
Xiaomi/Redmi/POCO device showed the volume panel displays **Media/Music** (not Alarm) while the
adhan plays, and the adhan volume follows the MUSIC stream — the active `MediaSession`
associates playback with media, so the volume key adjusts MUSIC and the ALARM stream never
changes. The observer therefore registers on **both** `volume_alarm_sound` and
`volume_music_sound` and stops when **either** stream reaches its floor. This keeps stock
devices (adhan on ALARM) working while fixing OEMs that route the key to MUSIC.

**Follow-up (same day, round 2): floor = `max(min, 1)` + polling fallback.** On the same
Xiaomi device the panel then showed **Alarm**, but the volume stopped at 1 (not 0) and the
adhan kept playing. Two issues surfaced: (1) `getStreamMinVolume(STREAM_ALARM)` can report 0
even though the volume key cannot go below 1, so `volume <= min` never fired — the floor is
now `max(getStreamMinVolume(stream), 1)`; (2) the `volume_alarm_sound` ContentObserver is not
guaranteed to fire on all OEMs, so a 200 ms polling loop (`startVolumePolling`, cancelled in
`onDestroy`) was added as a reliable fallback that checks both streams.

**Scope deviation:** `AdhanService.kt` was originally out of scope ("untouched"); this fix
modifies its `volumeObserver` (now observing ALARM + MUSIC), adds `isStreamAtFloor` and the
polling fallback. `AdhanVolumeController`'s `onDeviceVolumeChanged` listener is retained as a
weak fallback (fires only on routing changes); it is not the mute mechanism.

**Known limitation:** the ContentObserver fires only when the setting value changes. If the
adhan starts while the relevant stream volume is already at the floor, a no-op "press down at
floor" does not change the value and so does not stop playback (the old gesture detection is
not reproducible via public API without a `MediaSessionService`). The polling fallback does not
fully close this gap either (it only observes the current value, not the key gesture).

**Tests:** `AdhanServiceTest` covers `isStreamAtFloor` for both `STREAM_ALARM` and
`STREAM_MUSIC` at/above minimum (min=1 via mocked `AudioManager`), the reported-min-0 /
volume-1 case, plus service-level tests: `volume to zero stops the service`,
`music volume at minimum stops the service`, and `volume decrease to non-zero does not stop
the service` (Robolectric min = 0).