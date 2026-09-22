package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.ZoneId
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun toEpochMillis(prayer: Prayer, zoneId: ZoneId): Long =
    LocalDateTime(prayer.date, prayer.time).toInstant(TimeZone.of(zoneId.id)).toEpochMilliseconds()

fun formatClockTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun isJumuahPrayer(prayer: Prayer): Boolean =
    prayer.arabicName == "الظهر" && prayer.date.dayOfWeek == DayOfWeek.FRIDAY
