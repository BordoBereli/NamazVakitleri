package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver
import org.koin.core.annotation.Single

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
            "Prayer times",
            NotificationManager.IMPORTANCE_HIGH,
            settings
        )
        createChannel(CHANNEL_ADHAN, "Adhan", NotificationManager.IMPORTANCE_HIGH, settings)
        createChannel(
            CHANNEL_COUNTDOWN,
            "Next prayer countdown",
            NotificationManager.IMPORTANCE_LOW,
            settings
        )
        createChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT, settings)
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
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(prayer.name)
            .setContentText("${prayer.name} time is now")
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
            .setContentTitle("Next prayer: ${nextPrayer.name}")
            .setContentText(formatRemaining(remainingMillis))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification)
    }

    fun cancelCountdown() {
        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
    }

    fun showTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test notification")
            .setContentText("Notifications are working")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_TEST, notification)
    }

    private fun formatRemaining(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
