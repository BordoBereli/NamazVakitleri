package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.common.Result
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
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

@KoinViewModel
class HomeViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getRandomVerseUseCase: GetRandomVerseUseCase,
        private val saveLocationUseCase: SaveLocationUseCase,
        private val getSavedLocationUseCase: GetSavedLocationUseCase,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter,
        private val locationService: LocationService
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Handles UI events such as pull-to-refresh.
     *
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> { loadPrayerTimesForCurrentLocation() }
            HomeEvent.OnCountDown -> { startPrayerCountdown() }
            HomeEvent.OnPermissionsGranted -> { loadPrayerTimesForCurrentLocation() }
            HomeEvent.OnUpdateLocationConfirmed -> { updateLocationChange() }
            HomeEvent.OnLoadQuranVerse -> { loadRandomVerse() }
            HomeEvent.OnVerseClicked -> {
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(
                        data = currentState.data.copy(isVerseDetailSheetVisible = true)
                    )
                }
            }
            HomeEvent.OnVerseDetailDismissed -> {
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(
                        data = currentState.data.copy(isVerseDetailSheetVisible = false)
                    )
                }
            }
        }
    }

    private fun loadRandomVerse() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                getRandomVerseUseCase()
                    .onSuccess {
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(
                                quranVerse = it
                            )
                        )
                    }.onFailure {
                        Log.e("LoadRandomVerse", "Failed to load random verse --> ${it.message}")
                    }
            }
        }
    }

    private fun updateLocationChange() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading // Set loading state
            val newLocation = locationService.getCurrentLocation()
            if (newLocation != null) {
                saveLocationUseCase(newLocation)
                processLocation(newLocation, showedLocationUpdatePrompt = false)
            } else {
                _uiState.value = HomeUiState.Error("Failed to get updated location. Please try again.")
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            when (val savedLocationResult = getSavedLocationUseCase()) {
                is Result.Success -> {
                    val savedLocation = savedLocationResult.data
                    processLocation(savedLocation)

                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null && locationService.isDifferentThen(savedLocation)) {
                        val currentState = _uiState.value
                        if (currentState is HomeUiState.Success) {
                            _uiState.value = currentState.copy(
                                data = currentState.data.copy(showLocationUpdatePrompt = true)
                            )
                        }
                    }
                }
                is Result.Error -> {
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null) {
                        saveLocationUseCase(currentLocation)
                        processLocation(currentLocation)
                    } else {
                        _uiState.value = HomeUiState.Error("Could not get location. Please enable GPS and restart the app.")
                    }
                }
            }
        }
    }

    private suspend fun processLocation(location: LocationData, showedLocationUpdatePrompt: Boolean = false) {
        val zoneId = getZoneIdFromLocation(location.countryCode)
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
                    locationInfo = formatter.locationInfo(location),
                    showLocationUpdatePrompt = showedLocationUpdatePrompt
                )
            )
            _uiState.value = successState
        }.onFailure { error ->
            val errorState = HomeUiState.Error(
                message = error.message ?: "An unknown error occurred while calculating prayer times."
            )
            _uiState.value = errorState
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
                    ),
                    showLocationUpdatePrompt = false
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
