package com.kutluoglu.prayer_notifications.scheduler

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AdhanService : Service(), KoinComponent {

    companion object {
        private const val SETTING_VOLUME_ALARM = "volume_alarm_sound"
        private const val SETTING_VOLUME_MUSIC = "volume_music_sound"
        private const val VOLUME_POLL_INTERVAL_MS = 200L
    }

    private val adhanPlayer: AdhanPlayer by inject()
    private val notificationDisplayer: NotificationDisplayer by inject()
    private val dataStore: NotificationSettingsDataStore by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager: AudioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private var volumeObserverRegistered: Boolean = false
    private var volumePollJob: Job? = null

    private val volumeUris = listOf(
        Settings.System.getUriFor(SETTING_VOLUME_ALARM),
        Settings.System.getUriFor(SETTING_VOLUME_MUSIC),
    )

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        private var alarmWasAtFloor = false
        private var musicWasAtFloor = false

        fun reset() {
            alarmWasAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)
            musicWasAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)
        }

        override fun onChange(selfChange: Boolean) {
            val alarmIsAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)
            val musicIsAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)
            if (shouldStopForVolumeFloor(
                    alarmWasAtFloor, alarmIsAtFloor, musicWasAtFloor, musicIsAtFloor
                )
            ) {
                stopSelf()
                return
            }
            alarmWasAtFloor = alarmIsAtFloor
            musicWasAtFloor = musicIsAtFloor
        }
    }

    override fun onCreate() {
        super.onCreate()
        adhanPlayer.setOnCompletionListener { stopSelf() }
        adhanPlayer.setOnFocusLossListener { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerKey = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)
        if (prayerKey == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(
            NotificationDisplayer.NOTIFICATION_ID_ADHAN,
            notificationDisplayer.buildAdhanNotification(prayerKey)
        )
        serviceScope.launch {
            val volume = dataStore.getSettings().adhanVolume
            adhanPlayer.play(prayerKey, volume)
        }
        registerVolumeObserver()
        startVolumePolling()
        return START_NOT_STICKY
    }

    private fun startVolumePolling() {
        volumePollJob?.cancel()
        volumePollJob = serviceScope.launch(Dispatchers.Default) {
            var alarmWasAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)
            var musicWasAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)
            while (isActive) {
                val alarmIsAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)
                val musicIsAtFloor = isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)
                if (shouldStopForVolumeFloor(
                        alarmWasAtFloor, alarmIsAtFloor, musicWasAtFloor, musicIsAtFloor
                    )
                ) {
                    stopSelf()
                    break
                }
                alarmWasAtFloor = alarmIsAtFloor
                musicWasAtFloor = musicIsAtFloor
                delay(VOLUME_POLL_INTERVAL_MS)
            }
        }
    }

    private fun registerVolumeObserver() {
        if (volumeObserverRegistered) return
        volumeObserver.reset()
        volumeUris.forEach { uri ->
            contentResolver.registerContentObserver(uri, false, volumeObserver)
        }
        volumeObserverRegistered = true
    }

    private fun unregisterVolumeObserver() {
        if (!volumeObserverRegistered) return
        contentResolver.unregisterContentObserver(volumeObserver)
        volumeObserverRegistered = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        volumePollJob?.cancel()
        unregisterVolumeObserver()
        adhanPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

internal fun isStreamAtFloor(audioManager: AudioManager, streamType: Int): Boolean {
    val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching { audioManager.getStreamMinVolume(streamType) }
            .getOrDefault(0)
    } else {
        0
    }
    val floor = maxOf(min, 1)
    return audioManager.getStreamVolume(streamType) <= floor
}

internal fun shouldStopForVolumeFloor(
    alarmWasAtFloor: Boolean,
    alarmIsAtFloor: Boolean,
    musicWasAtFloor: Boolean,
    musicIsAtFloor: Boolean,
): Boolean =
    (alarmIsAtFloor && !alarmWasAtFloor) || (musicIsAtFloor && !musicWasAtFloor)
