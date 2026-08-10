package com.kutluoglu.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.ZoneId

/**
 * Created by F.K. on 22.10.2025.
 *
 */

fun LocalDateTime.Companion.now(zoneId: ZoneId): LocalDateTime {
    val javaLocalDateTime = java.time.LocalDateTime.now(zoneId)
    return LocalDateTime(
        year = javaLocalDateTime.year,
        month = javaLocalDateTime.monthValue,
        day = javaLocalDateTime.dayOfMonth,
        hour = javaLocalDateTime.hour,
        minute = javaLocalDateTime.minute,
        second = javaLocalDateTime.second,
        nanosecond = javaLocalDateTime.nano
    )
}

fun LocalDateTime.Companion.createBy(year: Int, month: Int, day: Int): LocalDateTime {
    val javaLocalDateTime = java.time.LocalDateTime.now(ZoneId.systemDefault())
    return LocalDateTime(
        year = year,
        month = month,
        day = day,
        hour = javaLocalDateTime.hour,
        minute = javaLocalDateTime.minute,
        second = javaLocalDateTime.second,
        nanosecond = javaLocalDateTime.nano
    )
}

fun LocalDateTime.startOfMonthTime(): LocalDateTime =
    LocalDateTime(this.year, this.month, 1, 0, 0, 0, 0)

fun LocalDateTime.startOfMonth(): LocalDate =
    LocalDate(this.year, this.month, 1)
