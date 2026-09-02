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
import java.time.Clock
import java.time.Duration
import java.time.ZoneId


/**
 * Created by F.K. on 28.10.2025.
 *
 */

@Factory
class PrayerLogicEngine(
        private val clock: Clock = Clock.systemDefaultZone()
): PrayerLogic {
    override fun findCurrentAndNextPrayer(
            prayers: List<Prayer>,
            zoneId: ZoneId
    ): Pair<Prayer?, Prayer?> {
        val prayerTimes = prayers.filterNot { it.isImsak }
        val currentPrayer = findCurrentPrayer(prayerTimes, zoneId)
        // Handle period before the first prayer (Fajr)
        if (currentPrayer == null) {
            return Pair(prayerTimes.lastOrNull(), prayerTimes.firstOrNull())
        }
        val currentIndex = prayerTimes.indexOf(currentPrayer)
        val nextPrayer = prayerTimes.getOrNull(currentIndex + 1)

        // Handle period after the last prayer (Isha)
        return if (nextPrayer == null) {
            val nextPrayer = prayerTimes.firstOrNull()?.let {
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
            nextPrayerTime: LocalTime,
            zoneId: ZoneId
    ): Duration {
        val now = java.time.LocalTime.now(clock.withZone(zoneId))
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
            prayers: List<Prayer>,
            zoneId: ZoneId
    ): Prayer? = prayers.lastOrNull { prayer ->
        val currentTime = java.time.LocalTime.now(clock.withZone(zoneId))
        !prayer.time.toJavaLocalTime().isAfter(currentTime)
    }

}