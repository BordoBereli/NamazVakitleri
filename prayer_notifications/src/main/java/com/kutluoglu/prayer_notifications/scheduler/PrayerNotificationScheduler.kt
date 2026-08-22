package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class PrayerNotificationScheduler(
    private val context: Context,
    private val dataStore: NotificationSettingsDataStore,
    private val schedulePlan: SchedulePlan,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        // Request codes come from SchedulePlan.buildDailyAlarms (starts at 1000).
        // 5 prayers + 5 pre-prayers max = 10 codes; the range must cover it.
        const val REQUEST_CODE_START = 1000
        const val REQUEST_CODE_END = 1010
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll() {
        scope.launch {
            val settings = dataStore.getSettings()
            if (!settings.enabled) {
                cancelAll()
                return@launch
            }
            // TODO(Phase 5.4): load today's prayers for the active location and
            // schedule each ScheduledAlarm via setExactAndAllowWhileIdle.
            // This task wires the plumbing; the prayer loading is added in Task 5.4.
        }
    }

    fun cancelAll() {
        // Cancel all pending alarms by re-issuing the same PendingIntents with FLAG_NO_CREATE.
        for (code in REQUEST_CODE_START until REQUEST_CODE_END) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun scheduleAlarm(triggerAtMillis: Long, requestCode: Int, prayerKey: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
