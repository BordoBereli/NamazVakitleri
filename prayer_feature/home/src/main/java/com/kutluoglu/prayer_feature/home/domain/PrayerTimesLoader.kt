package com.kutluoglu.prayer_feature.home.domain

import com.kutluoglu.core.designsystem.prayerUtils.PrayerFormatter
import com.kutluoglu.core.designsystem.states.TimeUiState
import com.kutluoglu.prayer.domain.DailyPrayerTimesLoader
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.resolveZoneId
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.core.annotation.Factory
import java.time.Clock
import java.time.ZoneId

data class LoadedPrayerData(
    val prayerState: PrayerUiState,
    val timeState: TimeUiState,
    val locationState: LocationUiState,
    val zoneId: ZoneId,
    val nextImsakTime: LocalTime? = null
)

/**
 * Fetches prayer times for a [LocationData], localizes names, and computes which prayer is
 * current/next. Pure data transformation - no loops, no lifecycle.
 */
@Factory
class PrayerTimesLoader(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val dailyLoader: DailyPrayerTimesLoader,
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun load(
        location: LocationData,
        calculationMethod: CalculationMethod,
        hijriAdjustment: Int = 0,
        juristicMethod: JuristicMethod = JuristicMethod.STANDARD
    ): Result<LoadedPrayerData> {
        val zoneId = resolveZoneId(location)
        val locationDateTime = java.time.LocalDateTime.now(clock.withZone(zoneId)).toKotlinLocalDateTime()
        return dailyLoader.load(
            date = locationDateTime,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod,
            juristicMethod = juristicMethod,
            persistDailyCache = true
        ).map { result ->
            val localized = formatter.withLocalizedNames(result.prayers)
            LoadedPrayerData(
                prayerState = buildPrayerState(result.prayers, localized, result.currentPrayer, result.nextPrayer),
                timeState = formatter.getInitialTimeInfo(zoneId, hijriAdjustment = hijriAdjustment),
                locationState = LocationUiState(
                    locationData = location,
                    locationInfoText = formatter.locationInfo(location)
                ),
                zoneId = zoneId,
                nextImsakTime = loadNextImsakTime(
                    location = location,
                    zoneId = zoneId,
                    calculationMethod = calculationMethod,
                    juristicMethod = juristicMethod,
                    today = locationDateTime
                )
            )
        }
    }

    /**
     * Builds the [PrayerUiState] from the loader's current/next prayers. The loader
     * returns raw (English-named) prayers. The raw current/next are elements of
     * [rawPrayers] (or a date-shifted copy after Isha), so they are mapped by index
     * into the localized list (same size/order guaranteed by `withLocalizedNames`),
     * preserving the raw date. The [isCurrent] flag is computed by time + date, not
     * by name, so it stays correct across languages.
     */
    private fun buildPrayerState(
        rawPrayers: List<Prayer>,
        localized: List<Prayer>,
        currentPrayer: Prayer?,
        nextPrayer: Prayer?
    ): PrayerUiState {
        val localizedCurrent = currentPrayer?.let { raw ->
            localized.getOrNull(rawPrayers.indexOfFirst { it.time == raw.time })?.copy(date = raw.date)
        }
        val localizedNext = nextPrayer?.let { raw ->
            localized.getOrNull(rawPrayers.indexOfFirst { it.time == raw.time })?.copy(date = raw.date)
        }
        val prayersWithCurrent = localized.map { prayer ->
            prayer.copy(
                isCurrent = currentPrayer?.let { prayer.time == it.time && prayer.date == it.date } ?: false
            )
        }
        return PrayerUiState(
            prayers = prayersWithCurrent,
            currentPrayer = localizedCurrent,
            nextPrayer = localizedNext
        )
    }

    private suspend fun loadNextImsakTime(
        location: LocationData,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        today: LocalDateTime
    ): LocalTime? {
        val tomorrow = today.date.plus(1, DateTimeUnit.DAY)
            .atTime(today.hour, today.minute, today.second, today.nanosecond)
        return runCatching {
            getPrayerTimesUseCase(
                date = tomorrow,
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = zoneId,
                calculationMethod = calculationMethod,
                juristicMethod = juristicMethod,
                persistDailyCache = false
            ).getOrNull()?.firstOrNull { it.isImsak }?.time
        }.getOrNull()
    }

    /** Recomputes current/next + isCurrent flags. Mirrors the old updatePrayerState. */
    fun computePrayerState(prayers: List<Prayer>, zoneId: ZoneId): PrayerUiState {
        val (currentPrayer, nextPrayer) =
            calculator.findCurrentAndNextPrayer(prayers, zoneId)
        val prayersWithCurrent = prayers.map { prayer ->
            currentPrayer?.let {
                prayer.copy(isCurrent = prayer.name == it.name)
            } ?: prayer.copy(isCurrent = false)
        }
        return PrayerUiState(
            prayers = prayersWithCurrent,
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer
        )
    }
}
