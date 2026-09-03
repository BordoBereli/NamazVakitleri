package com.kutluoglu.prayer_widget

import android.app.AlarmManager
import android.content.Context
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
class WidgetMinuteSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> =
        shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms

    @Test
    fun `nextMinuteBoundaryMillis returns the next whole minute`() {
        val base = 10L * 60 * 60 * 1000 + 15 * 60_000 + 37_000 // 10:15:37
        assertThat(WidgetMinuteScheduler.nextMinuteBoundaryMillis(base))
            .isEqualTo(10L * 60 * 60 * 1000 + 16 * 60_000) // 10:16:00
    }

    @Test
    fun `nextMinuteBoundaryMillis on an exact minute advances to the next minute`() {
        val base = 10L * 60 * 60 * 1000 + 15 * 60_000 // 10:15:00.000
        assertThat(WidgetMinuteScheduler.nextMinuteBoundaryMillis(base))
            .isEqualTo(10L * 60 * 60 * 1000 + 16 * 60_000)
    }

    @Test
    fun `schedule registers an exact allow-while-idle alarm at the next minute boundary`() {
        WidgetMinuteScheduler(context).schedule()

        val alarms = scheduledAlarms()
        assertThat(alarms).hasSize(1)
        assertThat(alarms[0].allowWhileIdle).isTrue()
        assertThat(alarms[0].triggerAtTime % 60_000).isEqualTo(0)
        assertThat(alarms[0].triggerAtTime).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `cancel removes the scheduled alarm`() {
        val scheduler = WidgetMinuteScheduler(context)
        scheduler.schedule()
        scheduler.cancel()

        assertThat(scheduledAlarms()).isEmpty()
    }
}
