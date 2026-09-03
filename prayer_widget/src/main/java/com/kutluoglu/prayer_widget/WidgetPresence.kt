package com.kutluoglu.prayer_widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * True when at least one widget instance of any size is placed on the home screen.
 * Used to keep the per-minute refresh alarm running only while a widget exists.
 */
internal fun hasAnyWidget(
    context: Context,
    manager: AppWidgetManager = AppWidgetManager.getInstance(context)
): Boolean {
    val providers = listOf(
        ComponentName(context, PrayerWidgetReceiver::class.java),
        ComponentName(context, PrayerWidgetMediumReceiver::class.java),
        ComponentName(context, PrayerWidgetLargeReceiver::class.java)
    )
    return providers.any { manager.getAppWidgetIds(it).isNotEmpty() }
}
