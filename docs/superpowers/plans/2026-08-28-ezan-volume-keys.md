# Ezan Hardware Volume Keys Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the phone hardware volume buttons adjust the Ezan (adhan) playback and alarm stream while the `AdhanService` is playing, by binding an active `MediaSessionCompat` + `VolumeProviderCompat` to `STREAM_ALARM`.

**Architecture:** A new `AdhanVolumeController` class owns a `MediaSessionCompat` and a `VolumeProviderCompat` (`VOLUME_CONTROL_RELATIVE`). The provider's `onAdjustVolume`/`onSetVolumeTo` route to `AudioManager.adjustStreamVolume`/`setStreamVolume` on `STREAM_ALARM`, which is the stream the `MediaPlayer` already plays on (`USAGE_ALARM`). `AdhanPlayer` activates the session when playback starts and deactivates+releases it on stop/completion/error. Active session + held audio focus causes the system volume keys to be forwarded to the session's `VolumeProviderCompat` instead of silently adjusting the unused `STREAM_MUSIC`. No changes to `AdhanService`; its existing `volume_alarm_sound` ContentObserver (stop on zero) keeps working as a side effect.

**Tech Stack:** Kotlin 2.2.20, `androidx.media:media` 1.8.0 (`MediaSessionCompat`, `VolumeProviderCompat`, `PlaybackStateCompat`), Robolectric 4.14 + MockK + Truth for tests.

---

## Impact Analysis (read before starting)

GitNexus reports `AdhanPlayer` as **LOW** risk — the only importer is `AdhanService` (an `IMPORTS` edge, no execution flows affected). `AdhanService` is **not modified** in this plan. The changes are additive: one new dependency, one new class, and internal wiring inside `AdhanPlayer` (no constructor/API changes, so `AdhanPlayerTest`, `AdhanServiceTest`, and `AlarmReceiverTest` compile unchanged). Run `gitnexus_impact` again in the worktree before Task 3 to confirm the baseline is unchanged.

---

### Task 1: Add the `androidx.media:media` dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `prayer_notifications/build.gradle.kts`

- [ ] **Step 1: Add the version and library to the version catalog**

In `gradle/libs.versions.toml`, add under `[versions]` (after `reorderable = "3.1.0"`):

```toml
androidxMedia = "1.8.0"
```

Add under `[libraries]` (after `androidx-work-runtime-ktx`):

```toml
androidx-media = { group = "androidx.media", name = "media", version.ref = "androidxMedia" }
```

- [ ] **Step 2: Add the dependency to the notifications module**

In `prayer_notifications/build.gradle.kts`, inside the `dependencies { }` block (after `implementation(libs.androidx.work.runtime.ktx)`):

```kotlin
implementation(libs.androidx.media)
```

- [ ] **Step 3: Verify the dependency resolves and compiles**

Run: `./gradlew :prayer_notifications:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml prayer_notifications/build.gradle.kts
git commit -m "chore(notifications): add androidx media dependency"
```

---

### Task 2: Create `AdhanVolumeController`

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeController.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeControllerTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `AdhanVolumeControllerTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanVolumeControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun audioManager(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Test
    fun `lowering volume key decreases alarm stream volume`() {
        audioManager().setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = AdhanVolumeController(context)
        controller.volumeProvider.onAdjustVolume(AudioManager.ADJUST_LOWER)
        assertThat(audioManager().getStreamVolume(AudioManager.STREAM_ALARM)).isEqualTo(4)
    }

    @Test
    fun `raising volume key increases alarm stream volume`() {
        audioManager().setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = AdhanVolumeController(context)
        controller.volumeProvider.onAdjustVolume(AudioManager.ADJUST_RAISE)
        assertThat(audioManager().getStreamVolume(AudioManager.STREAM_ALARM)).isEqualTo(6)
    }

    @Test
    fun `set volume to sets alarm stream volume`() {
        audioManager().setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = AdhanVolumeController(context)
        controller.volumeProvider.onSetVolumeTo(8)
        assertThat(audioManager().getStreamVolume(AudioManager.STREAM_ALARM)).isEqualTo(8)
    }

    @Test
    fun `activate creates an active media session and deactivate releases it`() {
        val controller = AdhanVolumeController(context)
        controller.activate()
        assertThat(controller.mediaSession?.isActive).isTrue()
        controller.deactivate()
        assertThat(controller.mediaSession).isNull()
    }

    @Test
    fun `deactivate is idempotent`() {
        val controller = AdhanVolumeController(context)
        controller.activate()
        controller.deactivate()
        controller.deactivate()
        assertThat(controller.mediaSession).isNull()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanVolumeControllerTest"`
