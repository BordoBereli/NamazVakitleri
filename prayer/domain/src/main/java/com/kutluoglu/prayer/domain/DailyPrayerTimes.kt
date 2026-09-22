package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.Prayer

/**
 * The daily prayer times for a single day, with the current and next prayers
 * computed from the raw (non-localized) [prayers] list.
 */
data class DailyPrayerTimes(
    val prayers: List<Prayer>,
    val currentPrayer: Prayer?,
    val nextPrayer: Prayer?,
    val currentPrayerEpochMillis: Long,
    val nextPrayerEpochMillis: Long,
    val isJumuah: Boolean
)
