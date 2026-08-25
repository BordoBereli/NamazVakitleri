package com.kutluoglu.prayer_notifications.scheduler

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AdhanService : Service(), KoinComponent {

    companion object {
        private const val SETTING_VOLUME_ALARM = "volume_alarm_sound"
    }

    private val adhanPlayer: AdhanPlayer by inject()
    private val notificationManager: PrayerNotificationManager by inject()

    private val audioManager: AudioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private var lastAlarmVolume: Int = 0
    private var volumeObserverRegistered: Boolean = false

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < lastAlarmVolume) {
                stopSelf()
            }
            lastAlarmVolume = current
        }
    }

    override fun onCreate() {
        super.onCreate()
        adhanPlayer.setOnCompletionListener { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerKey = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)
        if (prayerKey == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        adhanPlayer.play(prayerKey, 30)
        startForeground(
            PrayerNotificationManager.NOTIFICATION_ID_ADHAN,
            notificationManager.buildAdhanNotification(prayerKey)
        )
        lastAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        registerVolumeObserver()
        return START_NOT_STICKY
    }

    private fun registerVolumeObserver() {
        if (volumeObserverRegistered) return
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_VOLUME_ALARM),
            false,
            volumeObserver
        )
        volumeObserverRegistered = true
    }

    private fun unregisterVolumeObserver() {
        if (!volumeObserverRegistered) return
        contentResolver.unregisterContentObserver(volumeObserver)
        volumeObserverRegistered = false
    }

    override fun onDestroy() {
        unregisterVolumeObserver()
        adhanPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
