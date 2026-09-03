package com.kutluoglu.prayer_widget

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
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
class WidgetBootReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> =
        shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms

    @Test
    fun `boot completed schedules the minute tick when a widget is placed`() {
        val receiver = WidgetBootReceiver()

        receiver.handleBoot(context, widgetPresent = true)

        assertThat(scheduledAlarms()).hasSize(1)
    }

    @Test
    fun `boot completed does not schedule when no widget is placed`() {
        val receiver = WidgetBootReceiver()

        receiver.handleBoot(context, widgetPresent = false)

        assertThat(scheduledAlarms()).isEmpty()
    }

    @Test
    fun `non-boot action does not schedule`() {
        val receiver = WidgetBootReceiver()

        receiver.onReceive(context, Intent("com.example.OTHER"))

        assertThat(scheduledAlarms()).isEmpty()
    }
}
