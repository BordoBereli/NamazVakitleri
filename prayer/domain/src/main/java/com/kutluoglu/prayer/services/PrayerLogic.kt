package com.kutluoglu.prayer.services

import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalTime
import java.time.Duration
import java.time.ZoneId


/**
 * Created by F.K. on 28.10.2025.
 *
 */
interface PrayerLogic {
    fun findCurrentAndNextPrayer(
            prayers: List<Prayer>,
            zoneId: ZoneId
    ): Pair<Prayer?, Prayer?>

    fun calculateTimeRemaining(
            nextPrayerTime: LocalTime,
            zoneId: ZoneId
    ): Duration
}