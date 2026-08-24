package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver
import org.koin.core.annotation.Single
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Single
class PrayerNotificationManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_PRAYER_ALERTS = "prayer_alerts"
        const val CHANNEL_ADHAN = "adhan"
        const val CHANNEL_COUNTDOWN = "countdown"
        const val CHANNEL_REMINDERS = "reminders"
        const val NOTIFICATION_ID_PRAYER = 1001
        const val NOTIFICATION_ID_COUNTDOWN = 1002
        const val NOTIFICATION_ID_TEST = 1003
        const val NOTIFICATION_ID_JUMUAH = 1004
        const val NOTIFICATION_ID_PRE_PRAYER = 1005
        const val NOTIFICATION_ID_DAILY_REMINDER = 1006
        const val NOTIFICATION_ID_SPECIAL_DAY = 1007
        const val NOTIFICATION_ID_PRE_SPECIAL_DAY = 1008
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels(settings: NotificationSettings = NotificationSettings()) {
        createChannel(
            CHANNEL_PRAYER_ALERTS,
            localizedString(R.string.channel_prayer_alerts),
            NotificationManager.IMPORTANCE_HIGH,
            settings
        )
        createChannel(
            CHANNEL_ADHAN,
            localizedString(R.string.channel_adhan),
            NotificationManager.IMPORTANCE_HIGH,
            settings,
            forceSilent = true
        )
        createChannel(
            CHANNEL_COUNTDOWN,
            localizedString(R.string.channel_countdown),
            NotificationManager.IMPORTANCE_LOW,
            settings
        )
        createChannel(
            CHANNEL_REMINDERS,
            localizedString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
            settings
        )
    }

    private fun createChannel(
        id: String,
        name: String,
        importance: Int,
        settings: NotificationSettings,
        forceSilent: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                enableVibration(settings.vibrationEnabled)
                if (!settings.soundEnabled || forceSilent) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerNotification(prayerName: String, settings: NotificationSettings) {
        val channel = if (settings.adhanEnabled) CHANNEL_ADHAN else CHANNEL_PRAYER_ALERTS
        val localizedName = localizedPrayerName(prayerName)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedName)
            .setContentText(localizedString(R.string.notification_prayer_time, localizedName))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID_PRAYER, builder.build())
    }

    fun showCountdownNotification(
        nextPrayerName: String,
        nextPrayerTimeMillis: Long,
        previousPrayerTimeMillis: Long?,
        remainingMillis: Long
    ) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_STOP_COUNTDOWN),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                localizedString(
                    R.string.notification_countdown_title,
                    localizedPrayerName(nextPrayerName),
                    formatClockTime(nextPrayerTimeMillis)
                )
            )
            .setContentText(
                localizedString(R.string.notification_remaining, formatRemaining(remainingMillis))
            )
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, localizedString(R.string.notification_stop), stopIntent)
        previousPrayerTimeMillis?.let { previous ->
            if (previous < nextPrayerTimeMillis) {
                val now = nextPrayerTimeMillis - remainingMillis
                val max = (nextPrayerTimeMillis - previous).toInt()
                val progress = (now - previous).coerceIn(0, max.toLong()).toInt()
                builder.setProgress(max, progress, false)
            }
        }
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, builder.build())
    }

    fun cancelCountdown() {
        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
    }

    fun showTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_test_title))
            .setContentText(localizedString(R.string.notification_test_body))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_TEST, notification)
    }

    fun showJumuahNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_jumuah_title))
            .setContentText(localizedString(R.string.notification_jumuah_body))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_JUMUAH, notification)
    }

    fun showPrePrayerNotification(prayerName: String, minutes: Int) {
        val localizedName = localizedPrayerName(prayerName)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedName)
            .setContentText(localizedString(R.string.notification_pre_prayer, minutes))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PRE_PRAYER, notification)
    }

    fun showDailyReminderNotification(summary: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_daily_reminder_title))
            .setContentText(summary)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    fun showSpecialDayNotification(day: SpecialDay) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_special_day_title))
            .setContentText(
                localizedString(R.string.notification_special_day_body, localizedSpecialDay(day))
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_SPECIAL_DAY, notification)
    }

    fun showPreSpecialDayNotification(day: SpecialDay) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_pre_special_day_title))
            .setContentText(
                localizedString(R.string.notification_pre_special_day_body, localizedSpecialDay(day))
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PRE_SPECIAL_DAY, notification)
    }

    private fun formatRemaining(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            localizedString(R.string.notification_remaining_hours_minutes, hours, minutes)
        } else {
            localizedString(R.string.notification_remaining_minutes, minutes)
        }
    }

    private fun formatClockTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }

    private fun localizedPrayerName(key: String): String = when (key) {
        "Fajr" -> localizedString(R.string.prayer_fajr)
        "Dhuhr" -> localizedString(R.string.prayer_dhuhr)
        "Asr" -> localizedString(R.string.prayer_asr)
        "Maghrib" -> localizedString(R.string.prayer_maghrib)
        "Isha" -> localizedString(R.string.prayer_isha)
        else -> key
    }

    private fun localizedSpecialDay(day: SpecialDay): String = when (day) {
        SpecialDay.RAMADAN_START -> localizedString(R.string.special_day_ramadan_start)
        SpecialDay.EID_AL_FITR -> localizedString(R.string.special_day_eid_al_fitr)
        SpecialDay.EID_AL_ADHA -> localizedString(R.string.special_day_eid_al_adha)
        SpecialDay.LAYLAT_AL_QADIR -> localizedString(R.string.special_day_laylat_al_qadr)
    }

    private fun localizedString(resId: Int, vararg args: Any): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.getDefault())
        return context.createConfigurationContext(config).getString(resId, *args)
    }
}
