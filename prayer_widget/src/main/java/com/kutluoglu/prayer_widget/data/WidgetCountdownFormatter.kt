package com.kutluoglu.prayer_widget.data

import android.content.Context
import com.kutluoglu.prayer_widget.R
import org.koin.core.annotation.Factory

@Factory
class WidgetCountdownFormatter(
    private val context: Context
) {
    fun format(hours: Int, minutes: Int): String = formatCompact(
        hours = hours,
        minutes = minutes,
        hourShort = context.getString(R.string.widget_countdown_hour_short),
        minuteShort = context.getString(R.string.widget_countdown_minute_short)
    )
}

fun formatCompact(hours: Int, minutes: Int, hourShort: String, minuteShort: String): String {
    val h = hours.coerceAtLeast(0)
    val m = minutes.coerceAtLeast(0)
    return when {
        h > 0 && m > 0 -> "$h$hourShort $m$minuteShort"
        h > 0 -> "$h$hourShort"
        else -> "$m$minuteShort"
    }
}
