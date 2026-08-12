package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.home.domain.CountdownEngine
import com.kutluoglu.prayer_feature.home.domain.LocationCoordinator
import com.kutluoglu.prayer_feature.home.domain.PrayerTimesLoader
import com.kutluoglu.prayer_feature.home.domain.QuranVerseLoader
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeErrorMapper
import com.kutluoglu.prayer_feature.home.state.HomeScreenGate
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import com.kutluoglu.prayer_feature.home.state.QuranUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class HomeViewModel(
    private val locationCoordinator: LocationCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader
) : ViewModel() {

    private val _screenGate = MutableStateFlow<HomeScreenGate>(HomeScreenGate.Loading)
    val screenGate: StateFlow<HomeScreenGate> = _screenGate

    private val _timeState = MutableStateFlow<TimeUiState?>(null)
    val timeState: StateFlow<TimeUiState?> = _timeState

    private val _locationState = MutableStateFlow<LocationUiState?>(null)
    val locationState: StateFlow<LocationUiState?> = _locationState

    private val _prayerState = MutableStateFlow<PrayerUiState?>(null)
    val prayerState: StateFlow<PrayerUiState?> = _prayerState

    private val _promptState = MutableStateFlow(false)
    val promptState: StateFlow<Boolean> = _promptState

    val countdownState: StateFlow<CountdownUiState> = countdownEngine.countdownState
    val quranState: StateFlow<QuranUiState> = quranVerseLoader.quranState

    private var locationObserverJob: Job? = null
    private var settingsObserverJob: Job? = null
    private var prayerPassedObserverJob: Job? = null
    private var dayChangedObserverJob: Job? = null

    init {
        locationObserverJob = viewModelScope.launch {
            locationCoordinator.observeLocationChanges().collect { onLocationResolved(it) }
        }
        settingsObserverJob = viewModelScope.launch {
            locationCoordinator.observeSettingsChanges().collect { onLocationResolved(it) }
        }
        prayerPassedObserverJob = viewModelScope.launch {
            countdownEngine.prayerPassedSignal.collect { refreshPrayerState() }
        }
        dayChangedObserverJob = viewModelScope.launch {
            countdownEngine.dayChangedSignal.collect { loadPrayerTimesForCurrentLocation() }
        }
        loadInitialLocation()
    }

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

    private fun loadInitialLocation() {
        viewModelScope.launch {
            val location = locationCoordinator.resolveInitial()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val location = locationCoordinator.resolveSavedAndDetectDrift()
            if (location != null) {
                onLocationResolved(location)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    private fun updateLocationChange() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val newLocation = locationCoordinator.refreshFromGps()
            if (newLocation != null) {
                onLocationResolved(newLocation)
            } else {
                _screenGate.value = HomeScreenGate.Error(
                    "Failed to get updated location. Please try again."
                )
            }
        }
    }

    private suspend fun onLocationResolved(location: LocationData) {
        prayerTimesLoader.load(location)
            .onSuccess { loaded ->
                _locationState.value = loaded.locationState
                _timeState.value = loaded.timeState
                _prayerState.value = loaded.prayerState
                _promptState.value = locationCoordinator.locationUpdatePrompt.value
                _screenGate.value = HomeScreenGate.Ready
                startCountdownFromCurrentState()
            }
            .onFailure { error ->
                _screenGate.value = HomeScreenGate.Error(
                    error.message ?: HomeErrorMapper.getUserFriendlyErrorMessage(error)
                )
            }
    }

    private fun refreshPrayerState() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        val refreshed = prayerTimesLoader.computePrayerState(currentState.prayers, zoneId)
        _prayerState.value = refreshed
        _screenGate.value = HomeScreenGate.Ready
    }

    private fun startPrayerCountdown() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        countdownEngine.start(currentState, zoneId, viewModelScope)
    }

    private fun startCountdownFromCurrentState() {
        val currentState = _prayerState.value ?: return
        val zoneId = getZoneIdFromLocation(_locationState.value?.locationData?.countryCode)
        countdownEngine.start(currentState, zoneId, viewModelScope)
    }

    private fun loadRandomVerse() {
        quranVerseLoader.loadVerse(
            scope = viewModelScope,
            isScreenReady = { _screenGate.value == HomeScreenGate.Ready }
        )
    }

    private fun setVerseSheetVisibility(isVisible: Boolean) {
        quranVerseLoader.setSheetVisible(isVisible)
    }

    private fun fail(message: String) {
        _screenGate.value = HomeScreenGate.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        countdownEngine.stop()
        locationObserverJob?.cancel()
        settingsObserverJob?.cancel()
        prayerPassedObserverJob?.cancel()
        dayChangedObserverJob?.cancel()
    }
}
