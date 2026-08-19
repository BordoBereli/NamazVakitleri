package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_feature.home.domain.CountdownEngine
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.domain.PrayerTimesLoader
import com.kutluoglu.prayer_feature.home.domain.QuranVerseLoader
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeErrorMapper
import com.kutluoglu.prayer_feature.home.state.HomeScreenGate
import com.kutluoglu.prayer_feature.home.state.QuranUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.annotation.KoinViewModel

@OptIn(FlowPreview::class)
@KoinViewModel
class HomeViewModel(
    private val locationsCoordinator: LocationsCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _screenGate = MutableStateFlow<HomeScreenGate>(HomeScreenGate.Loading)
    val screenGate: StateFlow<HomeScreenGate> = _screenGate

    private val _locationsState = MutableStateFlow<LocationsState>(LocationsState())
    val locationsState: StateFlow<LocationsState> = _locationsState

    private val _prayerDataByLocation = MutableStateFlow<Map<String, LoadedPrayerData>>(emptyMap())
    val prayerDataByLocation: StateFlow<Map<String, LoadedPrayerData>> = _prayerDataByLocation

    private val _activeLocationId = MutableStateFlow<String?>(null)
    val activeLocationId: StateFlow<String?> = _activeLocationId

    val countdownState: StateFlow<CountdownUiState> = countdownEngine.countdownState
    val quranState: StateFlow<QuranUiState> = quranVerseLoader.quranState

    private val stateMutex = Mutex()

    private var locationsObserverJob: Job? = null
    private var prayerPassedObserverJob: Job? = null
    private var dayChangedObserverJob: Job? = null
    private var settingsObserverJob: Job? = null

    init {
        locationsObserverJob = viewModelScope.launch {
            locationsCoordinator.observeState()
                .drop(1)
                .collect { state ->
                    _locationsState.value = state
                    handleState(state)
                }
        }
        prayerPassedObserverJob = viewModelScope.launch {
            countdownEngine.prayerPassedSignal.collect { refreshPrayerState() }
        }
        dayChangedObserverJob = viewModelScope.launch {
            countdownEngine.dayChangedSignal.collect { loadPrayerTimesForCurrentLocation() }
        }
        settingsObserverJob = viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.calculationMethod }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    _screenGate.value = HomeScreenGate.Loading
                    _prayerDataByLocation.value = emptyMap()
                    loadPrayerTimesForCurrentLocation()
                }
        }
        loadInitialLocation()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> loadPrayerTimesForCurrentLocation()
            HomeEvent.OnPermissionsGranted -> loadPrayerTimesForCurrentLocation()
            HomeEvent.OnLoadQuranVerse -> loadRandomVerse()
            HomeEvent.OnVerseClicked -> setVerseSheetVisibility(isVisible = true)
            HomeEvent.OnVerseDetailDismissed -> setVerseSheetVisibility(isVisible = false)
            is HomeEvent.OnLocationSelected -> selectLocation(event.locationId)
        }
    }

    private fun loadInitialLocation() {
        viewModelScope.launch {
            val location = locationsCoordinator.resolveInitial()
            if (location != null) {
                val state = locationsCoordinator.observeState().first()
                _locationsState.value = state
                handleState(state)
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    fun loadPrayerTimesForCurrentLocation() {
        viewModelScope.launch {
            _screenGate.value = HomeScreenGate.Loading
            val location = locationsCoordinator.resolveSelected()
            if (location != null) {
                val state = locationsCoordinator.observeState().first()
                _locationsState.value = state
                val activeId = state.selectedId ?: state.entries.firstOrNull()?.id
                if (activeId != null) {
                    val (method, adjustment) = currentSettings()
                    val result = prayerTimesLoader.load(location, method, adjustment)
                    if (result.isSuccess) {
                        val loaded = result.getOrThrow()
                        stateMutex.withLock {
                            _prayerDataByLocation.value = _prayerDataByLocation.value + (activeId to loaded)
                            _activeLocationId.value = activeId
                            _screenGate.value = HomeScreenGate.Ready
                            startCountdownFor(activeId)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        _screenGate.value = HomeScreenGate.Error(
                            error?.message ?: HomeErrorMapper.getUserFriendlyErrorMessage(error)
                        )
                    }
                } else {
                    fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
                }
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            }
        }
    }

    private fun selectLocation(locationId: String) {
        viewModelScope.launch {
            locationsCoordinator.selectLocation(locationId)
        }
    }

    private suspend fun handleState(state: LocationsState) {
        val activeId = state.selectedId ?: state.entries.firstOrNull()?.id
        if (activeId == null) {
            fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
            return
        }
        val loaded = stateMutex.withLock {
            if (_activeLocationId.value != activeId) {
                _screenGate.value = HomeScreenGate.Loading
            }
            _activeLocationId.value = activeId
            val activeData = loadActiveLocation(state, activeId)
            if (activeData != null) {
                _screenGate.value = HomeScreenGate.Ready
                startCountdownFor(activeId)
                true
            } else {
                fail(HomeErrorMapper.getUserFriendlyErrorMessage(null))
                false
            }
        }
        if (loaded) {
            preloadOtherLocations(state, activeId)
        }
    }

    private suspend fun loadActiveLocation(state: LocationsState, activeId: String): LoadedPrayerData? {
        val entry = state.entries.firstOrNull { it.id == activeId } ?: return null
        val cached = _prayerDataByLocation.value[activeId]
        val locationChanged = cached?.locationState?.locationData != entry.location
        if (cached != null && !locationChanged) return cached
        val (method, adjustment) = currentSettings()
        return prayerTimesLoader.load(entry.location, method, adjustment)
            .onSuccess { loaded ->
                _prayerDataByLocation.value = _prayerDataByLocation.value + (activeId to loaded)
            }
            .onFailure { error ->
                Log.e("HomeViewModel", "Failed to load active location ${entry.id}: ${error.message}")
            }
            .getOrNull()
    }

    private fun preloadOtherLocations(state: LocationsState, activeId: String) {
        val others = state.entries.filter { it.id != activeId }
        if (others.isEmpty()) return
        viewModelScope.launch {
            coroutineScope {
                others.map { entry ->
                    async {
                        val cached = _prayerDataByLocation.value[entry.id]
                        val locationChanged = cached?.locationState?.locationData != entry.location
                        if (cached == null || locationChanged) {
                            val (method, adjustment) = currentSettings()
                            prayerTimesLoader.load(entry.location, method, adjustment)
                                .onSuccess { loaded ->
                                    stateMutex.withLock {
                                        _prayerDataByLocation.value = _prayerDataByLocation.value + (entry.id to loaded)
                                    }
                                }
                                .onFailure { error ->
                                    Log.e("HomeViewModel", "Failed to pre-load ${entry.id}: ${error.message}")
                                }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private fun refreshPrayerState() {
        viewModelScope.launch {
            stateMutex.withLock {
                val activeId = _activeLocationId.value ?: return@withLock
                val data = _prayerDataByLocation.value[activeId] ?: return@withLock
                val refreshed = prayerTimesLoader.computePrayerState(data.prayerState.prayers, data.zoneId)
                _prayerDataByLocation.value = _prayerDataByLocation.value + (activeId to data.copy(prayerState = refreshed))
                _screenGate.value = HomeScreenGate.Ready
                startCountdownFor(activeId)
            }
        }
    }

    private fun startCountdownFor(locationId: String) {
        val data = _prayerDataByLocation.value[locationId] ?: return
        countdownEngine.start(data.prayerState, data.zoneId, viewModelScope)
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

    private suspend fun currentSettings(): Pair<CalculationMethod, Int> {
        val settings = getSettingsUseCase()
        return CalculationMethod.fromSettingsId(settings.calculationMethod) to settings.hijriAdjustment
    }

    private fun fail(message: String) {
        _screenGate.value = HomeScreenGate.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        countdownEngine.stop()
        locationsObserverJob?.cancel()
        prayerPassedObserverJob?.cancel()
        dayChangedObserverJob?.cancel()
        settingsObserverJob?.cancel()
    }
}
