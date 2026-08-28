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
        controller.volumeProvider.onSetVolumeTo(3)
        assertThat(audioManager().getStreamVolume(AudioManager.STREAM_ALARM)).isEqualTo(3)
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
