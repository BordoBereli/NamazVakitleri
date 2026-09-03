package com.kutluoglu.prayer_widget

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
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerWidgetReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        runCatching { WorkManager.initialize(context, Configuration.Builder().build()) }
    }

    @Test
    fun `enqueueRefresh enqueues unique periodic work for the widget worker`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val receiver = PrayerWidgetReceiver()

        receiver.enqueueRefresh(context, workManager)

        val requestSlot = slot<PeriodicWorkRequest>()
        verify {
            workManager.enqueueUniquePeriodicWork(
                PrayerWidgetReceiver.REFRESH_WORK_NAME,
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
                PrayerWidgetReceiver.ONE_TIME_REFRESH_WORK_NAME,
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
}
