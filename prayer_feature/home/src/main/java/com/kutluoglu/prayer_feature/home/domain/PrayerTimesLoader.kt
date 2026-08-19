package com.kutluoglu.prayer_feature.home.domain

import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId

data class LoadedPrayerData(
    val prayerState: PrayerUiState,
    val timeState: TimeUiState,
    val locationState: LocationUiState,
    val zoneId: ZoneId
)

/**
 * Fetches prayer times for a [LocationData], localizes names, and computes which prayer is
 * current/next. Pure data transformation - no loops, no lifecycle.
 */
@Factory
class PrayerTimesLoader(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter
) {
    suspend fun load(location: LocationData, calculationMethod: CalculationMethod): Result<LoadedPrayerData> {
        val zoneId = getZoneIdFromLocation(location.countryCode)
        val locationDateTime = LocalDateTime.now(zoneId)
        return getPrayerTimesUseCase(
            date = locationDateTime,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = calculationMethod
        ).map { prayerTimes ->
            val localized = formatter.withLocalizedNames(prayerTimes)
            LoadedPrayerData(
                prayerState = computePrayerState(localized, zoneId),
                timeState = formatter.getInitialTimeInfo(zoneId),
                locationState = LocationUiState(
                    locationData = location,
                    locationInfoText = formatter.locationInfo(location)
                ),
                zoneId = zoneId
            )
        }
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
