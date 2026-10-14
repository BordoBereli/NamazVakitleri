package com.kutluoglu.wear.data

import com.kutluoglu.prayer.domain.DailyPrayerTimesLoader
import com.kutluoglu.prayer.domain.formatClockTime
import com.kutluoglu.prayer.domain.isJumuahPrayer
import com.kutluoglu.prayer.domain.toEpochMillis
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import org.koin.core.annotation.Factory
import java.time.ZoneId

/**
 * Computes [WatchTileData] locally on the watch using the pure-Kotlin prayer
 * calculation engine. This is the fallback for devices where the Google data
 * layer is unavailable (e.g. some Samsung Galaxy Watches report the wearable
 * network as DISCONNECTED), so the tile can still show fresh prayer times
 * without any phone round-trip.
 */
@Factory
class WatchTileDataBuilder(
    private val dailyLoader: DailyPrayerTimesLoader
) {

    /**
     * Computes [WatchTileData] for the given location and date.
     *
     * The caller must pass [date] as the watch's current date in [zoneId]
     * (e.g. `LocalDateTime.now(zoneId)`), consistent with the engine's internal
     * clock used to select the current/next prayer. The phone's
     * `WidgetDataProvider` derives its date from the same clock; if the two
     * disagree, current/next selection will not match the computed date.
     */
    suspend fun build(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        date: LocalDateTime,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        locationName: String,
        prayerNames: List<String>
    ): WatchTileData? {
        val result = dailyLoader.load(
            date = date,
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            juristicMethod = juristicMethod,
            persistDailyCache = false
        ).getOrNull() ?: return null
        // The engine can only date-shift today's Imsak as the after-Isha next
        // prayer; load tomorrow's exact Imsak and patch it in so the watch tile
        // matches the phone widget and the notification scheduler. Fails soft:
        // falls back to the date-shifted time when the tomorrow load fails.
        val nextPrayerRaw = result.nextPrayer
        val isAfterIsha = nextPrayerRaw != null && nextPrayerRaw.date > date.date
        val tomorrowImsakTime = if (isAfterIsha) {
            runCatching {
                dailyLoader.load(
                    date = LocalDateTime(date.date.plus(1, DateTimeUnit.DAY), date.time),
                    latitude = latitude,
                    longitude = longitude,
                    zoneId = zoneId,
                    calculationMethod = calculationMethod,
                    juristicMethod = juristicMethod,
                    persistDailyCache = false
                ).getOrNull()?.prayers?.firstOrNull { it.isImsak }?.time
            }.getOrNull()
        } else {
            null
        }
        val effectiveNextPrayer = if (isAfterIsha && tomorrowImsakTime != null && nextPrayerRaw != null && nextPrayerRaw.isImsak) {
            nextPrayerRaw.copy(time = tomorrowImsakTime)
        } else {
            nextPrayerRaw
        }
        val localizedPrayers = localizeNames(result.prayers, prayerNames)
        // The loader returns raw (English-named) prayers. The raw current/next are
        // elements of result.prayers (or a date-shifted copy after Isha), so map by
        // index into the localized list (same size/order guaranteed by localizeNames),
        // preserving the raw date.
        val nextPrayer = effectiveNextPrayer?.let { raw ->
            val index = result.prayers.indexOfFirst { it.time == raw.time }
            if (index >= 0) {
                localizedPrayers.getOrNull(index)?.copy(date = raw.date)
            } else {
                localizedPrayers.firstOrNull { it.isImsak }?.copy(date = raw.date, time = raw.time) ?: raw
            }
        } ?: return null
        return WatchTileData(
            locationName = locationName,
            nextPrayerName = nextPrayer.name,
            nextPrayerEpochMillis = effectiveNextPrayer?.let { toEpochMillis(it, zoneId) }
                ?: result.nextPrayerEpochMillis,
            currentPrayerEpochMillis = result.currentPrayerEpochMillis,
            isJumuah = result.isJumuah,
            prayers = localizedPrayers.map { prayer ->
                WatchPrayer(
                    name = prayer.name,
                    time = formatClockTime(prayer.time),
                    isNext = prayer.date == nextPrayer.date && prayer.time == nextPrayer.time,
                    isJumuah = isJumuahPrayer(prayer)
                )
            },
            syncedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun localizeNames(prayers: List<Prayer>, prayerNames: List<String>): List<Prayer> {
        if (prayers.size != prayerNames.size) return prayers
        return prayers.mapIndexed { index, prayer -> prayer.copy(name = prayerNames[index]) }
    }
}
