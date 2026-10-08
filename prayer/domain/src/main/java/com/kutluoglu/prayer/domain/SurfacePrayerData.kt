package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Surface-agnostic snapshot of today's prayer data shared by the widget,
 * the Wear tile, and the Android Auto screens. Contains everything a surface
 * needs to render: location, current/next prayer, epoch millis, and the
 * localized prayer list. [tomorrowImsakTime] is only populated after Isha,
 * when the next prayer falls on tomorrow's date.
 */
data class SurfacePrayerData(
    val location: LocationData,
    val city: String,
    val district: String?,
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val nextPrayerLocalTime: LocalTime,
    val currentPrayerLocalTime: LocalTime?,
    val nextPrayerEpochMillis: Long,
    val currentPrayerEpochMillis: Long,
    val isJumuah: Boolean,
    val hijriAdjustment: Int,
    val prayers: List<Prayer>,
    val nextPrayerDate: LocalDate,
    val tomorrowImsakTime: LocalTime? = null
)
