package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

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
}
