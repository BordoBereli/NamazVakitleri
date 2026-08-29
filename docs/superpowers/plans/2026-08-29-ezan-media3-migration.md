# Ezan (Adhan) Jetpack Media3 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the deprecated `MediaPlayer`/`MediaSessionCompat`/`VolumeProviderCompat`/`PlaybackStateCompat` adhan playback stack in `prayer_notifications` with **ExoPlayer + Media3 `MediaSession`** (1.11.0), preserving ALARM-stream play, percent volume, manual audio focus, completion cleanup, and mute-press-stops-adhan.

**Architecture:** `AdhanPlayer` takes an injected `androidx.media3.common.Player` (a Koin-provided `ExoPlayer`); `AdhanVolumeController` owns `MediaSession` create/release plus a `Player.Listener.onDeviceVolumeChanged` mute listener. Tests inject `androidx.media3.test.utils.FakePlayer`, which faithfully fires `STATE_ENDED` and `onDeviceVolumeChanged` events, replacing Robolectric `ShadowMediaPlayer`.

**Tech Stack:** Kotlin 2.2.20, Jetpack Media3 1.11.0 (`media3-common`, `media3-exoplayer`, `media3-session`, `media3-test-utils`), Koin annotations (KSP), Robolectric 4.14, JUnit (vintage via `useJUnitPlatform()`), Truth.

---

## Context & Verbatim Facts

- **Branch:** implement on `dev/feature/media3-migration` (already checked out, spec committed as `0fb5557`).
- **Module test command:**
  ```bash
  ./gradlew :prayer_notifications:testDebugUnitTest
  ```
- **Single test class:**
  ```bash
  ./gradlew :prayer_notifications:testDebugUnitTest --tests "com.kutluoglu.prayer_notifications.manager.AdhanVolumeControllerTest"
  ```
- The module uses `useJUnitPlatform()`. Robolectric tests use JUnit4 (vintage engine) with `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [35])`, `@Test` from `org.junit.Test`. **Keep this exact pattern.**
- `FakePlayer` is `@UnstableApi` → every file that constructs it must be annotated `@OptIn(androidx.media3.common.util.UnstableApi::class)`.
- `FakePlayer` construction requires a Looper thread → tests run on Robolectric's main thread (same as existing tests). It defaults to `bufferingDelayMs = 100`; pass `bufferingDelayMs = 0` so `prepare()` goes straight to `STATE_READY`.
- FakePlayer/SimpleBasePlayer dispatches player events via the main-looper queue → **after any `setPlaybackState(...)`, `setDeviceVolume(...)`, `setDeviceMuted(...)`, `setPlayerError(...)`, or `increaseDeviceVolume(...)` call, the test must call `shadowOf(Looper.getMainLooper()).idle()`** for the listener callbacks to run.
- Media3 1.11.0 volume flag constants (AudioManager-aligned, no `VOLUME_FLAG_NONE`): `C.VOLUME_FLAG_SHOW_UI` (=1) is the right flag to pass in tests for a concrete audio-manager callback. `FakePlayer` ignores the flag (all commands enabled via `addAllCommands()`).
- Device-volume event semantics on `FakePlayer` (extends `SimpleBasePlayer`): `setDeviceVolume(v, flags)` fires `onDeviceVolumeChanged(v, muted)` only when `volume` differs; `setDeviceMuted(m, flags)` fires `(currentVolume, m)`; `increaseDeviceVolume`/`decreaseDeviceVolume` adjust by 1. Initial device volume is **0**.
- `androidx.media:media` (`libs.androidx.media`, `androidxMedia = "1.8.0"`) is imported ONLY by `AdhanVolumeController.kt` — after its rewrite it can be removed.
- `AdhanVolumeControllerTest.kt` and `AdhanPlayerTest.kt` are the ONLY tests constructing the controller/player. `AdhanServiceTest`, `AlarmReceiverTest`, `DailyRescheduleWorkerTest`, `PrayerNotificationManagerTest`, `NotificationSettingsDataStoreTest` do NOT touch them (scheduler tests `mockk<AdhanPlayer>(relaxed = true)`).
- The Koin `@Single` provider-function pattern inside the already-`@ComponentScan`'d `PrayerNotificationsModule` is proven (`provideNotificationSettingsDataStore`). Adding `providePlayer` needs no new wiring; the `:app` `@KoinApplication` picks it up via KSP-generated rules.
- `ExoPlayer.Builder.setAudioAttributes(androidx.media3.common.AudioAttributes, boolean)` exists in 1.11.0 (verified at `ExoPlayer.java:811`).
- `Player.Listener.onPlaybackStateChanged(Int)` fires (queued) via `FakePlayer.setPlaybackState(Player.STATE_ENDED)`.
- `PlaybackException(message: String?, cause: Throwable?, errorCode: Int)` (`@UnstableApi`) + `PlaybackException.ERROR_CODE_IO_UNSPECIFIED` exist; `FakePlayer.setPlayerError(...)` fires the listener's `onPlayerError`.

