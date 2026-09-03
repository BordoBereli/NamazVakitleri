package com.kutluoglu.core.common

/**
 * Cross-module broadcast contract between prayer_notifications (which fires
 * exact alarms at prayer times) and prayer_widget (which refreshes on them).
 */
object WidgetRefreshContract {
    const val ACTION_REFRESH = "com.kutluoglu.prayer_widget.REFRESH"
}
