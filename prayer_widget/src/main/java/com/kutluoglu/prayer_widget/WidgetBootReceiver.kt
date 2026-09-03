package com.kutluoglu.prayer_widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the per-minute widget refresh after a reboot, but only while at least one
 * widget instance is placed (alarms do not survive a reboot; widgets do).
 */
class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            handleBoot(context)
        }
    }

    internal fun handleBoot(
        context: Context,
        widgetPresent: Boolean = hasAnyWidget(context)
    ) {
        if (widgetPresent) {
            WidgetMinuteScheduler(context).schedule()
        }
    }
}
