package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single
import java.time.Instant
import java.time.ZoneId

@Single
class PrayerNotificationScheduler(
    private val context: Context,
    private val dataStore: NotificationSettingsDataStore,
    private val schedulePlan: SchedulePlan,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val locationsCoordinator: LocationsCoordinator,
    private val getSettingsUseCase: GetSettingsUseCase,
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
            val location = locationsCoordinator.resolveSelected() ?: run {
                cancelAll()
                return@launch
            }
            val appSettings = getSettingsUseCase()
            val zoneId = ZoneId.of(appSettings.location.timeZone)
            val today = LocalDateTime.now(zoneId)
            val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
            val prayers = getPrayerTimesUseCase(
                date = today,
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = zoneId,
                calculationMethod = method
            ).getOrNull() ?: return@launch

            val enabled = settings.prayerToggles.filterValues { it }.keys
            val alarms = schedulePlan.buildDailyAlarms(
                prayers = prayers,
                zoneId = zoneId,
                now = Instant.now(),
                enabledPrayers = enabled,
                prePrayerMinutes = settings.prePrayerMinutes,
                prePrayerEnabled = settings.prePrayerReminderEnabled
            )
            cancelAll()
            alarms.forEach { scheduleAlarm(it.triggerAtMillis, it.requestCode, it.prayerKey) }
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
