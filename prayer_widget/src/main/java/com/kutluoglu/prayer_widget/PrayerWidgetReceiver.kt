package com.kutluoglu.prayer_widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.kutluoglu.core.common.WidgetRefreshContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 2x1 (small) widget. Also owns the cross-module refresh broadcast handling and the
 * per-minute tick: a refresh updates every placed widget instance regardless of size.
 */
class PrayerWidgetReceiver : BasePrayerWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetRefreshContract.ACTION_REFRESH -> {
                enqueueOneTimeRefresh(context)
                return
            }
            WidgetMinuteScheduler.ACTION_MINUTE_TICK -> {
                handleMinuteTick(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    internal fun handleMinuteTick(context: Context) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                refreshWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
        rearmMinuteTick(context)
    }

    internal suspend fun refreshWidgets(
        context: Context,
        refresh: suspend (Context) -> Unit = { PrayerWidget().updateAll(it) }
    ) {
        runCatching { refresh(context) }
    }

    internal fun rearmMinuteTick(
        context: Context,
        widgetPresent: Boolean = hasAnyWidget(context)
    ) {
        if (widgetPresent) {
            WidgetMinuteScheduler(context).schedule()
        } else {
            WidgetMinuteScheduler(context).cancel()
        }
    }
}
