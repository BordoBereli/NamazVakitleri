package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.services.PrayerLogic
import org.koin.core.annotation.Factory
import kotlin.collections.firstOrNull
import kotlin.collections.getOrNull
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalTime
import java.time.Duration
import java.time.ZoneId


/**
 * Created by F.K. on 28.10.2025.
 *
 */

@Factory
class PrayerLogicEngine: PrayerLogic {
    override fun findCurrentAndNextPrayer(
            prayers: List<Prayer>
    ): Pair<Prayer?, Prayer?> {
        val currentPrayer = findCurrentPrayer(prayers)
        // Handle period before the first prayer (Fajr)
        if (currentPrayer == null) {
            return Pair(prayers.lastOrNull(), prayers.firstOrNull())
        }
        val currentIndex = prayers.indexOf(currentPrayer)
        val nextPrayer = prayers.getOrNull(currentIndex + 1)

        // Handle period after the last prayer (Isha)
        return if (nextPrayer == null) {
            val nextPrayer = prayers.firstOrNull()?.let {
                it.copy(
                    date = it.date.plus(1, DateTimeUnit.DAY)
                )
            }
            Pair(currentPrayer, nextPrayer)
        } else {
            Pair(currentPrayer, nextPrayer)
        }
    }

    override fun calculateTimeRemaining(
            nextPrayerTime: LocalTime
    ): Duration {
        val now = java.time.LocalTime.now(ZoneId.systemDefault())
        val duration = Duration.between(
            now,
            nextPrayerTime.toJavaLocalTime()
        )
        return if (duration.isNegative) {
            duration.plusHours(24)
        } else {
            duration
        }
    }

    private fun findCurrentPrayer(
            prayers: List<Prayer>
    ): Prayer? = prayers.lastOrNull { prayer ->
        val currentTime = java.time.LocalTime.now(ZoneId.systemDefault())
        !prayer.time.toJavaLocalTime().isAfter(currentTime)
    }

}