package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import com.kutluoglu.prayer_notifications.domain.ScheduledAlarm
import com.kutluoglu.prayer_notifications.domain.SpecialDaysCalculator
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Single
class PrayerNotificationScheduler(
    private val context: Context,
    private val dataStore: NotificationSettingsDataStore,
    private val schedulePlan: SchedulePlan,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val locationsCoordinator: LocationsCoordinator,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val notificationManager: PrayerNotificationManager,
    private val specialDaysCalculator: SpecialDaysCalculator = SpecialDaysCalculator(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        // Request codes come from SchedulePlan.buildDailyAlarms (starts at 1000).
        // 5 prayers + 5 pre-prayers max = 10 codes; the range must cover it.
        const val REQUEST_CODE_START = 1000
        const val REQUEST_CODE_END = 1010
        const val DAILY_RESCHEDULE_WORK_NAME = "daily_prayer_reschedule"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll() {
        scope.launch { scheduleAllSuspending() }
    }

    suspend fun scheduleAllSuspending() {
        val settings = dataStore.getSettings()
        if (!settings.enabled) {
            cancelAll()
            cancelDailyReschedule()
            return
        }
        val location = locationsCoordinator.resolveSelected() ?: run {
            cancelAll()
            cancelDailyReschedule()
            return
        }
        notificationManager.createChannels(settings)
        val appSettings = runCatching { getSettingsUseCase() }.getOrElse {
            cancelAll()
            cancelDailyReschedule()
            return
        }
        val zoneId = runCatching { ZoneId.of(appSettings.location.timeZone) }
            .getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(zoneId)
        val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
        val prayers = getPrayerTimesUseCase(
            date = LocalDateTime.now(zoneId),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method
        ).getOrNull() ?: run {
            cancelAll()
            cancelDailyReschedule()
            return
        }

        val nextDayFajrTimeMillis = if (settings.countdownEnabled) {
            val tomorrow = today.plusDays(1)
            getPrayerTimesUseCase(
                date = LocalDateTime(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth, 0, 0),
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = zoneId,
                calculationMethod = method,
                persistDailyCache = false
            ).getOrNull()
                ?.firstOrNull { it.name == "Fajr" }
                ?.let {
                    LocalTime.of(it.time.hour, it.time.minute)
                        .atDate(tomorrow)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                }
        } else {
            null
        }

        val enabled = settings.prayerToggles.filterValues { it }.keys
        val summary = buildDailySummary(prayers)
        val specialDayToday = if (settings.specialDaysEnabled) {
            specialDaysCalculator.specialDayFor(today, appSettings.hijriAdjustment)
        } else {
            null
        }
        val specialDayTomorrow = if (settings.specialDaysEnabled) {
            specialDaysCalculator.specialDayFor(today.plusDays(1), appSettings.hijriAdjustment)
        } else {
            null
        }
        val alarms = schedulePlan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zoneId,
            now = Instant.now(),
            enabledPrayers = enabled,
            prePrayerMinutes = settings.prePrayerMinutes,
            prePrayerEnabled = settings.prePrayerReminderEnabled,
            dailyReminderEnabled = settings.dailyReminderEnabled,
            dailyReminderHour = settings.dailyReminderHour,
            dailyReminderMinute = settings.dailyReminderMinute,
            dailySummary = summary,
            specialDayToday = specialDayToday,
            specialDayTomorrow = specialDayTomorrow,
            jumuahEnabled = settings.jumuahEnabled,
            nextDayFajrTimeMillis = nextDayFajrTimeMillis
        )
        cancelAll()
        alarms.forEach { scheduleAlarm(it) }
        if (settings.countdownEnabled) {
            val nextPrayer = alarms.firstOrNull { it.type == AlarmType.PRAYER }
            if (nextPrayer != null) {
                updateCountdown(
                    nextPrayer.triggerAtMillis,
                    nextPrayer.prayerKey,
                    nextPrayer.previousPrayerTimeMillis
                )
            } else if (nextDayFajrTimeMillis != null) {
                val lastEnabledTrigger = prayers
                    .filter { it.name in enabled }
                    .lastOrNull()
                    ?.let {
                        LocalTime.of(it.time.hour, it.time.minute)
                            .atDate(today)
                            .atZone(zoneId)
                            .toInstant()
                            .toEpochMilli()
                    }
                updateCountdown(nextDayFajrTimeMillis, "Fajr", lastEnabledTrigger)
            }
        }
        if (WorkManager.isInitialized()) {
            val request = PeriodicWorkRequestBuilder<DailyRescheduleWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_RESCHEDULE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } else {
            Log.w("PrayerNotificationScheduler", "WorkManager not initialized; skipping daily reschedule enqueue")
        }
    }

    fun cancelDailyReschedule() {
        if (WorkManager.isInitialized()) {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_RESCHEDULE_WORK_NAME)
        }
    }

    fun cancelAll() {
        notificationManager.cancelCountdown()
        // Cancel all pending alarms by re-issuing the same PendingIntents with FLAG_NO_CREATE.
        for (code in REQUEST_CODE_START until REQUEST_CODE_END) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
        val countdownIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
        val countdownPendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, countdownIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        countdownPendingIntent?.let { alarmManager.cancel(it) }
        listOf(
            SchedulePlan.REQUEST_CODE_DAILY_REMINDER,
            SchedulePlan.REQUEST_CODE_SPECIAL_DAY,
            SchedulePlan.REQUEST_CODE_PRE_SPECIAL_DAY
        ).forEach { code ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun scheduleAlarm(alarm: ScheduledAlarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, alarm.type.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, alarm.prayerKey)
            .putExtra(AlarmReceiver.EXTRA_IS_JUMUAH, alarm.isJumuah)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_TIME, alarm.nextPrayerTimeMillis ?: 0L)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_NAME, alarm.nextPrayerName ?: "")
            .putExtra(AlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, alarm.prePrayerMinutes ?: 0)
            .putExtra(AlarmReceiver.EXTRA_DAILY_SUMMARY, alarm.dailySummary ?: "")
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, alarm.specialDay?.name ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(alarm.triggerAtMillis, pendingIntent)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun updateCountdown(targetMillis: Long, prayerName: String, previousTimeMillis: Long? = null) {
        val remaining = targetMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            notificationManager.cancelCountdown()
            return
        }
        notificationManager.showCountdownNotification(prayerName, targetMillis, previousTimeMillis, remaining)
        scheduleCountdownTick(targetMillis, prayerName, previousTimeMillis)
    }

    fun cancelCountdown() {
        notificationManager.cancelCountdown()
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun scheduleCountdownTick(targetMillis: Long, prayerName: String, previousTimeMillis: Long?) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, targetMillis)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, prayerName)
        if (previousTimeMillis != null) {
            intent.putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PREVIOUS_TIME, previousTimeMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(System.currentTimeMillis() + 60_000, pendingIntent)
    }

    suspend fun scheduleDailyReminder() {
        val settings = dataStore.getSettings()
        if (!settings.dailyReminderEnabled) return
        val location = locationsCoordinator.resolveSelected() ?: return
        val appSettings = runCatching { getSettingsUseCase() }.getOrNull() ?: return
        val zoneId = runCatching { ZoneId.of(appSettings.location.timeZone) }
            .getOrDefault(ZoneId.systemDefault())
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
        val prayers = getPrayerTimesUseCase(
            date = LocalDateTime(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth, 0, 0),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method
        ).getOrNull() ?: return
        val trigger = LocalTime.of(settings.dailyReminderHour, settings.dailyReminderMinute)
            .atDate(tomorrow)
            .atZone(zoneId)
            .toInstant()
        scheduleAlarm(
            ScheduledAlarm(
                prayerKey = "daily_reminder",
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = SchedulePlan.REQUEST_CODE_DAILY_REMINDER,
                type = AlarmType.DAILY_REMINDER,
                dailySummary = buildDailySummary(prayers)
            )
        )
    }

    private fun buildDailySummary(prayers: List<Prayer>): String =
        prayers.joinToString(" · ") { prayer ->
            val time = "${prayer.time.hour.toString().padStart(2, '0')}:" +
                prayer.time.minute.toString().padStart(2, '0')
            "${prayer.name} $time"
        }
}
