package com.kutluoglu.prayer_feature.home.domain

import org.koin.core.annotation.Factory
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

@Factory
class RamadanDateResolver {

    fun ramadanDayFor(date: LocalDate, hijriAdjustment: Int = 0): Int? {
        val hijrah = HijrahDate.from(date).plus(hijriAdjustment.toLong(), ChronoUnit.DAYS)
        val month = hijrah.get(ChronoField.MONTH_OF_YEAR)
        val day = hijrah.get(ChronoField.DAY_OF_MONTH)
        return if (month == 9) day else null
    }
}
