package com.kutluoglu.prayer_widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrayerWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            PrayerWidget().updateAll(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
