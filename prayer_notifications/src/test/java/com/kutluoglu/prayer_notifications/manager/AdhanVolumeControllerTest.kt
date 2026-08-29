package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.FakePlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
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

    private fun newController(player: Player = FakePlayer(bufferingDelayMs = 0)) =
        AdhanVolumeController(context, player)

    @After
    fun flushPendingSessionWork() {
        idleMainLooper()
    }

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
        val player = TestVolumePlayer()
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceMuted(true, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isTrue()
        controller.deactivate()
    }

    @Test
    fun `volume reaching zero invokes the mute listener`() {
        val player = TestVolumePlayer()
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceVolume(5, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        player.setDeviceVolume(0, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isTrue()
        controller.deactivate()
    }

    @Test
    fun `unmuted positive volume does not invoke the mute listener`() {
        val player = TestVolumePlayer()
        val controller = newController(player)
        var muted = false
        controller.setOnMuteRequestedListener { muted = true }
        controller.activate()
        player.setDeviceVolume(85, C.VOLUME_FLAG_SHOW_UI)
        idleMainLooper()
        assertThat(muted).isFalse()
        controller.deactivate()
    }

    @Test
    fun `listener is not attached until activate`() {
        val player = TestVolumePlayer()
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
        controller.deactivate()
    }

    @Test
    fun `listener is removed after deactivate`() {
        val player = TestVolumePlayer()
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