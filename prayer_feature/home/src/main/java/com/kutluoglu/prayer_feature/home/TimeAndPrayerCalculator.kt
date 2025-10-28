package com.kutluoglu.prayer_feature.home// You can create a new file for this, e.g., TimeAndPrayerCalculator.kt

import com.kutluoglu.core.common.gregorianFormatter
import com.kutluoglu.core.common.hijriFormatter
import com.kutluoglu.core.common.timeFormatter
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import kotlin.time.toKotlinDuration

@Factory
class TimeAndPrayerCalculator {

    fun getInitialTimeInfo(): TimeInfo {
        val today = LocalDate.now(ZoneId.systemDefault())
        val hijrahDate = HijrahDate.from(today)

        return TimeInfo(
            hijriDate = hijrahDate.format(hijriFormatter),
            gregorianDate = today.format(gregorianFormatter),
            currentTime = getCurrentTime()
        )
    }

    fun getCurrentTime(): String {
        return java.time.LocalTime.now(ZoneId.systemDefault()).format(timeFormatter)
    }

    fun findCurrentAndNextPrayer(
        prayers: List<Prayer>,
        currentTime: java.time.LocalTime
    ): Pair<Prayer?, Prayer?> {
        val currentPrayer = findCurrentPrayer(prayers, currentTime)
        val currentIndex = if (currentPrayer != null) prayers.indexOf(currentPrayer) else -1
        val nextPrayer = if (currentIndex != -1) prayers.getOrNull(currentIndex + 1) else prayers.firstOrNull()

        // Handle the case where the next prayer is Fajr of the next day
        return if (currentPrayer != null && nextPrayer == null) {
            Pair(currentPrayer, prayers.firstOrNull())
        } else {
            Pair(currentPrayer, nextPrayer)
        }
    }

    fun findCurrentPrayer(
        prayers: List<Prayer>,
        currentTime: java.time.LocalTime
    ): Prayer? = prayers.lastOrNull { prayer ->
        !prayer.time.toJavaLocalTime().isAfter(currentTime)
    }

    fun calculateTimeRemaining(nextPrayerTime: LocalTime): Duration {
        val now = java.time.LocalTime.now()
        val duration = Duration.between(now, nextPrayerTime.toJavaLocalTime())

        return if (duration.isNegative) {
            duration.plusHours(24)
        } else {
            duration
        }
    }

    fun formatTimeRemaining(duration: Duration): String {
        return duration.toKotlinDuration().toComponents { hours, minutes, seconds, _ ->
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}