---

## File Map

| File | Action |
|------|--------|
| `gradle/libs.versions.toml` | Modify: add `media3 = "1.11.0"` + 4 libs; remove `androidxMedia`/`androidx-media` |
| `prayer_notifications/build.gradle.kts` | Modify: swap `libs.androidx.media` → 3 media3 impl deps + `libs.media3.test.utils` |
| `prayer_notifications/.../manager/AdhanVolumeController.kt` | Rewrite (session lifecycle + muted listener) |
| `prayer_notifications/.../manager/AdhanPlayer.kt` | Rewrite (injected `Player`) |
| `prayer_notifications/.../di/PrayerNotificationsModule.kt` | Modify: add `@Single providePlayer(context): Player` |
| `prayer_notifications/.../manager/AdhanVolumeControllerTest.kt` | Rewrite (FakePlayer-based) |
| `prayer_notifications/.../manager/AdhanPlayerTest.kt` | Rewrite (FakePlayer-based; drop `ShadowMediaPlayer`) |

Decisions locked in the spec (`docs/superpowers/specs/2026-08-29-ezan-media3-migration-design.md`): manual audio focus retained (`handleAudioFocus = false`), mute semantics = `player.onDeviceVolumeChanged(volume, muted)` → fire when `muted || volume <= 0`, `AdhanService.kt` **untouched**.

---

## Task 1: Add Media3 1.11.0 dependencies (additive, no code change)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `prayer_notifications/build.gradle.kts`

- [ ] **Step 1: Add the version and library entries**

In `gradle/libs.versions.toml` `[versions]`, after the `androidxMedia = "1.8.0"` line (line 35), add:

```toml
media3 = "1.11.0"
```

In `[libraries]`, after the `androidx-media` line (line 86), add:

```toml
media3-common = { module = "androidx.media3:media3-common", version.ref = "media3" }
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
media3-test-utils = { module = "androidx.media3:media3-test-utils", version.ref = "media3" }
```

Do **not** remove `androidx-media` yet — the current `AdhanVolumeController.kt` still imports it.

- [ ] **Step 2: Add the dependencies to the module build file**

In `prayer_notifications/build.gradle.kts`, after line 58 (`implementation(libs.androidx.media)`), add:

```kotlin
    implementation(libs.media3.common)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
```

After line 81 (`testImplementation(libs.robolectric)`), add:

```kotlin
    testImplementation(libs.media3.test.utils)
```

- [ ] **Step 3: Verify additive build stays green**

Run:
```bash
./gradlew :prayer_notifications:compileDebugKotlin :prayer_notifications:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL (both compile classes and unit tests pass unchanged).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml prayer_notifications/build.gradle.kts
git commit -m "build: add Jetpack Media3 1.11.0 dependencies"
```

---

## Task 2: Rewrite `AdhanVolumeController` and `AdhanPlayer` to Media3 (TDD)

The two files are compile-coupled (the new controller requires a `Player`, and only `AdhanPlayer` can produce it), so they are rewritten in one GREEN commit after writing both test files RED.

