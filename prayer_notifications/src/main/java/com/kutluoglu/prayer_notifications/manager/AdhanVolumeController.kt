package com.kutluoglu.prayer_notifications.manager

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.kutluoglu.prayer_notifications.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
                .setSessionActivity(sessionActivity())
                .setCallback(muteCallback())
                .setCustomLayout(listOf(muteButton()))
                .build()
            player.addListener(playerListener)
        }
    }

    fun deactivate() {
        mediaSession?.release()
        mediaSession = null
        player.removeListener(playerListener)
    }

    private fun sessionActivity(): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun muteButton(): CommandButton =
        CommandButton.Builder()
            .setDisplayName(displayName())
            .setIconResId(iconResId())
            .setSessionCommand(SessionCommand(ACTION_MUTE_ADHAN, Bundle.EMPTY))
            .build()

    private fun muteCallback() = object : MediaSession.Callback {
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.ConnectionResult> {
            val connectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    SessionCommands.Builder()
                        .add(SessionCommand(ACTION_MUTE_ADHAN, Bundle.EMPTY))
                        .build()
                )
                .build()
            return Futures.immediateFuture(connectionResult)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_MUTE_ADHAN) {
                onMuteRequested?.invoke()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private fun displayName(): String = context.getString(R.string.notification_mute)

    private fun iconResId(): Int = R.drawable.ic_notification_mute

    private companion object {
        const val ACTION_MUTE_ADHAN = "com.kutluoglu.prayer_notifications.MUTE_ADHAN"
        const val ID_PREFIX = "AdhanVolumeController-"
    }
}