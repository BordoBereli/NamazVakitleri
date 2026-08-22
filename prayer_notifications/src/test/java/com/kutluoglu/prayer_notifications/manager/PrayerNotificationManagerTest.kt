package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = PrayerNotificationManager(context)

    @Test
    fun `createChannels registers four channels`() {
        manager.createChannels()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = shadowOf(nm).notificationChannels
        assertThat(channels.map { it.id }).containsExactly(
            "prayer_alerts", "adhan", "countdown", "reminders"
        )
    }

    @Test
    fun `showTestNotification posts a notification`() {
        manager.createChannels()
        manager.showTestNotification()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isNotEmpty()
    }

    @Test
    fun `showPrayerNotification picks channel based on adhanEnabled`() {
        manager.createChannels()
        val prayer = Prayer(
            name = "Fajr",
            arabicName = "الفجر",
            time = LocalTime(4, 30),
            date = LocalDate(2026, 8, 22)
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.showPrayerNotification(prayer, NotificationSettings(adhanEnabled = true))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("adhan")

        manager.showPrayerNotification(prayer, NotificationSettings(adhanEnabled = false))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }
}
