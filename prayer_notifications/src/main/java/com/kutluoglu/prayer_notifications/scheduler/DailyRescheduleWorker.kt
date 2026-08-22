package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.annotation.Factory

@Factory
class DailyRescheduleWorker(
    appContext: Context,
    params: WorkerParameters,
    private val scheduler: PrayerNotificationScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        scheduler.scheduleAll()
        return Result.success()
    }
}
