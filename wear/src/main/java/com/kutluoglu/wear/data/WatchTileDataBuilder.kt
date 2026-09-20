package com.kutluoglu.wear.data

import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.domain.PrayerTimeEngine
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.core.annotation.Factory
import java.time.ZoneId
import kotlin.time.ExperimentalTime

private const val DHUHR_ARABIC_NAME = "الظهر"

/**
 * Computes [WatchTileData] locally on the watch using the pure-Kotlin prayer
 * calculation engine. This is the fallback for devices where the Google data
 * layer is unavailable (e.g. some Samsung Galaxy Watches report the wearable
 * network as DISCONNECTED), so the tile can still show fresh prayer times
 * without any phone round-trip.
 */
@Factory
class WatchTileDataBuilder(
    private val prayerTimeEngine: PrayerTimeEngine,
    private val prayerLogicEngine: PrayerLogicEngine
) {

    fun build(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        date: LocalDateTime,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        locationName: String,
        prayerNames: List<String>
    ): WatchTileData? {
        val prayers = prayerTimeEngine.calculateDailyPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            date = date,
            calculationMethod = calculationMethod,
            juristicMethod = juristicMethod
        )
        val localizedPrayers = localizeNames(prayers, prayerNames)
        val (current, next) = prayerLogicEngine.findCurrentAndNextPrayer(localizedPrayers, zoneId)
        val nextPrayer = next ?: return null
        val currentPrayerEpochMillis = current?.let { toEpochMillis(it, zoneId) } ?: 0L
        val nextPrayerEpochMillis = toEpochMillis(nextPrayer, zoneId)
        val isJumuah = isJumuahPrayer(nextPrayer)
        return WatchTileData(
            locationName = locationName,
            nextPrayerName = nextPrayer.name,
            nextPrayerEpochMillis = nextPrayerEpochMillis,
            currentPrayerEpochMillis = currentPrayerEpochMillis,
            isJumuah = isJumuah,
            prayers = localizedPrayers.map { prayer ->
                WatchPrayer(
                    name = prayer.name,
                    time = formatClockTime(prayer.time),
                    isNext = prayer.name == nextPrayer.name,
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

    @OptIn(ExperimentalTime::class)
    private fun toEpochMillis(prayer: Prayer, zoneId: ZoneId): Long =
        LocalDateTime(prayer.date, prayer.time)
            .toInstant(TimeZone.of(zoneId.id))
            .toEpochMilliseconds()

    private fun formatClockTime(time: LocalTime): String =
        "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    private fun isJumuahPrayer(prayer: Prayer): Boolean =
        prayer.arabicName == DHUHR_ARABIC_NAME && prayer.date.dayOfWeek == DayOfWeek.FRIDAY
}
