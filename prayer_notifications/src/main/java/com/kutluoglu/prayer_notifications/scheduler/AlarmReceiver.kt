package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        const val EXTRA_COUNTDOWN_PREVIOUS_TIME = "extra_countdown_previous_time"
        const val EXTRA_ALARM_TRIGGER_TIME = "extra_alarm_trigger_time"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
        const val ACTION_COUNTDOWN_TICK = "COUNTDOWN_TICK"
        const val ACTION_STOP_ADHAN = "STOP_ADHAN"
    }

    private val notificationManager: PrayerNotificationManager by inject()
    private val adhanPlayer: AdhanPlayer by inject()
    private val scheduler: PrayerNotificationScheduler by inject()
    private val dataStore: NotificationSettingsDataStore by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_COUNTDOWN -> scheduler.cancelCountdown()
            ACTION_COUNTDOWN_TICK -> {
                val target = intent.getLongExtra(EXTRA_COUNTDOWN_TARGET, 0L)
                val name = intent.getStringExtra(EXTRA_COUNTDOWN_PRAYER_NAME) ?: return
                val previous = if (intent.hasExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME)) {
                    intent.getLongExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME, 0L)
                } else {
                    null
                }
                scheduler.updateCountdown(target, name, previous)
            }
            else -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        handleAlarm(intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    internal suspend fun handleAlarm(intent: Intent) {
        val type = intent.getStringExtra(EXTRA_ALARM_TYPE)
            ?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }
            ?: return
        val settings = dataStore.getSettings()
        when (type) {
            AlarmType.PRAYER -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
                if (intent.getBooleanExtra(EXTRA_IS_JUMUAH, false) && settings.jumuahEnabled) {
                    notificationManager.showJumuahNotification()
                } else {
                    notificationManager.showPrayerNotification(prayerKey, settings)
                }
                if (settings.adhanEnabled) {
                    adhanPlayer.play(prayerKey)
                }
                if (settings.countdownEnabled) {
                    val nextTime = intent.getLongExtra(EXTRA_NEXT_PRAYER_TIME, 0L)
                    val nextName = intent.getStringExtra(EXTRA_NEXT_PRAYER_NAME)
                    val previous = if (intent.hasExtra(EXTRA_ALARM_TRIGGER_TIME)) {
                        intent.getLongExtra(EXTRA_ALARM_TRIGGER_TIME, 0L)
                    } else {
                        null
                    }
                    if (nextTime > 0L && nextName != null) {
                        scheduler.updateCountdown(nextTime, nextName, previous)
                    }
                }
            }
            AlarmType.PRE_PRAYER -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)?.removeSuffix("_pre") ?: return
                val minutes = intent.getIntExtra(EXTRA_PRE_PRAYER_MINUTES, 15)
                notificationManager.showPrePrayerNotification(prayerKey, minutes)
            }
            AlarmType.DAILY_REMINDER -> {
                val summary = intent.getStringExtra(EXTRA_DAILY_SUMMARY) ?: return
                notificationManager.showDailyReminderNotification(summary)
                scheduler.scheduleDailyReminder()
            }
            AlarmType.SPECIAL_DAY -> {
                val day = intent.getStringExtra(EXTRA_SPECIAL_DAY)
                    ?.let { runCatching { SpecialDay.valueOf(it) }.getOrNull() }
                    ?: return
                notificationManager.showSpecialDayNotification(day)
            }
            AlarmType.PRE_SPECIAL_DAY -> {
                val day = intent.getStringExtra(EXTRA_SPECIAL_DAY)
                    ?.let { runCatching { SpecialDay.valueOf(it) }.getOrNull() }
                    ?: return
                notificationManager.showPreSpecialDayNotification(day)
            }
            AlarmType.COUNTDOWN_TICK -> Unit
        }
    }
}
