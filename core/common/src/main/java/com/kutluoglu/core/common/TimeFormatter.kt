package com.kutluoglu.core.common

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

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
 */
val hijriFormatter = DateTimeFormatter
    .ofPattern("dd MMMM yyyy")
    .withLocale(Locale.getDefault())

/* Create a formatter with the desired pattern and locale.
 * - "EEEE" for the full day name (e.g., "Monday").
 * - "MMMM" for the full month name (e.g., "October").
 * - "d" for the day of the month.
 * - "yyyy" for the year.
 */
val gregorianFormatter = DateTimeFormatter
    .ofPattern("dd MMMM yyyy, EEEE")
    .withLocale(Locale.getDefault())
