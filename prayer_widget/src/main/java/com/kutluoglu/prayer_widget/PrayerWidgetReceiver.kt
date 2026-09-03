package com.kutluoglu.prayer_widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kutluoglu.core.common.WidgetRefreshContract
import java.util.concurrent.TimeUnit

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetRefreshContract.ACTION_REFRESH) {
            enqueueOneTimeRefresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueRefresh(context)
    }

    internal fun enqueueRefresh(
        context: Context,
        workManager: WorkManager = WorkManager.getInstance(context)
    ) {
        val request = PeriodicWorkRequestBuilder<PrayerWidgetWorker>(30, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    internal fun enqueueOneTimeRefresh(
        context: Context,
        workManager: WorkManager = WorkManager.getInstance(context)
    ) {
        val request = OneTimeWorkRequestBuilder<PrayerWidgetWorker>().build()
        workManager.enqueueUniqueWork(
            ONE_TIME_REFRESH_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val REFRESH_WORK_NAME = "prayer-widget-refresh"
        const val ONE_TIME_REFRESH_WORK_NAME = "prayer-widget-refresh-once"
    }
}
