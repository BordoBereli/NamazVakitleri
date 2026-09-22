package com.kutluoglu.wear.data

import com.kutluoglu.prayer.domain.DailyPrayerTimesLoader
import com.kutluoglu.prayer.domain.formatClockTime
import com.kutluoglu.prayer.domain.isJumuahPrayer
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.datetime.LocalDateTime
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
        val localizedPrayers = localizeNames(result.prayers, prayerNames)
        // The loader returns raw (English-named) prayers. The raw current/next are
        // elements of result.prayers (or a date-shifted copy after Isha), so map by
        // index into the localized list (same size/order guaranteed by localizeNames),
        // preserving the raw date.
        val nextPrayer = result.nextPrayer?.let { raw ->
            localizedPrayers.getOrNull(result.prayers.indexOfFirst { it.time == raw.time })?.copy(date = raw.date)
        } ?: return null
        return WatchTileData(
            locationName = locationName,
            nextPrayerName = nextPrayer.name,
            nextPrayerEpochMillis = result.nextPrayerEpochMillis,
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
