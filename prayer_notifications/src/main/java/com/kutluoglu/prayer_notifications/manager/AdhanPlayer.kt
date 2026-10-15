package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

import java.util.Locale

@Single
class AdhanPlayer(
    private val context: Context,
    private val player: Player,
    private val resolver: AdhanResIdResolver,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var onCompletion: (() -> Unit)? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null

    /** The active media session while adhan playback is in progress, null otherwise. */
    val activeMediaSession: MediaSession?
        get() = volumeController.mediaSession

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

    fun play(prayerKey: String, volumePercent: Int, styleId: String? = null) {
        stop()
        val volume = volumePercent.coerceIn(0, 100) / 100f
        val resId = resolver.resolve(prayerKey, styleId)
        try {
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.parse("android.resource://${context.packageName}/$resId"))
                    .setMediaMetadata(mediaMetadata(prayerKey))
                    .build()
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

    private fun mediaMetadata(prayerKey: String): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(localizedPrayerName(prayerKey))
            .setArtist(localizedString(R.string.channel_adhan))
            .build()

    private fun localizedPrayerName(key: String): String = when (key) {
        "Imsak" -> localizedString(R.string.prayer_imsak)
        "Sunrise" -> localizedString(R.string.prayer_sunrise)
        "Dhuhr" -> localizedString(R.string.prayer_dhuhr)
        "Asr" -> localizedString(R.string.prayer_asr)
        "Maghrib" -> localizedString(R.string.prayer_maghrib)
        "Isha" -> localizedString(R.string.prayer_isha)
        else -> key
    }

    private fun localizedString(resId: Int): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.getDefault())
        return context.createConfigurationContext(config).getString(resId)
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