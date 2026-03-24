package com.kutluoglu.core.common

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus

/**
 * Created by F.K. on 28.12.2025.
 *
 */

/**
 * Usage example:
 * val today = LocalDate(2024, 3, 15)
 * val monthRange = today.monthRange()
 * println("Month starts: ${monthRange.start}") // 2024-03-01
 * println("Month ends: ${monthRange.end}")     // 2024-03-31
 */

data class MonthRange(val start: LocalDate, val end: LocalDate)

fun LocalDate.monthRange(): MonthRange {
    val start = LocalDate(this.year, this.month, 1)
    val end = if (this.month == Month.DECEMBER) {
        LocalDate(this.year + 1, Month.JANUARY, 1).minus(1, DateTimeUnit.DAY)
    } else {
        LocalDate(this.year, this.month.ordinal + 2, 1).minus(1, DateTimeUnit.DAY)
    }
    return MonthRange(start, end)
}
