package com.kutluoglu.prayer_notifications.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val isPrePrayer: Boolean = false
)

class SchedulePlan {

    fun buildDailyAlarms(
        prayers: List<Prayer>,
        zoneId: ZoneId,
        now: Instant,
        enabledPrayers: Set<String>,
        prePrayerMinutes: Int,
        prePrayerEnabled: Boolean
    ): List<ScheduledAlarm> {
        val nowZoned = now.atZone(zoneId)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        prayers.forEach { prayer ->
            if (prayer.name !in enabledPrayers) return@forEach
            val trigger = LocalTime.of(prayer.time.hour, prayer.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
            if (trigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++
                )
            }
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                if (preTrigger.isAfter(now)) {
                    result += ScheduledAlarm(
                        prayerKey = "${prayer.name}_pre",
                        triggerAtMillis = preTrigger.toEpochMilli(),
                        requestCode = requestCode++,
                        isPrePrayer = true
                    )
                }
            }
        }
        return result
    }
}
