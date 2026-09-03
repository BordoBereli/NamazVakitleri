package com.kutluoglu.prayer_widget

import androidx.glance.appwidget.GlanceAppWidget

/** 4x2 (large) widget. */
class PrayerWidgetLargeReceiver : BasePrayerWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
