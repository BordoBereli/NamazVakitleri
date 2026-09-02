package com.kutluoglu.prayer_notifications.manager

import android.app.Notification
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    fun `createChannels makes adhan channel silent when sound enabled`() {
        manager.createChannels(NotificationSettings(soundEnabled = true))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = shadowOf(nm).notificationChannels
        assertThat(channels.first { it.id == "adhan" }.sound).isNull()
        assertThat(channels.first { it.id == "prayer_alerts" }.sound).isNotNull()
    }

    @Test
    fun `showPrayerNotification localizes prayer name and content`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            manager.showPrayerNotification("Dhuhr", NotificationSettings())
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Öğle")
            assertThat(notification.extras.getString("android.text")).isEqualTo("Öğle vakti geldi")
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

        manager.showPrayerNotification("Dhuhr", NotificationSettings(adhanEnabled = true))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("adhan")

        manager.showPrayerNotification("Dhuhr", NotificationSettings(adhanEnabled = false))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }

    @Test
    fun `showPrayerNotification takes a prayer name`() {
        manager.createChannels()
        manager.showPrayerNotification("Dhuhr", NotificationSettings(adhanEnabled = false))
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
        manager.showPrePrayerNotification("Dhuhr", 15)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showDailyReminderNotification posts summary on reminders channel`() {
        manager.createChannels()
        manager.showDailyReminderNotification("Dhuhr 12:30 · Asr 16:00")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.channelId).isEqualTo("reminders")
        assertThat(notification.extras.getString("android.text")).isEqualTo("Dhuhr 12:30 · Asr 16:00")
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

    @Test
    fun `showCountdownNotification shows prayer name and clock time in title`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getString("android.title")).isEqualTo("Maghrib · 18:45")
    }

    @Test
    fun `showCountdownNotification shows remaining time in body`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getString("android.text")).isEqualTo("1h 30m remaining")
    }

    @Test
    fun `showCountdownNotification sets progress bar between previous and next`() {
        manager.createChannels()
        val previous = LocalTime.of(16, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val target = LocalTime.of(19, 55).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val gap = target - previous
        val now = previous + gap / 2
        manager.showCountdownNotification("Maghrib", target, previous, target - now)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX)).isEqualTo(gap.toInt())
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo((gap / 2).toInt())
    }

    @Test
    fun `showCountdownNotification omits progress bar when previous is null`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX)).isEqualTo(0)
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo(0)
    }

    @Test
    fun `buildAdhanNotification shows prayer name and playing text`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val notification = manager.buildAdhanNotification("Dhuhr")
            assertThat(notification.extras.getString("android.title")).isEqualTo("Öğle")
            assertThat(notification.extras.getString("android.text")).isEqualTo("Ezan çalıyor")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `buildAdhanNotification includes stop action and delete intent`() {
        manager.createChannels()
        val notification = manager.buildAdhanNotification("Dhuhr")
        assertThat(notification.actions).hasLength(1)
        assertThat(notification.actions!![0].title.toString()).isEqualTo("Stop")
        assertThat(notification.deleteIntent).isNotNull()
    }

    @Test
    fun `showCountdownNotification shows Cuma for Dhuhr on Friday`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(12, 30).atDate(LocalDate.of(2026, 8, 28)) // Friday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Dhuhr", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Cuma · 12:30")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `showCountdownNotification shows Dhuhr name for Dhuhr on Monday`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(12, 30).atDate(LocalDate.of(2026, 8, 24)) // Monday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Dhuhr", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Öğle · 12:30")
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `showCountdownNotification shows Maghrib on Friday for non-Dhuhr`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            manager.createChannels()
            val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 28)) // Friday
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = shadowOf(nm).allNotifications.single()
            assertThat(notification.extras.getString("android.title")).isEqualTo("Akşam · 18:45")
        } finally {
            Locale.setDefault(original)
        }
    }
}
