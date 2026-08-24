package com.kutluoglu.prayer_notifications.domain

import org.koin.core.annotation.Factory
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

@Factory
class SpecialDaysCalculator {

    fun specialDayFor(date: LocalDate, hijriAdjustment: Int = 0): SpecialDay? {
        val hijrah = HijrahDate.from(date).plus(hijriAdjustment.toLong(), ChronoUnit.DAYS)
        val month = hijrah.get(ChronoField.MONTH_OF_YEAR)
        val day = hijrah.get(ChronoField.DAY_OF_MONTH)
        return when {
            month == 9 && day == 1 -> SpecialDay.RAMADAN_START
            month == 9 && day == 27 -> SpecialDay.LAYLAT_AL_QADIR
            month == 10 && day == 1 -> SpecialDay.EID_AL_FITR
            month == 12 && day == 10 -> SpecialDay.EID_AL_ADHA
            else -> null
        }
    }
}
