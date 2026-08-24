package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class SchedulePlanTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val date = LocalDate(2026, 8, 22)

    private fun prayer(name: String, time: LocalTime) = Prayer(
        name = name,
        arabicName = name,
        time = time,
        date = date
    )

    private val prayers = listOf(
        prayer("Fajr", LocalTime(4, 30)),
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
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(5)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun `skips disabled prayers`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms).hasSize(1)
        assertThat(alarms[0].prayerKey).isEqualTo("Fajr")
    }

    @Test
    fun `adds pre-prayer alarms when enabled`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        assertThat(alarms).hasSize(2)
        assertThat(alarms.map { it.prayerKey }).containsExactly("Fajr", "Fajr_pre")
    }

    @Test
    fun `skips alarms already in the past`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
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
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms[0].triggerAtMillis).isEqualTo(1787362200000L)
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
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 0,
            dailySummary = "Fajr 04:30"
        )
        val reminder = alarms.first { it.type == AlarmType.DAILY_REMINDER }
        assertThat(reminder.dailySummary).isEqualTo("Fajr 04:30")
        assertThat(reminder.requestCode).isEqualTo(SchedulePlan.REQUEST_CODE_DAILY_REMINDER)
    }

    @Test
    fun `skips daily reminder when time already passed`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Fajr"),
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
            enabledPrayers = setOf("Fajr"),
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
            enabledPrayers = setOf("Fajr", "Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val fajr = alarms.first { it.prayerKey == "Fajr" }
        assertThat(fajr.nextPrayerName).isEqualTo("Dhuhr")
        assertThat(fajr.nextPrayerTimeMillis).isNotNull()
    }
}
