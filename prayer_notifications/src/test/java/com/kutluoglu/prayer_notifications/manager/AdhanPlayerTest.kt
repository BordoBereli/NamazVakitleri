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
import org.junit.After
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

    private fun adhanPlayer(fakePlayer: Player = FakePlayer(bufferingDelayMs = 0)) =
        AdhanPlayer(context, fakePlayer)

    @After
    fun flushPendingSessionWork() {
        idleMainLooper()
    }

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
        val fakePlayer = TestVolumePlayer()
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
        val fakePlayer = TestVolumePlayer()
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
        val fakePlayer = TestVolumePlayer()
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