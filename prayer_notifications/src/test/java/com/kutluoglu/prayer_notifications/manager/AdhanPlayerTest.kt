package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.R
import java.io.IOException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `play and stop do not throw`() {
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        player.stop()
    }

    @Test
    fun `play does not throw when playback fails`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, uri), IOException("boom"))
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        player.stop()
    }

    @Test
    fun `stop is idempotent`() {
        val player = AdhanPlayer(context)
        player.play("Dhuhr", 30)
        player.stop()
        player.stop()
    }

    @Test
    fun `repeated play calls do not throw`() {
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        player.play("Isha", 30)
        player.stop()
    }

    @Test
    fun `completion listener is invoked when playback completes`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        var createdPlayer: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
        val player = AdhanPlayer(context)
        var completed = false
        player.setOnCompletionListener { completed = true }
        player.play("Fajr", 30)
        val mediaPlayer = createdPlayer ?: error("no player created")
        shadowOf(mediaPlayer).invokeCompletionListener()
        assertThat(completed).isTrue()
    }

    @Test
    fun `play applies volume as a fraction`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        var createdPlayer: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        val mediaPlayer = createdPlayer ?: error("no player created")
        assertThat(shadowOf(mediaPlayer).getLeftVolume()).isEqualTo(0.3f)
        assertThat(shadowOf(mediaPlayer).getRightVolume()).isEqualTo(0.3f)
    }

    @Test
    fun `play requests audio focus on alarm stream`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest()
        assertThat(request).isNotNull()
        assertThat(request.durationHint).isEqualTo(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        assertThat(request.audioFocusRequest.audioAttributes.usage)
            .isEqualTo(AudioAttributes.USAGE_ALARM)
        player.stop()
    }

    @Test
    fun `stop abandons audio focus`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        player.stop()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertThat(shadowOf(audioManager).getLastAbandonedAudioFocusRequest()).isNotNull()
    }

    @Test
    fun `permanent focus loss invokes focus loss listener`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        val player = AdhanPlayer(context)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest() ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertThat(focusLost).isTrue()
    }

    @Test
    fun `transient focus loss does not stop playback`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        var createdPlayer: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
        val player = AdhanPlayer(context)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val mediaPlayer = createdPlayer ?: error("no player created")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest() ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertThat(focusLost).isFalse()
        assertThat(shadowOf(mediaPlayer).isReallyPlaying()).isTrue()
    }
}
