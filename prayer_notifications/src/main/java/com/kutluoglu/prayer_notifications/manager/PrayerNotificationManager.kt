package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver
import org.koin.core.annotation.Single
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
            settings
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
        settings: NotificationSettings
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                enableVibration(settings.vibrationEnabled)
                if (!settings.soundEnabled) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerNotification(prayer: Prayer, settings: NotificationSettings) {
        val channel = if (settings.adhanEnabled) CHANNEL_ADHAN else CHANNEL_PRAYER_ALERTS
        val prayerName = localizedPrayerName(prayer.name)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(prayerName)
            .setContentText(localizedString(R.string.notification_prayer_time, prayerName))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID_PRAYER, builder.build())
    }

    fun showCountdownNotification(nextPrayer: Prayer, remainingMillis: Long) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                localizedString(R.string.notification_next_prayer, localizedPrayerName(nextPrayer.name))
            )
            .setContentText(formatRemaining(remainingMillis))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, localizedString(R.string.notification_stop), stopIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification)
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

    private fun localizedPrayerName(key: String): String = when (key) {
        "Fajr" -> localizedString(R.string.prayer_fajr)
        "Dhuhr" -> localizedString(R.string.prayer_dhuhr)
        "Asr" -> localizedString(R.string.prayer_asr)
        "Maghrib" -> localizedString(R.string.prayer_maghrib)
        "Isha" -> localizedString(R.string.prayer_isha)
        else -> key
    }

    private fun localizedString(resId: Int, vararg args: Any): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.getDefault())
        return context.createConfigurationContext(config).getString(resId, *args)
    }
}
