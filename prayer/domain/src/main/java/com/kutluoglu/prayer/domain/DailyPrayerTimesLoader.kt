package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId

@Factory
class DailyPrayerTimesLoader(
    private val source: DailyPrayerTimesSource,
    private val logicEngine: PrayerLogicEngine
) {
    suspend fun load(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        persistDailyCache: Boolean = true
    ): Result<DailyPrayerTimes> =
        source.getDailyPrayerTimes(date, latitude, longitude, zoneId, calculationMethod, juristicMethod, persistDailyCache)
            .map { prayers ->
                val (current, next) = logicEngine.findCurrentAndNextPrayer(prayers, zoneId)
                DailyPrayerTimes(
                    prayers = prayers,
                    currentPrayer = current,
                    nextPrayer = next,
                    currentPrayerEpochMillis = current?.let { toEpochMillis(it, zoneId) } ?: 0L,
                    nextPrayerEpochMillis = next?.let { toEpochMillis(it, zoneId) } ?: 0L,
                    isJumuah = next?.let { isJumuahPrayer(it) } ?: false
                )
            }
}
