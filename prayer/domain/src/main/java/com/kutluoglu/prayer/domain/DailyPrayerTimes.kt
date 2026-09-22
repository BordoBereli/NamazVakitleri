package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.Prayer

data class DailyPrayerTimes(
    val prayers: List<Prayer>,            // raw (non-localized) prayers
    val currentPrayer: Prayer?,
    val nextPrayer: Prayer?,
    val currentPrayerEpochMillis: Long,
    val nextPrayerEpochMillis: Long,
    val isJumuah: Boolean
)
