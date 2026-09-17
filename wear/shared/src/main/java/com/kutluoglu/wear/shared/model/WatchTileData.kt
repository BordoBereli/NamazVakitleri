package com.kutluoglu.wear.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchTileData(
    val locationName: String,
    val nextPrayerName: String,
    val nextPrayerEpochMillis: Long,
    val currentPrayerEpochMillis: Long,
    val isJumuah: Boolean,
    val prayers: List<WatchPrayer>,
    val syncedAtEpochMillis: Long
)

@Serializable
data class WatchPrayer(
    val name: String,
    val time: String,
    val isNext: Boolean,
    val isJumuah: Boolean = false
)
