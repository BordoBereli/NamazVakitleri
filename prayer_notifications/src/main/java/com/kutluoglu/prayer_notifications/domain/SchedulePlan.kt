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
        jumuahEnabled: Boolean = true,
        nextDayFajrTimeMillis: Long? = null
    ): List<ScheduledAlarm> {
        val nowZoned = now.atZone(zoneId)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        val enabled = prayers.filter { it.name in enabledPrayers }
        enabled.forEachIndexed { index, prayer ->
            val trigger = LocalTime.of(prayer.time.hour, prayer.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
            val next = enabled.getOrNull(index + 1)
            val nextTime = next?.let {
                LocalTime.of(it.time.hour, it.time.minute)
                    .atDate(nowZoned.toLocalDate())
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
            val isLast = index == enabled.lastIndex
            val effectiveNextTime = if (isLast) nextDayFajrTimeMillis else nextTime
            val effectiveNextName = if (isLast && nextDayFajrTimeMillis != null) "Fajr" else next?.name
            val previous = enabled.getOrNull(index - 1)?.let {
                LocalTime.of(it.time.hour, it.time.minute)
                    .atDate(nowZoned.toLocalDate())
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
            if (trigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRAYER,
                    isJumuah = jumuahEnabled &&
                        prayer.name == "Dhuhr" &&
                        nowZoned.dayOfWeek == DayOfWeek.FRIDAY,
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

        if (dailyReminderEnabled) {
            val reminderTrigger = LocalTime.of(dailyReminderHour, dailyReminderMinute)
                .atDate(nowZoned.toLocalDate())
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
                .atDate(nowZoned.toLocalDate())
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
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
            if (specialTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "pre_special_day",
                    triggerAtMillis = specialTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_PRE_SPECIAL_DAY,
                    type = AlarmType.PRE_SPECIAL_DAY,
                    specialDay = day
                )
            }
        }

        return result
    }
}
