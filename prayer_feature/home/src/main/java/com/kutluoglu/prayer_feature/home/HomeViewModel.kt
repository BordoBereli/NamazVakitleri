package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_feature.common.LanguageProvider
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_location.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
        private val languageProvider: LanguageProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState>
        get() = _uiState
            .stateIn(
                scope = viewModelScope,
                initialValue = HomeUiState.Loading,
                started = SharingStarted.WhileSubscribed(5_000)
            )

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
            HomeEvent.OnVerseClicked -> { setVerseSheetVisibility(isVisible = true) }
            HomeEvent.OnVerseDetailDismissed -> { setVerseSheetVisibility(isVisible = false) }
        }
    }

    private fun setVerseSheetVisibility(isVisible: Boolean) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.value = currentState.copy(isVerseDetailSheetVisible = isVisible)
        }
    }

    private fun loadRandomVerse(delayMillis: Long = 1000L) {
        viewModelScope.launch {
            val currentState = _uiState.value
            when(currentState) {
                is HomeUiState.Success -> {
                    val language = languageProvider.getLanguageCode()
                    getRandomVerseUseCase(language)
                        .onSuccess { verse ->
                            _uiState.value = currentState.copy(quranVerse = verse)
                        }.onFailure {
                            Log.e("LoadRandomVerse", "Failed to load random verse -> ${it.message}")
                        }
                }
                is HomeUiState.Loading, is HomeUiState.Error -> {
                    delay(delayMillis)
                    val nextDelay = (delayMillis * 2).coerceAtMost(30_000L)
                    loadRandomVerse(nextDelay)
                }
            }
        }
    }

    private fun updateLocationChange() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val newLocation = locationService.getCurrentLocation()
            if (newLocation != null) {
                saveLocationUseCase(newLocation)
                processLocationForPrayerTimes(newLocation)
            } else {
                _uiState.value = HomeUiState.Error(
                    "Failed to get updated location. Please try again."
                )
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getSavedLocationUseCase()
                .onSuccess{ savedLocation ->
                    processLocationForPrayerTimes(savedLocation)
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null && locationService.isDifferentThen(savedLocation)) {
                        val successState = _uiState.value as? HomeUiState.Success
                        if (successState != null) {
                            _uiState.value = successState.copy(showLocationUpdatePrompt = true)
                        }
                    }
                }.onFailure {
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null) {
                        saveLocationUseCase(currentLocation)
                        processLocationForPrayerTimes(currentLocation)
                    } else {
                        _uiState.value = HomeUiState.Error(
                            "Could not get location. Please enable GPS and restart the app."
                        )
                    }
                }
        }
    }

    private suspend fun processLocationForPrayerTimes(location: LocationData) {
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
                prayerState = PrayerUiState(prayers = langDetectedPrayerTimes),
                timeState = formatter.getInitialTimeInfo(zoneId),
                locationState = LocationUiState(
                    locationData = location,
                    locationInfoText = formatter.locationInfo(location)
                )
            )
            _uiState.value = successState
            updatePrayerState()
        }.onFailure { error ->
            _uiState.value = HomeUiState.Error(
                message = error.message ?: "An unknown error occurred while calculating prayer times."
            )
        }
    }

    private fun updatePrayerState() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val (currentPrayer, nextPrayer) =
            calculator.findCurrentAndNextPrayer(currentState.prayerState.prayers)
        val prayersWithCurrent = currentState.prayerState.prayers.map { prayer ->
            currentPrayer?.let {
                prayer.copy(isCurrent = prayer.name == it.name)
            } ?: prayer.copy(isCurrent = false)
        }

        _uiState.value = currentState.copy(
            prayerState = currentState.prayerState.copy(
                prayers = prayersWithCurrent,
                currentPrayer = currentPrayer,
                nextPrayer = nextPrayer
            )
        )
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

    private fun updateCountdown() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val nextPrayer = currentState.prayerState.nextPrayer
        val zoneId = getZoneIdFromLocation(currentState.locationState.locationData.countryCode)
        val currentTime = LocalDateTime.now(zoneId)
        val currentTimeString = formatter.getFormattedCurrentTime(zoneId)

        if (nextPrayer != null) {
            val currentPrayer = currentState.prayerState.currentPrayer
            if (currentPrayer != null && nextPrayer.date != currentPrayer.date) {
                if(isDayChanged(prayerDate = currentPrayer.date, currentDeviceDate = currentTime.date)) {
                    countdownJob?.cancel()
                    loadPrayerTimesForCurrentLocation()
                    return
                }
            }

            val nextPrayerDateTime = LocalDateTime(date = nextPrayer.date, time = nextPrayer.time)
            if (currentTime >= nextPrayerDateTime) {
                updatePrayerState()
                return
            }

            val duration = calculator.calculateTimeRemaining(nextPrayer.time)
            val timeRemainingString = formatter.formatTimeRemaining(duration)
            _uiState.value = currentState.copy(
                prayerState = currentState.prayerState.copy(timeRemaining = timeRemainingString),
                timeState = currentState.timeState.copy(currentTime = currentTimeString)
            )
        } else {
            val currentDeviceDate = currentTime.date
            val prayerDate = currentState.prayerState.prayers.firstOrNull()?.date
            if (isDayChanged(prayerDate, currentDeviceDate)) {
                countdownJob?.cancel()
                loadPrayerTimesForCurrentLocation()
            } else {
                _uiState.value = currentState.copy(
                    prayerState = currentState.prayerState.copy(timeRemaining = "--:--:--"),
                    timeState = currentState.timeState.copy(currentTime = currentTimeString)
                )
            }
        }
    }

    private fun isDayChanged(
            prayerDate: LocalDate?,
            currentDeviceDate: LocalDate
    ): Boolean = prayerDate != null && currentDeviceDate != prayerDate

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
