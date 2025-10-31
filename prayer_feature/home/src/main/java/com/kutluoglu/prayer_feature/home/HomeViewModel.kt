package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.home.common.PrayerFormatter
import com.kutluoglu.prayer_location.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.android.annotation.KoinViewModel
import java.time.ZoneId
import java.time.temporal.TemporalQueries.zoneId

@KoinViewModel
class HomeViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter,
        private val locationService: LocationService
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
//        loadPrayerTimes()
//        observeLocationChanges()
    }

    /**
     * Handles UI events such as pull-to-refresh.
     *
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> { loadPrayerTimes() }
            HomeEvent.OnCountDown -> { startPrayerCountdown() }
            HomeEvent.OnPermissionsGranted -> { loadPrayerTimes() }
        }
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            // Using placeholder location values for now 41.03145023904377, 28.80314290541189
//            val latitude = 41.03145023904377 // 41.0082
//            val longitude = 28.80314290541189 //28.9784
            val location = locationService.getCurrentLocation()
            if (location == null) {
                val errorState = HomeUiState.Error(message = "Could not get location. Please enable GPS and try again.")
                _uiState.value = errorState
            } else {
                // 1. Get the ZoneId from the location data.
                val zoneId = getZoneIdFromLocation(location.countryCode)
                // 2. Get the current LocalDateTime for that specific zone.
                val locationDateTime = LocalDateTime.now(zoneId)
                getPrayerTimesUseCase(
                    date = locationDateTime,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoneId = zoneId
                ).onSuccess { prayerTimes ->
                        val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                        val successState = HomeUiState.Success(
                            data = HomeDataUiState(
                                prayers = langDetectedPrayerTimes,
                                timeInfo = formatter.getInitialTimeInfo(zoneId),
                                locationInfo = formatter.locationInfo(location)
                            )
                        )
                        _uiState.value = successState
                    }
                .onFailure { error ->
                        val errorState = HomeUiState.Error(
                            message = error.message ?: "An unknown error occurred"
                        )
                        _uiState.value = errorState
                    }
            }
        }
    }

    fun startPrayerCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                updateCountdown()
                delay(1_000)
            }
        }
    }

    private suspend fun updateCountdown() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val (currentPrayer, nextPrayer) = calculator.findCurrentAndNextPrayer(
                prayers = currentState.data.prayers
            )
            val prayersWithCurrent = currentState.data.prayers.map { prayer ->
                prayer.copy(isCurrent = prayer.name == currentPrayer?.name)
            }
            val timeRemainingString = if (nextPrayer != null) {
                val duration = calculator.calculateTimeRemaining(nextPrayer.time)
                formatter.formatTimeRemaining(duration)
            } else {
                "--:--:--" // Use a placeholder that matches the format
            }

            val location = locationService.getCurrentLocation()
            val zoneId = getZoneIdFromLocation(location?.countryCode)
            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    prayers = prayersWithCurrent,
                    currentPrayer = currentPrayer,
                    nextPrayer = nextPrayer,
                    timeRemaining = timeRemainingString,
                    timeInfo = currentState.data.timeInfo.copy(
                        currentTime = formatter.getFormattedCurrentTime(zoneId = zoneId)
                    )
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
