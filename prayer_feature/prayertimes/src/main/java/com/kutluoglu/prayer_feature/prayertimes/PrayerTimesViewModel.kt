package com.kutluoglu.prayer_feature.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.yearMonth
import org.koin.android.annotation.KoinViewModel
import java.time.ZoneId
import java.time.chrono.HijrahDate

@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val activeLocationProvider: ActiveLocationProvider,
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

    private var activeLocationId: String? = null
    private var zoneId: ZoneId? = null
    private val monthCache = mutableMapOf<String, MutableMap<YearMonth, List<DailyPrayer>>>()
    private val selectedMonthByLocation = mutableMapOf<String, YearMonth>()
    private var isLoading = false
    private var pendingMonth: YearMonth? = null

    fun loadMonthlyPrayerTimes() {
        viewModelScope.launch {
            val location = activeLocationProvider.location.first() ?: run {
                _uiState.value = PrayerTimesUiState.Error("Failed to get active location.")
                return@launch
            }
            activeLocationId = location.locationId()
            val resolvedZoneId = getZoneIdFromLocation(location.countryCode)
            zoneId = resolvedZoneId
            val today = LocalDateTime.now(resolvedZoneId)
            val month = selectedMonthByLocation[activeLocationId] ?: today.date.yearMonth
            loadMonth(month)
        }
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.OnPreviousMonth -> navigateToMonth(selectedMonth().minus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnNextMonth -> navigateToMonth(selectedMonth().plus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnToday -> navigateToMonth(currentMonth())
        }
    }

    private fun selectedMonth(): YearMonth {
        val id = activeLocationId ?: return currentMonth()
        return selectedMonthByLocation[id] ?: currentMonth()
    }

    private fun navigateToMonth(month: YearMonth) {
        activeLocationId?.let { selectedMonthByLocation[it] = month }
        val current = _uiState.value
        if (current is PrayerTimesUiState.Success) {
            _uiState.value = current.copy(
                selectedMonth = month,
                isCurrentMonth = month == currentMonth()
            )
        }
        loadMonth(month)
    }

    private fun currentMonth(): YearMonth =
        LocalDateTime.now(zoneId ?: ZoneId.systemDefault()).date.yearMonth

    private fun loadMonth(month: YearMonth) {
        if (isLoading) {
            pendingMonth = month
            return
        }
        isLoading = true
        viewModelScope.launch {
            val locationId = activeLocationId ?: run {
                isLoading = false
                return@launch
            }
            val location = activeLocationProvider.location.value ?: run {
                isLoading = false
                return@launch
            }
            val resolvedZoneId = zoneId ?: run {
                isLoading = false
                return@launch
            }
            try {
                val locationCache = monthCache.getOrPut(locationId) { mutableMapOf() }
                val cached = locationCache[month]
                if (cached != null) {
                    emitSuccess(month, cached, locationId, location, resolvedZoneId)
                    return@launch
                }
                val today = LocalDateTime.now(resolvedZoneId)
                val monthlyPrayers = mutableListOf<DailyPrayer>()
                for (day in 1..month.numberOfDays) {
                    val date = month.onDay(day)
                    getPrayerTimesUseCase(
                        date = date.atTime(0, 0),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        zoneId = resolvedZoneId
                    ).onSuccess { prayerTimes ->
                        val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                        val isToday = date == today.date
                        val (currentPrayer, _) = if (isToday) {
                            calculator.findCurrentAndNextPrayer(langDetectedPrayerTimes, resolvedZoneId)
                        } else {
                            Pair(null, null)
                        }
                        val prayersWithCurrent = langDetectedPrayerTimes.map {
                            it.copy(isCurrent = isToday && it.name == currentPrayer?.name)
                        }
                        val timeState = formatter.getInitialTimeInfo(
                            resolvedZoneId,
                            date.toJavaLocalDate(),
                            HijrahDate.from(date.toJavaLocalDate())
                        )
                        monthlyPrayers.add(
                            DailyPrayer(
                                dayOfMonth = date.day,
                                prayers = prayersWithCurrent,
                                gregorianDate = timeState.gregorianDayAndName,
                                hijriDate = timeState.hijriDate
                            )
                        )
                    }.onFailure {
                        _uiState.value = PrayerTimesUiState.Error(
                            it.message ?: "Failed to load prayer times for day $day."
                        )
                        return@launch
                    }
                }
                locationCache[month] = monthlyPrayers
                emitSuccess(month, monthlyPrayers, locationId, location, resolvedZoneId)
            } finally {
                isLoading = false
                val next = pendingMonth
                pendingMonth = null
                if (next != null && (next != month || locationId != activeLocationId)) {
                    loadMonth(next)
                }
            }
        }
    }

    private fun emitSuccess(
        month: YearMonth,
        monthlyPrayers: List<DailyPrayer>,
        locationId: String,
        location: LocationData,
        resolvedZoneId: ZoneId
    ) {
        if (month != selectedMonth() || locationId != activeLocationId) return
        val today = LocalDateTime.now(resolvedZoneId)
        _uiState.value = PrayerTimesUiState.Success(
            monthlyPrayers = monthlyPrayers,
            currentDayOfMonth = today.day,
            selectedMonth = month,
            isCurrentMonth = month == today.date.yearMonth,
            timeState = formatter.getInitialTimeInfo(resolvedZoneId),
            locationState = LocationUiState(
                locationData = location,
                locationInfoText = formatter.locationInfo(location)
            )
        )
    }

    private fun LocationData.locationId(): String = "$latitude,$longitude"
}
