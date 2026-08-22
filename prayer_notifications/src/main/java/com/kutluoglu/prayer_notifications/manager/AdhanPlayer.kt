package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun play() {
        stop()
        val player = MediaPlayer()
        try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.adhan}"))
                prepare()
                start()
            }
            mediaPlayer = player
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
    }
}