Expected: COMPILATION FAILURE — `AdhanVolumeController` does not exist.

- [ ] **Step 3: Implement `AdhanVolumeController`**

Create `AdhanVolumeController.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioManager
import androidx.media.VolumeProviderCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class AdhanVolumeController(
    context: Context
) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    internal val volumeProvider: VolumeProviderCompat = object : VolumeProviderCompat(
        VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,
        MAX_VOLUME,
        audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    ) {
        override fun onAdjustVolume(direction: Int) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_ALARM,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }

        override fun onSetVolumeTo(volume: Int) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                volume.coerceIn(0, MAX_VOLUME),
                0
            )
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
    }

    internal var mediaSession: MediaSessionCompat? = null
        private set

    fun activate() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context, TAG).also { session ->
                session.setPlaybackToRemote(volumeProvider)
                session.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                        .build()
                )
                session.isActive = true
            }
        }
    }

    fun deactivate() {
        mediaSession?.let { session ->
            session.isActive = false
            session.release()
        }
        mediaSession = null
    }

    private companion object {
        const val TAG = "AdhanVolumeController"
        const val MAX_VOLUME = 100
    }
}
```

Notes:
- Namespace note (verified against the `androidx.media:media:1.8.0` AAR): `MediaSessionCompat`/`PlaybackStateCompat` live in the legacy `android.support.v4.media.session.*` package, but `VolumeProviderCompat` was repackaged and is `androidx.media.VolumeProviderCompat` (there is no `android.support.v4.media.VolumeProviderCompat` in 1.8.0).
- `internal` visibility lets the unit tests in the same module read `volumeProvider` and the session state without widening the public API.
- Robolectric supports this: `ShadowMediaSession` shadows the framework session, and `ShadowAudioManager.adjustStreamVolume` is implemented for `ADJUST_RAISE`/`ADJUST_LOWER`. If (unexpectedly) the `activate` test trips over a Robolectric/MediaSessionCompat limitation, keep the four volume tests and fall back to asserting `controller.activate()` does not throw.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanVolumeControllerTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeController.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeControllerTest.kt
git commit -m "feat(notifications): create AdhanVolumeController for hardware volume keys"
```

---

### Task 3: Wire the volume controller into `AdhanPlayer`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`

Run `gitnexus_impact({target: "AdhanPlayer", direction: "upstream"})` before editing (AGENTS.md requirement). Expected: LOW risk, `AdhanService` the only importer.

- [ ] **Step 1: Write the failing tests**

Add these three tests to `AdhanPlayerTest.kt` (after the last test, `transient focus loss does not stop playback`):

```kotlin
@Test
fun `play activates media session for volume keys`() {
    val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
    ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
    val player = AdhanPlayer(context)
    player.play("Fajr", 30)
    assertThat(player.volumeController.mediaSession?.isActive).isTrue()
    player.stop()
}

@Test
fun `stop deactivates the media session`() {
    val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
    ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
    val player = AdhanPlayer(context)
    player.play("Fajr", 30)
    player.stop()
    assertThat(player.volumeController.mediaSession).isNull()
}

@Test
fun `completion deactivates the media session`() {
    val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
    ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
    var createdPlayer: MediaPlayer? = null
    ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
    val player = AdhanPlayer(context)
    player.play("Fajr", 30)
    val mediaPlayer = createdPlayer ?: error("no player created")
    shadowOf(mediaPlayer).invokeCompletionListener()
    assertThat(player.volumeController.mediaSession).isNull()
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"`
Expected: COMPILATION FAILURE — `volumeController` does not exist on `AdhanPlayer`.

- [ ] **Step 3: Implement the wiring in `AdhanPlayer`**

In `AdhanPlayer.kt`:

