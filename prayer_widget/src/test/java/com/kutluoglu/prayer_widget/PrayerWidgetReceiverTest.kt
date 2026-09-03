package com.kutluoglu.prayer_widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.WidgetRefreshContract
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerWidgetReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        runCatching { WorkManager.initialize(context, Configuration.Builder().build()) }
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun `enqueueRefresh enqueues unique periodic work for the widget worker`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val receiver = PrayerWidgetReceiver()

        receiver.enqueueRefresh(context, workManager)

        val requestSlot = slot<PeriodicWorkRequest>()
        verify {
            workManager.enqueueUniquePeriodicWork(
                BasePrayerWidgetReceiver.REFRESH_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(requestSlot)
            )
        }
        assertThat(requestSlot.captured.workSpec.workerClassName)
            .isEqualTo(PrayerWidgetWorker::class.java.name)
    }

    @Test
    fun `enqueueOneTimeRefresh enqueues unique one-time work for the widget worker`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val receiver = PrayerWidgetReceiver()

        receiver.enqueueOneTimeRefresh(context, workManager)

        val requestSlot = slot<OneTimeWorkRequest>()
        verify {
            workManager.enqueueUniqueWork(
                BasePrayerWidgetReceiver.ONE_TIME_REFRESH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                capture(requestSlot)
            )
        }
        assertThat(requestSlot.captured.workSpec.workerClassName)
            .isEqualTo(PrayerWidgetWorker::class.java.name)
    }

    @Test
    fun `REFRESH action triggers one-time refresh enqueue`() {
        val receiver = spyk(PrayerWidgetReceiver())

        receiver.onReceive(context, Intent(WidgetRefreshContract.ACTION_REFRESH))

        verify { receiver.enqueueOneTimeRefresh(context, any()) }
    }

    @Test
    fun `non-REFRESH action does not trigger one-time refresh`() {
        val receiver = spyk(PrayerWidgetReceiver())

        receiver.onReceive(context, Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE))

        verify(exactly = 0) { receiver.enqueueOneTimeRefresh(context, any()) }
    }

    @Test
    fun `MINUTE_TICK action dispatches to handleMinuteTick`() {
        val receiver = spyk(PrayerWidgetReceiver())
        every { receiver.handleMinuteTick(context) } just Runs

        receiver.onReceive(context, Intent(WidgetMinuteScheduler.ACTION_MINUTE_TICK))

        verify { receiver.handleMinuteTick(context) }
    }

    @Test
    fun `rearmMinuteTick schedules next tick when a widget is present`() {
        val receiver = PrayerWidgetReceiver()

        receiver.rearmMinuteTick(context, widgetPresent = true)

        val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms
        assertThat(alarms).hasSize(1)
    }

    @Test
    fun `rearmMinuteTick cancels when no widget is present`() {
        val receiver = PrayerWidgetReceiver()

        receiver.rearmMinuteTick(context, widgetPresent = false)

        val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms
        assertThat(alarms).isEmpty()
    }

    @Test
    fun `refreshWidgets invokes the widget update`() = runTest {
        val receiver = PrayerWidgetReceiver()
        var refreshed = false

        receiver.refreshWidgets(context) { refreshed = true }

        assertThat(refreshed).isTrue()
    }
}
