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
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_IS_JUMUAH = "extra_is_jumuah"
        const val EXTRA_NEXT_PRAYER_TIME = "extra_next_prayer_time"
        const val EXTRA_NEXT_PRAYER_NAME = "extra_next_prayer_name"
        const val EXTRA_PRE_PRAYER_MINUTES = "extra_pre_prayer_minutes"
        const val EXTRA_DAILY_SUMMARY = "extra_daily_summary"
        const val EXTRA_SPECIAL_DAY = "extra_special_day"
        const val EXTRA_COUNTDOWN_TARGET = "extra_countdown_target"
        const val EXTRA_COUNTDOWN_PRAYER_NAME = "extra_countdown_prayer_name"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
        const val ACTION_COUNTDOWN_TICK = "COUNTDOWN_TICK"
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
