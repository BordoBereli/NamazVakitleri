package com.kutluoglu.prayer_notifications.scheduler

import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class SchedulePlanRamadanTest {

    private val plan = SchedulePlan()
    private val zoneId = ZoneId.of("Europe/Istanbul")

    private fun prayer(name: String, time: LocalTime) =
        Prayer(name, name, time, LocalDate(2026, 9, 2))

    @Test
    fun `builds sahur and iftar alarms when ramadan enabled`() {
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50)).copy(isImsak = true),
            prayer("Fajr", LocalTime(5, 0)),
            prayer("Maghrib", LocalTime(19, 30))
        )
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zoneId,
            now = Instant.parse("2026-09-02T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Maghrib"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            ramadanEnabled = true
        )
        assertTrue(alarms.any { it.type == AlarmType.SAHUR_END })
        assertTrue(alarms.any { it.type == AlarmType.IFTAR })
    }

    @Test
    fun `does not build ramadan alarms when disabled`() {
        val prayers = listOf(
            prayer("Imsak", LocalTime(4, 50)).copy(isImsak = true),
            prayer("Fajr", LocalTime(5, 0)),
            prayer("Maghrib", LocalTime(19, 30))
        )
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zoneId,
            now = Instant.parse("2026-09-02T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Maghrib"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            ramadanEnabled = false
        )
        assertTrue(alarms.none { it.type == AlarmType.SAHUR_END || it.type == AlarmType.IFTAR })
    }
}
