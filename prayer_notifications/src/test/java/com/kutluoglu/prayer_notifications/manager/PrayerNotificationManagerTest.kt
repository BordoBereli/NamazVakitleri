package com.kutluoglu.prayer_notifications.manager

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale

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
    fun `createChannels uses localized channel names`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = shadowOf(nm).notificationChannels
            assertThat(channels.first { it.id == "prayer_alerts" }.name).isEqualTo("Namaz vakitleri")
            assertThat(channels.first { it.id == "adhan" }.name).isEqualTo("Ezan")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `showPrayerNotification localizes prayer name and content`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            manager.showPrayerNotification("Fajr", NotificationSettings())
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("İmsak")
            assertThat(notification.extras.getString("android.text")).isEqualTo("İmsak vakti geldi")
        } finally {
            Locale.setDefault(original)
        }
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
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.showPrayerNotification("Fajr", NotificationSettings(adhanEnabled = true))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("adhan")

        manager.showPrayerNotification("Fajr", NotificationSettings(adhanEnabled = false))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }

    @Test
    fun `showPrayerNotification takes a prayer name`() {
        manager.createChannels()
        manager.showPrayerNotification("Fajr", NotificationSettings(adhanEnabled = false))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }

    @Test
    fun `showJumuahNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showJumuahNotification()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showPrePrayerNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showPrePrayerNotification("Fajr", 15)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showDailyReminderNotification posts summary on reminders channel`() {
        manager.createChannels()
        manager.showDailyReminderNotification("Fajr 04:30 · Dhuhr 12:30")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.channelId).isEqualTo("reminders")
        assertThat(notification.extras.getString("android.text")).isEqualTo("Fajr 04:30 · Dhuhr 12:30")
    }

    @Test
    fun `showSpecialDayNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showSpecialDayNotification(SpecialDay.EID_AL_FITR)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showPreSpecialDayNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showPreSpecialDayNotification(SpecialDay.EID_AL_ADHA)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }
}
