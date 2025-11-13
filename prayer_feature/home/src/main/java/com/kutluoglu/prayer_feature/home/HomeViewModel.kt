package com.kutluoglu.prayer_feature.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
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
        private val locationService: LocationService,
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
            HomeEvent.OnRefresh -> { loadPrayerTimes() }
            HomeEvent.OnCountDown -> { startPrayerCountdown() }
            HomeEvent.OnPermissionsGranted -> { loadPrayerTimes() }
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

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val savedLocation = getSavedLocationUseCase()
            val currentLocation = locationService.getCurrentLocation()

            // If cache is empty, fetch from the service
            if (savedLocation == null) {
                // First time use or cache cleared: use current location and save it.
                if (currentLocation == null) {
                    _uiState.value = HomeUiState.Error("Could not get location. Please enable GPS and try again.")
                } else {
                    saveLocationUseCase(currentLocation)
                    processLocation(currentLocation)
                }
            } else {
                // We have a saved location, load it immediately for a fast UI response.
                processLocation(savedLocation)

                // Now, check if the user has moved.
                isLocationChanged(currentLocation, savedLocation)
            }
        }
    }

    private fun isLocationChanged(
            currentLocation: LocationData?,
            savedLocation: LocationData
    ) {
        if (currentLocation != null && isLocationSignificantlyDifferent(
                savedLocation,
                currentLocation
            )
        ) {
            // The locations are different. Update the UI to show a prompt.
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                _uiState.value = currentState.copy(
                    data = currentState.data.copy(
                        showLocationUpdatePrompt = true
                    )
                )
            }
        }
    }

    // Extracted the success logic into a separate private function for cleanliness
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

    /**
     * Helper function to check if two locations are far enough apart to warrant an update.
     * Using a simple distance check (e.g., > 1km).
     */
    private fun isLocationSignificantlyDifferent(loc1: LocationData, loc2: LocationData): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        val distanceInMeters = results[0]
        return distanceInMeters > 1000 // Considered different if more than 1 kilometer apart
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
