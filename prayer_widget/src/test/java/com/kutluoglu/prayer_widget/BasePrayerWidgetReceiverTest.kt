package com.kutluoglu.prayer_widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BasePrayerWidgetReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        runCatching { WorkManager.initialize(context, Configuration.Builder().build()) }
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> =
        shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms

    @Test
    fun `onUpdate schedules the minute tick`() {
        val receiver = PrayerWidgetReceiver()

        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(1))

        assertThat(scheduledAlarms()).hasSize(1)
    }

    @Test
    fun `onDeleted cancels the minute tick when no widget remains`() {
        val receiver = PrayerWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(1))
        assertThat(scheduledAlarms()).hasSize(1)

        receiver.onDeleted(context, intArrayOf(1))

        assertThat(scheduledAlarms()).isEmpty()
    }
}
