package com.kutluoglu.core.common

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Created by F.K. on 22.10.2025.
 *
 */

/* Create a formatter for "HH:mm" format.
 * - "HH" hour of the day.
 * - "mm" minute of the hour
 */
val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/* Create a formatter for the Hijri calendar system.
 * - "dd" for day of the month.
 * - "MMMM" for the full month name (e.g., "Rabi' al-awwal").
 * - "yyyy" for the year.
 *
 * Built as a function so the locale is resolved at format time. Top-level vals
 * would capture Locale.getDefault() once and stay stale after a runtime language
 * change (LocaleManager.setLanguage + activity recreate).
 */
fun hijriFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("dd MMMM yyyy")
        .withLocale(Locale.getDefault())

/* Create a formatter with the desired pattern and locale.
 * - "EEEE" for the full day name (e.g., "Monday").
 * - "MMMM" for the full month name (e.g., "October").
 * - "d" for the day of the month.
 * - "yyyy" for the year.
 */
fun gregorianFullFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("dd MMMM yyyy, EEEE")
        .withLocale(Locale.getDefault())

/* Create a formatter with the desired pattern and locale.
 * - "MMMM" for the full month name (e.g., "October").
 * - "yyyy" for the year.
 */
fun gregorianShortFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("MMMM yyyy")
        .withLocale(Locale.getDefault())

fun gregorianDayAndNameFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("dd EEEE")  // Day number + full day name
        .withLocale(Locale.getDefault())

fun extractWeekdayName(gregorianDayAndName: String): String =
    gregorianDayAndName.substringAfterLast(' ')
