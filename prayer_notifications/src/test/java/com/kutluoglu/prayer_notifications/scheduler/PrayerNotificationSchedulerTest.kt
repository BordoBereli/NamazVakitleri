package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)
    private val schedulePlan = SchedulePlan()

    @Test
    fun `scheduleAll with disabled settings schedules nothing`() = runTest {
        coEvery { dataStore.getSettings() } returns com.kutluoglu.prayer_notifications.domain.NotificationSettings(enabled = false)
        val scheduler = PrayerNotificationScheduler(
            context = context,
            dataStore = dataStore,
            schedulePlan = schedulePlan
        )
        scheduler.scheduleAll()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }
}
