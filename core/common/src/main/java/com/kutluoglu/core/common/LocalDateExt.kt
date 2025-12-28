package com.kutluoglu.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Created by F.K. on 28.12.2025.
 *
 */

fun LocalDate.startOfMonth(): LocalDate =
    LocalDate(this.year, this.month, 1)

@OptIn(ExperimentalTime::class)
fun Instant.startOfMonth(timeZone: TimeZone): LocalDate {
    val localDate = this.toLocalDateTime(timeZone)
    return LocalDate(localDate.year, localDate.month, 1)
}