**Files:**
- Rewrite (test): `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeControllerTest.kt`
- Rewrite (test): `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`
- Rewrite (main): `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeController.kt`
- Rewrite (main): `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Modify (main): `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/di/PrayerNotificationsModule.kt`
- Modify: `prayer_notifications/build.gradle.kts`

### RED

- [ ] **Step 1: Replace `AdhanVolumeControllerTest.kt` with the new Media3 test**

Write the **entire** file to `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeControllerTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.FakePlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanVolumeControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun newController(player: FakePlayer = FakePlayer(bufferingDelayMs = 0)) =
        AdhanVolumeController(context, player)

    @Test
    fun `activate creates a media session and deactivate releases it`() {
        val controller = newController()
        controller.activate()
        assertThat(controller.mediaSession).isNotNull()
        controller.deactivate()
        assertThat(controller.mediaSession).isNull()
    }

    @Test
    fun `double activate keeps a single session`() {
        val controller = newController()
        controller.activate()
        val first = controller.mediaSession
        controller.activate()
        assertThat(controller.mediaSession).isSameInstanceAs(first)
        controller.deactivate()
    }

    @Test
    fun `deactivate is idempotent`() {
        val controller = newController()
        controller.activate()
        controller.deactivate()
        controller.deactivate()
        assertThat(controller.mediaSession).isNull()
    }

    @Test
    fun `deactivate before activate does not throw`() {
        val controller = newController()
        controller.deactivate()
        controller.activate()
        assertThat(controller.mediaSession).isNotNull()
        controller.deactivate()
    }

    @Test
    fun `muted device volume invokes the mute listener`() {
        val player = FakePlayer(bufferingDelayMs = 0)
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isTrue()
    }

    @Test
    fun `volume reaching zero invokes the mute listener`() {
        val player = FakePlayer(bufferingDelayMs = 0)
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceVolume(5, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        player.setDeviceVolume(0, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isTrue()
    }

    @Test
    fun `unmuted positive volume does not invoke the mute listener`() {
        val player = FakePlayer(bufferingDelayMs = 0)
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceVolume(85, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isFalse()
    }

    @Test
    fun `listener is not attached until activate`() {
        val player = FakePlayer(bufferingDelayMs = 0)
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        player.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isFalse()
        controller.activate()
        player.setDeviceMuted(false, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        player.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isTrue()
    }

    @Test
    fun `listener is removed after deactivate`() {
        val player = FakePlayer(bufferingDelayMs = 0)
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceVolume(5, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        controller.deactivate()
        player.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isFalse()
    }
}
```

- [ ] **Step 2: Replace `AdhanPlayerTest.kt` with the new Media3 test**

Write the **entire** file to `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.FakePlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun adhanPlayer(fakePlayer: FakePlayer = FakePlayer(bufferingDelayMs = 0)) =
        AdhanPlayer(context, fakePlayer)

    private fun fajrUri(): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")

    private fun audioManager(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Test
    fun `play and stop do not throw`() {
        val player = adhanPlayer()
        player.play("Fajr", 30)
        player.stop()
    }

    @Test
    fun `stop is idempotent`() {
        val player = adhanPlayer()
        player.play("Dhuhr", 30)
        player.stop()
        player.stop()
    }

    @Test
    fun `repeated play calls do not throw`() {
        val player = adhanPlayer()
        player.play("Fajr", 30)
        player.play("Isha", 30)
        player.stop()
    }

    @Test
    fun `play prepares a media item with alarm volume fraction`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        player.play("Fajr", 30)
        idleMainLooper()
        assertThat(fakePlayer.mediaItemCount).isEqualTo(1)
        assertThat(fakePlayer.currentMediaItem?.localConfiguration?.uri).isEqualTo(fajrUri())
        assertThat(fakePlayer.volume).isEqualTo(0.3f)
        assertThat(fakePlayer.playWhenReady).isTrue()
        assertThat(fakePlayer.playbackState).isEqualTo(Player.STATE_READY)
        player.stop()
    }

    @Test
    fun `play requests audio focus on alarm stream`() {
        val player = adhanPlayer()
        player.play("Fajr", 30)
        val request = shadowOf(audioManager()).getLastAudioFocusRequest()
        assertThat(request).isNotNull()
        assertThat(request?.durationHint).isEqualTo(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        assertThat(request?.audioFocusRequest?.audioAttributes?.usage)
            .isEqualTo(AudioAttributes.USAGE_ALARM)
        player.stop()
    }

    @Test
    fun `stop abandons audio focus`() {
        val player = adhanPlayer()
        player.play("Fajr", 30)
        player.stop()
        assertThat(shadowOf(audioManager()).getLastAbandonedAudioFocusRequest()).isNotNull()
    }

    @Test
    fun `play activates media session and stop deactivates it`() {
        val player = adhanPlayer()
        player.play("Fajr", 30)
        assertThat(player.volumeController.mediaSession).isNotNull()
        player.stop()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `completion invokes listener and releases session`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        var completed = false
        player.setOnCompletionListener { completed = true }
        player.play("Fajr", 30)
        fakePlayer.setPlaybackState(Player.STATE_ENDED)
        idleMainLooper()
        assertThat(completed).isTrue()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `playback error stops playback`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        player.play("Fajr", 30)
        fakePlayer.setPlayerError(
            PlaybackException("boom", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        )
        idleMainLooper()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `permanent focus loss invokes listener`() {
        val player = adhanPlayer()
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val request = shadowOf(audioManager()).getLastAudioFocusRequest()
            ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertThat(focusLost).isTrue()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `transient focus loss does not stop playback`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val request = shadowOf(audioManager()).getLastAudioFocusRequest()
            ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertThat(focusLost).isFalse()
        assertThat(fakePlayer.isPlaying).isTrue()
        player.stop()
    }

    @Test
    fun `mute stops playback and notifies listener`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        fakePlayer.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(focusLost).isTrue()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `volume reaching zero stops playback and notifies listener`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        fakePlayer.setDeviceVolume(5, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        fakePlayer.setDeviceVolume(0, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(focusLost).isTrue()
        assertThat(player.volumeController.mediaSession).isNull()
    }

    @Test
    fun `unmuted positive volume keeps playing`() {
        val fakePlayer = FakePlayer(bufferingDelayMs = 0)
        val player = adhanPlayer(fakePlayer)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        fakePlayer.setDeviceVolume(85, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(focusLost).isFalse()
        assertThat(player.volumeController.mediaSession).isNotNull()
        player.stop()
    }
}
```

- [ ] **Step 3: Run tests to verify RED**

Run:
```bash
./gradlew :prayer_notifications:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (main sources still compile against the old controller).

Then run:
```bash
./gradlew :prayer_notifications:testDebugUnitTest --tests "com.kutluoglu.prayer_notifications.manager.AdhanVolumeControllerTest"
```
Expected: FAIL — compilation error `Unresolved reference: constructor AdhanVolumeController(context, player)` (new ctor does not exist yet). This is the RED state.

### GREEN

- [ ] **Step 4: Rewrite `AdhanVolumeController.kt`**

Write the **entire** file to `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanVolumeController.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaSession

class AdhanVolumeController(
    context: Context,
    private val player: Player,
) {
    internal var mediaSession: MediaSession? = null
        private set

    private var onMuteRequested: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            if (muted || volume <= 0) {
                onMuteRequested?.invoke()
            }
        }
    }

    fun setOnMuteRequestedListener(listener: () -> Unit) {
        onMuteRequested = listener
    }

    fun activate() {
        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(context, player)
                .setId(TAG)
                .build()
            player.addListener(playerListener)
        }
    }

    fun deactivate() {
        mediaSession?.release()
        mediaSession = null
        player.removeListener(playerListener)
    }

    private companion object {
        const val TAG = "AdhanVolumeController"
    }
}
```

- [ ] **Step 5: Rewrite `AdhanPlayer.kt`**

Write the **entire** file to `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context,
    private val player: Player,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var onCompletion: (() -> Unit)? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null
    internal val volumeController = AdhanVolumeController(context, player).also { controller ->
        controller.setOnMuteRequestedListener {
            stop()
            onFocusLoss?.invoke()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                abandonAudioFocus()
                volumeController.deactivate()
                onCompletion?.invoke()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stop()
            Log.e("AdhanPlayer", "Failed to play adhan -> ${error.message}")
        }
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            stop()
            onFocusLoss?.invoke()
        }
    }

    init {
        player.addListener(playerListener)
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
        try {
            player.setMediaItem(
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$resId"))
            )
            player.volume = volume
            player.prepare()
            player.playWhenReady = true
            requestAudioFocus()
            volumeController.activate()
        } catch (e: Exception) {
            runCatching { player.stop() }
            volumeController.deactivate()
            Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
        }
    }

    fun stop() {
        runCatching { player.stop() }
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

- [ ] **Step 6: Add the Koin `ExoPlayer` provider**

Write the **entire** file to `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/di/PrayerNotificationsModule.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_notifications**")
object PrayerNotificationsModule {

    @Single
    fun provideNotificationSettingsDataStore(context: Context): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context)

    @Single
    fun providePlayer(context: Context): Player =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
}
```

- [ ] **Step 7: Remove the deprecated `androidx.media` dependency**

In `prayer_notifications/build.gradle.kts`, delete the line `implementation(libs.androidx.media)` (line 58). The three media3 dependencies added in Task 1 remain.

Verify nothing still references it:
```bash
grep -rn "androidx.media\.\|MediaSessionCompat\|VolumeProviderCompat\|PlaybackStateCompat" --include="*.kt" prayer_notifications/src
```
Expected: no matches.

- [ ] **Step 8: Run both new test classes to verify GREEN**

Run:
```bash
./gradlew :prayer_notifications:testDebugUnitTest --tests "com.kutluoglu.prayer_notifications.manager.AdhanVolumeControllerTest" --tests "com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"
```
Expected: BUILD SUCCESSFUL — all tests pass.

**If the `MediaSession` construction fails under Robolectric** (e.g. the mute/activate tests throw an `IllegalStateException` about a missing looper/service), fix by adding `idleMainLooper()` immediately after each `controller.activate()` and each `player.play(...)` in the tests; Robolectric needs the main-looper pump for the session's handler. Do not change production semantics.

**If test-utils compilation fails with unresolved `com.google.common.util.concurrent` symbols**, add `testImplementation(com.google.common:guava)` to the module and note it in the commit. (Expected not to be needed — our tests never reference the guava types in `FakePlayer`'s public surface.)

- [ ] **Step 9: Full module regression**

Run:
```bash
./gradlew :prayer_notifications:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL — includes `AdhanServiceTest`, `AlarmReceiverTest`, `PrayerNotificationSchedulerTest`, `DailyRescheduleWorkerTest`, `NotificationSettingsDataStoreTest`, `PrayerNotificationManagerTest` (they all either mock `AdhanPlayer` or never touch it).

- [ ] **Step 10: App compile**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (verifies the Koin-generated graph and `AdhanService`'s use of `AdhanPlayer` still resolve at compile time).

- [ ] **Step 11: Commit**

```bash
git add prayer_notifications/src
git add prayer_notifications/build.gradle.kts
git commit -m "refactor: migrate adhan playback from MediaPlayer/MediaSessionCompat to Media3 ExoPlayer + MediaSession"
```

---

## Task 3: Regression, impact check, and on-device verification

**Files:** none (verification only). The branch must be clean-merge-ready afterwards.

- [ ] **Step 1: Full project build + unit test suites**

Run:
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run GitNexus change detection**

Run (per AGENTS.md):
```
gitnexus_detect_changes()
```
Expected: the only affected paths are the four `prayer_notifications` files/branch in this plan (`AdhanPlayer`, `AdhanVolumeController`, `PrayerNotificationsModule`) and the two rewritten test files. `AdhanService` must appear **unaffected** (its blob is unchanged).

- [ ] **Step 3: Manual device checklist (mandatory — mitigates the spec's hardware-key risk)**

Install `:app` (debug) on a physical device and verify:
1. Trigger the test adhan (settings screen "test" action) — plays on the ALARM stream, audible via the volume panel.
2. While the adhan plays, press hardware volume up/down — volume changes and the ALARM stream is shown in the panel (Media3 maps `USAGE_ALARM` → `STREAM_ALARM`).
3. Press volume down until the minimum, then press once more (mute press) — adhan **stops** and the foreground notification disappears (mute-press-stops-adhan).
4. Play again; press volume down exactly to 0 — adhan stops (volume-0 backstop + mute listener).
5. Raise to max — adhan keeps playing, no spurious stop.
6. Let the raw audio file play to completion — playback stops itself, session released, notification cleared.
7. Receive an interruption (e.g. lock screen with another alarm/focus source) exercising permanent focus loss — adhan stops + notifies.

> **If hardware volume keys do NOT route to the ALARM stream via the bare `MediaSession`** (spec Risk #1 materializes): stop, and restore capture with the documented fallback — re-add an in-process `MediaController` that listens and forwards `setDeviceVolume`/`increaseDeviceVolume`/`decreaseDeviceVolume` to the player — then rely on the FakePlayer tests unchanged. File that as a follow-up task; do not change the already-green unit test contract.

- [ ] **Step 4: Convenience cleanup**

If the module still compiles with `SparkLint`/lint on the project, run:
```bash
./gradlew :prayer_notifications:lintDebug
```
Expected: no new errors introduced by this change.

- [ ] **Step 5: Final GitNexus check + commit readiness**

Re-run `gitnexus_detect_changes()` (scope = `all`) to confirm only expected symbols changed, then report the branch ready for review/PR (`main` → `dev/feature/media3-migration`).

---

## Self-Review Notes (completed inline)

- **Spec coverage:** every spec decision maps to a task — deps (T1), controller seam + mute semantics + session lifecycle (T2.4), injected Player + FakePlayer + ShadowMediaPlayer removal (T2.2/T2.5), Koin provider + `handleAudioFocus=false` + `USAGE_ALARM` (T2.6), parity behavior tests (T2.1/T2.2), manual device risk mitigations (T3.3). `AdhanService` untouched by design.
- **Placeholder scan:** all steps contain full replacement file contents; no TBD/TODO/"similar to"/"add error handling".
- **Type consistency:** `AdhanVolumeController(context, player)` appears identically in production (T2.4) and tests (T2.1); `AdhanPlayer(context, fakePlayer)` matches the new constructor (T2.5); `FakePlayer(bufferingDelayMs = 0)` used consistently; `C.VOLUME_FLAG_SHOW_UI` used for all device-volume triggers.