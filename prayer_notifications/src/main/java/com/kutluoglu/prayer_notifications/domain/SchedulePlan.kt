package com.kutluoglu.prayer_notifications.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import org.koin.core.annotation.Factory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val type: AlarmType = AlarmType.PRAYER,
    val isJumuah: Boolean = false,
    val nextPrayerTimeMillis: Long? = null,
    val nextPrayerName: String? = null,
    val previousPrayerTimeMillis: Long? = null,
    val prePrayerMinutes: Int? = null,
    val dailySummary: String? = null,
    val specialDay: SpecialDay? = null
)

@Factory
class SchedulePlan {

    companion object {
        const val REQUEST_CODE_COUNTDOWN_TICK = 2000
        const val REQUEST_CODE_DAILY_REMINDER = 2001
        const val REQUEST_CODE_SPECIAL_DAY = 2002
        const val REQUEST_CODE_PRE_SPECIAL_DAY = 2003
    }

    fun buildDailyAlarms(
        prayers: List<Prayer>,
        tomorrowPrayers: List<Prayer> = emptyList(),
        zoneId: ZoneId,
        now: Instant,
        enabledPrayers: Set<String>,
        prePrayerMinutes: Int,
        prePrayerEnabled: Boolean,
        dailyReminderEnabled: Boolean = false,
        dailyReminderHour: Int = 8,
        dailyReminderMinute: Int = 0,
        dailySummary: String = "",
        specialDayToday: SpecialDay? = null,
        specialDayTomorrow: SpecialDay? = null,
        jumuahEnabled: Boolean = true
    ): List<ScheduledAlarm> {
        val nowZoned = now.atZone(zoneId)
        val today = nowZoned.toLocalDate()
        val tomorrow = today.plusDays(1)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        val enabledToday = prayers.filter { it.name in enabledPrayers }
        val enabledTomorrow = tomorrowPrayers.filter { it.name in enabledPrayers }

        fun triggerFor(prayer: Prayer, date: java.time.LocalDate): Instant =
            LocalTime.of(prayer.time.hour, prayer.time.minute)
                .atDate(date)
                .atZone(zoneId)
                .toInstant()

        enabledToday.forEachIndexed { index, prayer ->
            val trigger = triggerFor(prayer, today)
            if (trigger.isAfter(now)) {
                val next = enabledToday.getOrNull(index + 1)
                val isLast = index == enabledToday.lastIndex
                val effectiveNextTime = if (isLast) {
                    enabledTomorrow.firstOrNull()?.let { triggerFor(it, tomorrow).toEpochMilli() }
                } else {
                    next?.let { triggerFor(it, today).toEpochMilli() }
                }
                val effectiveNextName = if (isLast && enabledTomorrow.isNotEmpty()) {
                    enabledTomorrow.first().name
                } else {
                    next?.name
                }
                val previous = enabledToday.getOrNull(index - 1)?.let {
                    triggerFor(it, today).toEpochMilli()
                }
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRAYER,
                    isJumuah = jumuahEnabled &&
                        prayer.name == "Dhuhr" &&
                        today.dayOfWeek == DayOfWeek.FRIDAY,
                    nextPrayerTimeMillis = effectiveNextTime,
                    nextPrayerName = effectiveNextName,
                    previousPrayerTimeMillis = previous
                )
            }
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                if (preTrigger.isAfter(now)) {
                    result += ScheduledAlarm(
                        prayerKey = "${prayer.name}_pre",
                        triggerAtMillis = preTrigger.toEpochMilli(),
                        requestCode = requestCode++,
                        type = AlarmType.PRE_PRAYER,
                        prePrayerMinutes = prePrayerMinutes
                    )
                }
            }
        }

        enabledTomorrow.forEachIndexed { index, prayer ->
            val trigger = triggerFor(prayer, tomorrow)
            val next = enabledTomorrow.getOrNull(index + 1)
            val previous = enabledTomorrow.getOrNull(index - 1)?.let {
                triggerFor(it, tomorrow).toEpochMilli()
            } ?: enabledToday.lastOrNull()?.let {
                triggerFor(it, today).toEpochMilli()
            }
            result += ScheduledAlarm(
                prayerKey = prayer.name,
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = requestCode++,
                type = AlarmType.PRAYER,
                isJumuah = jumuahEnabled &&
                    prayer.name == "Dhuhr" &&
                    tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
                nextPrayerTimeMillis = next?.let { triggerFor(it, tomorrow).toEpochMilli() },
                nextPrayerName = next?.name,
                previousPrayerTimeMillis = previous
            )
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                result += ScheduledAlarm(
                    prayerKey = "${prayer.name}_pre",
                    triggerAtMillis = preTrigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRE_PRAYER,
                    prePrayerMinutes = prePrayerMinutes
                )
            }
        }

        if (dailyReminderEnabled) {
            val reminderTrigger = LocalTime.of(dailyReminderHour, dailyReminderMinute)
                .atDate(today)
                .atZone(zoneId)
                .toInstant()
            if (reminderTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "daily_reminder",
                    triggerAtMillis = reminderTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_DAILY_REMINDER,
                    type = AlarmType.DAILY_REMINDER,
                    dailySummary = dailySummary
                )
            }
        }

        specialDayToday?.let { day ->
            val specialTrigger = LocalTime.of(8, 0)
                .atDate(today)
                .atZone(zoneId)
                .toInstant()
            if (specialTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "special_day",
                    triggerAtMillis = specialTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_SPECIAL_DAY,
                    type = AlarmType.SPECIAL_DAY,
                    specialDay = day
                )
            }
        }

        specialDayTomorrow?.let { day ->
            val specialTrigger = LocalTime.of(8, 0)
                .atDate(tomorrow)
                .atZone(zoneId)
                .toInstant()
            result += ScheduledAlarm(
                prayerKey = "pre_special_day",
                triggerAtMillis = specialTrigger.toEpochMilli(),
                requestCode = REQUEST_CODE_PRE_SPECIAL_DAY,
                type = AlarmType.PRE_SPECIAL_DAY,
                specialDay = day
            )
        }

        return result
    }
}
