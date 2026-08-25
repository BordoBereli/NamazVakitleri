package com.kutluoglu.prayer_notifications.manager

import android.content.Context
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
}
