package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyRescheduleWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `doWork returns success`() = runTest {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val worker = DailyRescheduleWorker(
            context,
            mockk<WorkerParameters>(relaxed = true),
            scheduler
        )
        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
    }
}
