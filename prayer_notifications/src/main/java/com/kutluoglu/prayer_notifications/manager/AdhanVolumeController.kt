package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.VolumeProviderCompat

class AdhanVolumeController(
    private val context: Context
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
            val session = MediaSessionCompat(context, TAG)
            session.setPlaybackToRemote(volumeProvider)
            session.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                    .build()
            )
            session.isActive = true
            mediaSession = session
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
