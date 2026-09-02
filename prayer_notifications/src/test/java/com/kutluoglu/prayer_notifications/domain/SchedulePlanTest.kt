package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SchedulePlanTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val date = kotlinx.datetime.LocalDate(2026, 8, 22)

    private fun prayer(name: String, time: LocalTime) = Prayer(
        name = name,
        arabicName = name,
        time = time,
        date = date
    )

    private val prayers = listOf(
        prayer("Dhuhr", LocalTime(13, 0)),
        prayer("Asr", LocalTime(16, 45)),
        prayer("Maghrib", LocalTime(19, 55)),
        prayer("Isha", LocalTime(21, 15))
    )

    @Test
    fun `builds an alarm for each enabled prayer`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(4)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun `skips disabled prayers`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(1)
        assertThat(alarms[0].prayerKey).isEqualTo("Dhuhr")
    }

    @Test
    fun `adds pre-prayer alarms when enabled`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        assertThat(alarms).hasSize(2)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Dhuhr", "Dhuhr_pre")
    }

    @Test
    fun `skips alarms already in the past`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms.map { it.prayerKey }).containsExactly("Maghrib", "Isha")
    }

    @Test
    fun `converts prayer time to correct epoch millis`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms[0].triggerAtMillis).isEqualTo(1787392800000L)
    }

    @Test
    fun `skips pre-prayer alarm when it is already in the past`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T16:50:00Z"), // 19:50 Istanbul
            enabledPrayers = setOf("Maghrib"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        assertThat(alarms.map { it.prayerKey }).containsExactly("Maghrib")
    }

    @Test
    fun `adds daily reminder alarm when enabled`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 0,
            dailySummary = "Dhuhr 13:00"
        )
        val reminder = alarms.first { it.type == AlarmType.DAILY_REMINDER }
        assertThat(reminder.dailySummary).isEqualTo("Dhuhr 13:00")
        assertThat(reminder.requestCode).isEqualTo(SchedulePlan.REQUEST_CODE_DAILY_REMINDER)
    }

    @Test
    fun `skips daily reminder when time already passed`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 0
        )
        assertThat(alarms.none { it.type == AlarmType.DAILY_REMINDER }).isTrue()
    }

    @Test
    fun `adds special day and pre-special day alarms`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            specialDayToday = SpecialDay.EID_AL_FITR,
            specialDayTomorrow = SpecialDay.EID_AL_ADHA
        )
        assertThat(alarms.first { it.type == AlarmType.SPECIAL_DAY }.specialDay)
            .isEqualTo(SpecialDay.EID_AL_FITR)
        assertThat(alarms.first { it.type == AlarmType.PRE_SPECIAL_DAY }.specialDay)
            .isEqualTo(SpecialDay.EID_AL_ADHA)
    }

    @Test
    fun `marks Friday Dhuhr alarm as jumuah`() {
        // 2026-08-22 is a Saturday; use a Friday: 2026-08-21.
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-21T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().isJumuah).isTrue()
    }

    @Test
    fun `does not mark non-Friday Dhuhr as jumuah`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"), // Saturday
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().isJumuah).isFalse()
    }

    @Test
    fun `carries next prayer time and name on prayer alarms`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val dhuhr = alarms.first { it.prayerKey == "Dhuhr" }
        assertThat(dhuhr.nextPrayerName).isEqualTo("Asr")
        assertThat(dhuhr.nextPrayerTimeMillis).isNotNull()
    }

    @Test
    fun `carries previous prayer time on prayer alarms`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val dhuhr = alarms.first { it.prayerKey == "Dhuhr" }
        val asr = alarms.first { it.prayerKey == "Asr" }
        assertThat(dhuhr.previousPrayerTimeMillis).isNull()
        assertThat(asr.previousPrayerTimeMillis).isEqualTo(dhuhr.triggerAtMillis)
    }

    @Test
    fun `last enabled prayer points to tomorrow's Dhuhr when provided`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = listOf(prayer("Dhuhr", LocalTime(13, 0))),
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val isha = alarms.first { it.prayerKey == "Isha" }
        val tomorrowDhuhr = java.time.LocalTime.of(13, 0)
            .atDate(LocalDate.of(2026, 8, 23))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertThat(isha.nextPrayerName).isEqualTo("Dhuhr")
        assertThat(isha.nextPrayerTimeMillis).isEqualTo(tomorrowDhuhr)
    }

    @Test
    fun `last enabled prayer has no next when no tomorrow prayers`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val isha = alarms.first { it.prayerKey == "Isha" }
        assertThat(isha.nextPrayerName).isNull()
        assertThat(isha.nextPrayerTimeMillis).isNull()
    }

    @Test
    fun `schedules today's remaining and tomorrow's full day`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms.map { it.prayerKey })
            .containsExactly("Maghrib", "Isha", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun `marks tomorrow's Friday Dhuhr as jumuah`() {
        // 2026-08-21 is a Friday. now = 2026-08-20 15:00 UTC (18:00 Istanbul), after today's Dhuhr.
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-20T15:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().prayerKey).isEqualTo("Dhuhr")
        assertThat(alarms.single().isJumuah).isTrue()
    }

    @Test
    fun `tomorrow's Dhuhr carries today's last prayer as previous`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T20:00:00Z"), // 23:00 Istanbul, after Isha
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val tomorrowDhuhr = alarms.first { it.prayerKey == "Dhuhr" }
        val todayIshaTrigger = java.time.LocalTime.of(21, 15)
            .atDate(LocalDate.of(2026, 8, 22))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertThat(tomorrowDhuhr.previousPrayerTimeMillis).isEqualTo(todayIshaTrigger)
    }

    @Test
    fun `request codes stay within the cancel range for two days`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        val codes = alarms.map { it.requestCode }
        assertThat(codes.min()!!).isAtLeast(1000)
        assertThat(codes.max()!!).isLessThan(1020)
    }
}
