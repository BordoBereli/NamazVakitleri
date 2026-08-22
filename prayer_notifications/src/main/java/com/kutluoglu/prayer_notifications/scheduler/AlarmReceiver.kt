package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
    }

    private val notificationManager: PrayerNotificationManager by inject()
    private val adhanPlayer: AdhanPlayer by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_COUNTDOWN -> notificationManager.cancelCountdown()
            else -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)
                if (prayerKey != null && !prayerKey.endsWith("_pre")) {
                    // Full prayer-time handling (post notification + adhan) is
                    // completed in Task 5.5 once use cases are wired.
                    notificationManager.showTestNotification()
                }
            }
        }
    }
}
