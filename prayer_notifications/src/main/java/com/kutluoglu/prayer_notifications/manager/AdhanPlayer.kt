package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var onCompletion: (() -> Unit)? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            stop()
            onFocusLoss?.invoke()
        }
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
        val player = MediaPlayer()
        try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$resId"))
                setOnCompletionListener {
                    abandonAudioFocus()
                    onCompletion?.invoke()
                }
                prepare()
                start()
                setVolume(volume, volume)
            }
            mediaPlayer = player
            requestAudioFocus()
        } catch (e: Exception) {
            runCatching { player.release() }
            Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
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
