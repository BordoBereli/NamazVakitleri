package com.kutluoglu.prayer_feature.prayertimes

/**
 * Created by F.K. on 20.12.2025.
 *
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.core.common.startOfMonth
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import org.koin.android.annotation.KoinViewModel
import java.time.chrono.HijrahDate

@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getSavedLocationUseCase: GetSavedLocationUseCase,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerTimesUiState>(PrayerTimesUiState.Loading)
    val uiState: StateFlow<PrayerTimesUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrayerTimesUiState.Loading
        )

    fun loadMonthlyPrayerTimes() {
        viewModelScope.launch {
            getSavedLocationUseCase()
                .onSuccess { savedLocation ->
                    val zoneId = getZoneIdFromLocation(savedLocation.countryCode)
                    val today = LocalDateTime.now(zoneId)
                    val startOfMonth = today.startOfMonth()
                    val endOfMonth = startOfMonth
                        .plus(1, DateTimeUnit.MONTH)
                        .minus(1, DateTimeUnit.DAY)
                    val daysInMonth = startOfMonth.daysUntil(endOfMonth) + 1
                    val monthlyPrayers = mutableListOf<DailyPrayer>()
                    for (day in 0 until daysInMonth) {
                        val date = startOfMonth.plus(day, DateTimeUnit.DAY)
                        getPrayerTimesUseCase(
                            date = date.atTime(0,0), // Use the date for the loop
                            latitude = savedLocation.latitude,
                            longitude = savedLocation.longitude,
                            zoneId = zoneId
                        ).onSuccess { prayerTimes ->
                            val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                            // isCurrent logic is only relevant for today's prayers
                            val isToday = date == today.date
                            val (currentPrayer, _) = if(isToday) calculator.findCurrentAndNextPrayer(langDetectedPrayerTimes) else Pair(null, null)
                            val prayersWithCurrent = langDetectedPrayerTimes.map {
                                it.copy(isCurrent = isToday && it.name == currentPrayer?.name)
                            }
                            val timeState = formatter.getInitialTimeInfo(
                                zoneId, date.toJavaLocalDate(), HijrahDate.from(date.toJavaLocalDate())
                            )
                            // You would also get daily gregorian/hijri dates from your formatter here
                            monthlyPrayers.add(
                                DailyPrayer(
                                    dayOfMonth = date.day,
                                    prayers = prayersWithCurrent,
                                    gregorianDate = timeState.gregorianDayAndName,
                                    hijriDate = timeState.hijriDate
                                )
                            )
                        }.onFailure {
                            _uiState.value = PrayerTimesUiState.Error(it.message ?: "Failed to load prayer times for day $day.")
                            return@launch // Exit if any day fails
                        }
                    }

                    _uiState.value = PrayerTimesUiState.Success(
                        monthlyPrayers = monthlyPrayers,
                        currentDayOfMonth = today.day,
                        timeState = formatter.getInitialTimeInfo(zoneId),
                        locationState = LocationUiState(
                            locationData = savedLocation,
                            locationInfoText = formatter.locationInfo(savedLocation)
                        )
                    )

                }.onFailure {
                    _uiState.value =
                        PrayerTimesUiState.Error(it.message ?: "Failed to get saved location.")
                }
        }
    }
}