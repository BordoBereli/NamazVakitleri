package com.kutluoglu.prayer_feature.home.common

import com.kutluoglu.core.common.gregorianFormatter
import com.kutluoglu.core.common.hijriFormatter
import com.kutluoglu.core.common.timeFormatter
import com.kutluoglu.prayer_feature.home.TimeInfo
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import kotlin.time.toKotlinDuration

/**
 * Created by F.K. on 29.10.2025.
 *
 */
@Factory
class PrayerFormatter {
    fun getInitialTimeInfo(): TimeInfo {
        val today = LocalDate.now(ZoneId.systemDefault())
        val hijrahDate = HijrahDate.now(ZoneId.systemDefault())

        return TimeInfo(
            hijriDate = hijrahDate.format(hijriFormatter),
            gregorianDate = today.format(gregorianFormatter),
            currentTime = getFormattedCurrentTime()
        )
    }

    fun getFormattedCurrentTime(): String {
        return java.time.LocalTime.now(ZoneId.systemDefault()).format(timeFormatter)
    }

    fun formatTimeRemaining(duration: Duration): String {
        return duration.toKotlinDuration().toComponents { hours, minutes, seconds, _ ->
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}