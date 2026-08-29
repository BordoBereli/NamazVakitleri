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