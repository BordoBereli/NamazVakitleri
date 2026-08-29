package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import java.util.UUID

internal class AdhanVolumeController(
    private val context: Context,
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
                .setId(ID_PREFIX + UUID.randomUUID())
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
        const val ID_PREFIX = "AdhanVolumeController-"
    }
}