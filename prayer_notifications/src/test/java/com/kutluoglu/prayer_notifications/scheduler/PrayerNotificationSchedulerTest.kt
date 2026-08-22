package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)
    private val schedulePlan = SchedulePlan()

    private fun scheduler(scope: CoroutineScope) = PrayerNotificationScheduler(
        context = context,
        dataStore = dataStore,
        schedulePlan = schedulePlan,
        scope = scope
    )

    @Test
    fun `scheduleAll with disabled settings schedules nothing`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = false)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `cancelAll cancels scheduled alarms`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
        val pendingIntent = PendingIntent.getBroadcast(
            context, PrayerNotificationScheduler.REQUEST_CODE_START, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            pendingIntent
        )
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)

        scheduler.cancelAll()

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }
}
