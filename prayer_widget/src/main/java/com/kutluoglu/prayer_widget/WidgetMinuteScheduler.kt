package com.kutluoglu.prayer_widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules an exact alarm at the next minute boundary that refreshes every placed
 * widget instance. The alarm targets [PrayerWidgetReceiver] with [ACTION_MINUTE_TICK],
 * which re-arms the next minute, so the chain survives process death.
 */
class WidgetMinuteScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return
        }
        pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)?.let {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinuteBoundaryMillis(System.currentTimeMillis()),
                it
            )
        }
    }

    fun cancel() {
        pendingIntent(PendingIntent.FLAG_NO_CREATE)?.let { alarmManager.cancel(it) }
    }

    private fun pendingIntent(flags: Int): PendingIntent? {
        val intent = Intent(context, PrayerWidgetReceiver::class.java)
            .setAction(ACTION_MINUTE_TICK)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MINUTE_TICK,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MINUTE_TICK = "com.kutluoglu.prayer_widget.MINUTE_TICK"
        const val REQUEST_CODE_MINUTE_TICK = 3000

        internal fun nextMinuteBoundaryMillis(nowMillis: Long): Long =
            (nowMillis / 60_000 + 1) * 60_000
    }
}
