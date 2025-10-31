package com.kutluoglu.prayer_feature.home.common

import com.kutluoglu.core.common.gregorianFormatter
import com.kutluoglu.core.common.hijriFormatter
import com.kutluoglu.core.common.timeFormatter
import com.kutluoglu.core.ui.theme.common.StringResourcesProvider
import com.kutluoglu.core.ui.R.*
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer_feature.home.TimeInfo
import com.kutluoglu.prayer_location.LocationData
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import kotlin.collections.mapIndexed
import kotlin.time.toKotlinDuration

/**
 * Created by F.K. on 29.10.2025.
 *
 */
@Factory
class PrayerFormatter(
    private val resProvider: StringResourcesProvider
) {
    fun getInitialTimeInfo(zoneId: ZoneId): TimeInfo {
        val today = LocalDate.now(zoneId)
        val hijrahDate = HijrahDate.now(zoneId)

        return TimeInfo(
            hijriDate = hijrahDate.format(hijriFormatter),
            gregorianDate = today.format(gregorianFormatter),
            currentTime = getFormattedCurrentTime(zoneId)
        )
    }

    fun getFormattedCurrentTime(zoneId: ZoneId): String {
        return java.time.LocalTime.now(zoneId).format(timeFormatter)
    }

    fun formatTimeRemaining(duration: Duration): String {
        return duration.toKotlinDuration().toComponents { hours, minutes, seconds, _ ->
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
    /**
     * Maps a list of Prayer objects to include localized names from string resources.
     */
    fun withLocalizedNames(prayerTimes: List<Prayer>): List<Prayer> {
        // Get the localized prayer names from the resources provider.
        val prayerNames = resProvider.getStringArray(array.prayers)

        // Ensure the lists can be safely zipped.
        if (prayerTimes.size != prayerNames.size) {
            // Or handle this error more gracefully, but returning the original list is a safe fallback.
            return prayerTimes
        }

        return prayerTimes.mapIndexed { index, prayer ->
            prayer.copy(name = prayerNames[index])
        }
    }

    fun locationInfo(locationData: LocationData): String {
        val countryCode = locationData.countryCode ?: ""
        val city = locationData.city ?: ""
        val county = locationData.county

        return county?.let { "$county, $city - $countryCode" }
            ?: "$city, $countryCode"
    }
}