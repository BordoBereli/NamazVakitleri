package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DailyRescheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    override suspend fun doWork(): Result {
        get<AlarmScheduler>().scheduleAllSuspending()
        return Result.success()
    }
}
