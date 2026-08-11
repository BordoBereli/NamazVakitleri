package com.kutluoglu.prayer_feature.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private var savedLocation: LocationData? = null
    private var zoneId: ZoneId? = null
    private var selectedMonth: YearMonth = LocalDateTime.now(ZoneId.systemDefault()).date.yearMonth
    private val monthCache = mutableMapOf<YearMonth, List<DailyPrayer>>()
    private var isLoading = false
    private var pendingMonth: YearMonth? = null

    fun loadMonthlyPrayerTimes() {
        viewModelScope.launch {
            getSavedLocationUseCase()
                .onSuccess { location ->
                    savedLocation = location
                    val resolvedZoneId = getZoneIdFromLocation(location.countryCode)
                    zoneId = resolvedZoneId
                    val today = LocalDateTime.now(resolvedZoneId)
                    selectedMonth = today.date.yearMonth
                    loadMonth(selectedMonth)
                }
                .onFailure {
                    _uiState.value = PrayerTimesUiState.Error(it.message ?: "Failed to get saved location.")
                }
        }
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.OnPreviousMonth -> navigateToMonth(selectedMonth.minus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnNextMonth -> navigateToMonth(selectedMonth.plus(1, DateTimeUnit.MONTH))
            PrayerTimesEvent.OnToday -> navigateToMonth(currentMonth())
        }
    }

    private fun navigateToMonth(month: YearMonth) {
        selectedMonth = month
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
            try {
                val cached = monthCache[month]
                if (cached != null) {
                    emitSuccess(month, cached)
                    return@launch
                }
                val location = savedLocation ?: return@launch
                val resolvedZoneId = zoneId ?: return@launch
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
                monthCache[month] = monthlyPrayers
                emitSuccess(month, monthlyPrayers)
            } finally {
                isLoading = false
                val next = pendingMonth
                pendingMonth = null
                if (next != null && next != month) {
                    loadMonth(next)
                }
            }
        }
    }

    private fun emitSuccess(month: YearMonth, monthlyPrayers: List<DailyPrayer>) {
        val location = savedLocation ?: return
        val resolvedZoneId = zoneId ?: return
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
}