1. Add the field (after `private var focusRequest: AudioFocusRequest? = null`):

```kotlin
internal val volumeController = AdhanVolumeController(context)
```

2. In `play()`, after `requestAudioFocus()` inside the `try` block:

```kotlin
        mediaPlayer = player
        requestAudioFocus()
        volumeController.activate()
```

3. In the `catch` block of `play()`, deactivate so a failed play never leaves a dangling active session:

```kotlin
    } catch (e: Exception) {
        runCatching { player.release() }
        volumeController.deactivate()
        Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
    }
```

4. In the completion listener inside `player.apply { }`, deactivate before invoking the callback:

```kotlin
            setOnCompletionListener {
                abandonAudioFocus()
                volumeController.deactivate()
                onCompletion?.invoke()
            }
```

5. In `stop()`, deactivate as part of the teardown (after the player is nulled):

```kotlin
    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
        volumeController.deactivate()
        abandonAudioFocus()
    }
```

The resulting `AdhanPlayer.kt` should read:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    internal val volumeController = AdhanVolumeController(context)

    private var mediaPlayer: MediaPlayer? = null
    private var onCompletion: (() -> Unit)? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            stop()
            onFocusLoss?.invoke()
        }
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletion = listener
    }

    fun setOnFocusLossListener(listener: () -> Unit) {
        onFocusLoss = listener
    }

    fun play(prayerKey: String, volumePercent: Int) {
        stop()
        val volume = volumePercent.coerceIn(0, 100) / 100f
        val resId = when (prayerKey) {
            "Fajr" -> R.raw.adhan_fajr
            "Dhuhr" -> R.raw.adhan_dhuhr
            "Asr" -> R.raw.adhan_asr
            "Maghrib" -> R.raw.adhan_maghrib
            "Isha" -> R.raw.adhan_isha
            else -> R.raw.adhan_fajr
        }
        val player = MediaPlayer()
        try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$resId"))
                setOnCompletionListener {
                    abandonAudioFocus()
                    volumeController.deactivate()
                    onCompletion?.invoke()
                }
                prepare()
                start()
                setVolume(volume, volume)
            }
            mediaPlayer = player
            requestAudioFocus()
            volumeController.activate()
        } catch (e: Exception) {
            runCatching { player.release() }
            volumeController.deactivate()
            Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
        volumeController.deactivate()
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
```

- [ ] **Step 4: Run the full AdhanPlayer test suite to verify**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"`
Expected: PASS (all existing tests plus the 3 new ones). The existing `play and stop do not throw` and `repeated play calls do not throw` tests double as smoke tests for the activate/deactivate lifecycle.

- [ ] **Step 5: Run the notifications module suite to confirm nothing else broke**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS (all tests, including `AdhanServiceTest`, `AlarmReceiverTest`, and `AdhanVolumeControllerTest`).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt
git commit -m "feat(notifications): route hardware volume keys to alarm stream during adhan"
```

---

## Final Verification

After all tasks are complete, run the affected suites and the change checks:

```bash
./gradlew :prayer_notifications:testDebugUnitTest
```

Expected: all tests green. Then run `gitnexus_detect_changes()` per AGENTS.md to confirm the diff only touches `AdhanPlayer.kt` (+ new `AdhanVolumeController.kt`, its test, the version catalog, and the module build file), affecting no execution flows beyond the adhan playback path.

Manual sanity check on a device: start an adhan, press volume up/down — the alarm-stream volume slider should appear and the ezan loudness should change. Lowering to zero should still stop the service (existing `AdhanService` behavior).

## Manual Risk Notes

- **OEM variance**: On some custom skins the volume-panel UI hides the alarm-stream slider, so key presses change the ezan (no visible slider) even though it works. This is expected and a known tradeoff of the alarm-stream approach.
- **Coexistence with other apps**: If another media app holds an active `MediaSession` AND both are "active" simultaneously, some devices may route keys to the other session. Holding `USAGE_ALARM` audio focus during the (transient) adhan favors our session in practice.
- **Volume→zero**: `VolumeProviderCompat` uses `ADJUST_LOWER`, so a single press lands at 1, not 0. Reaching 0 still triggers the existing `AdhanService` stop; this behavior is preserved, not newly introduced.