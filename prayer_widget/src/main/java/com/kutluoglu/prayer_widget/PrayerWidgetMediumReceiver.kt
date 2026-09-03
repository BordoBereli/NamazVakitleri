package com.kutluoglu.prayer_widget

import androidx.glance.appwidget.GlanceAppWidget

/** 4x1 (medium) widget. */
class PrayerWidgetMediumReceiver : BasePrayerWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
