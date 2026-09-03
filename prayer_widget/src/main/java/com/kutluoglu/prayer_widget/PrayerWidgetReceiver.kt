package com.kutluoglu.prayer_widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import com.kutluoglu.core.common.WidgetRefreshContract

/**
 * 2x1 (small) widget. Also owns the cross-module refresh broadcast handling;
 * a refresh updates every placed widget instance regardless of size.
 */
class PrayerWidgetReceiver : BasePrayerWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetRefreshContract.ACTION_REFRESH) {
            enqueueOneTimeRefresh(context)
            return
        }
        super.onReceive(context, intent)
    }
}
