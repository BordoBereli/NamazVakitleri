package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.analytics.AnalyticsEvents
import com.kutluoglu.core.common.analytics.AnalyticsParams
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.usecases.qibla.CalculateQiblaUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.ObserveSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCompassAutoRotateUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLockPortraitUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class QiblaUiState(
    val qiblaBearing: Double = 0.0,
    val deviceAzimuth: Float = 0.0f,
    val qiblaAngle: Float = 0.0f,
    val isLocationAvailable: Boolean = false,
    val error: String? = null,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val locationName: String? = null,
    val lockPortrait: Boolean = true,
    val compassAutoRotate: Boolean = true
)

@OptIn(FlowPreview::class)
@KoinViewModel
class QiblaViewModel(
    private val activeLocationProvider: ActiveLocationProvider,
    private val calculateQiblaUseCase: CalculateQiblaUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val formatter: PrayerFormatter,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateLockPortraitUseCase: UpdateLockPortraitUseCase,
    private val updateCompassAutoRotateUseCase: UpdateCompassAutoRotateUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QiblaUiState()
        )

    private var observationJob: Job? = null
    private var settingsJob: Job? = null
    private var hasLoggedAlignment = false

    fun onEvent(event: QiblaEvent) {
        when (event) {
            QiblaEvent.OnStart -> {
                analyticsTracker.logEvent(AnalyticsEvents.QIBLA_COMPASS_STARTED)
                hasLoggedAlignment = false
                loadQiblaPreferences()
                startQiblaObservation()
            }
            QiblaEvent.OnStop -> {
                analyticsTracker.logEvent(AnalyticsEvents.QIBLA_COMPASS_STOPPED)
                stopQiblaObservation()
                stopSettingsObservation()
            }
            QiblaEvent.ToggleLockPortrait -> toggleLockPortrait()
            QiblaEvent.ToggleCompassAutoRotate -> toggleCompassAutoRotate()
        }
    }

    private fun loadQiblaPreferences() {
        if (settingsJob?.isActive == true) return
        settingsJob = viewModelScope.launch {
            try {
                val initial = getSettingsUseCase()
                _uiState.update {
                    it.copy(
                        lockPortrait = initial.lockPortrait,
                        compassAutoRotate = initial.compassAutoRotate
                    )
                }
                observeSettingsUseCase()
                    .map { s -> s.lockPortrait to s.compassAutoRotate }
                    .distinctUntilChanged()
                    .collectLatest { (lock, compass) ->
                        _uiState.update {
                            it.copy(lockPortrait = lock, compassAutoRotate = compass)
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i("QiblaViewModel", "Settings observation cancelled.")
                } else {
                    Log.e("QiblaViewModel", "Error observing settings", e)
                }
            }
        }
    }

    private fun stopSettingsObservation() {
        settingsJob?.cancel()
        settingsJob = null
    }

    private fun startQiblaObservation() {
        if (observationJob?.isActive == true) return
        observationJob = viewModelScope.launch {
            try {
                activeLocationProvider.location
                    .flatMapLatest { location ->
                        if (location != null) {
                            calculateQiblaUseCase.observeQiblaDirection(location.latitude, location.longitude)
                                .map { qiblaState -> qiblaState to location }
                        } else {
                            flowOf(null to null)
                        }
                    }
                    .collectLatest { (currQiblaState, location) ->
                        if (currQiblaState != null && location != null) {
                            _uiState.update {
                                it.copy(
                                    qiblaAngle = currQiblaState.qiblaAngle,
                                    deviceAzimuth = currQiblaState.deviceAzimuth,
                                    sensorAccuracy = currQiblaState.sensorAccuracy,
                                    qiblaBearing = currQiblaState.qiblaBearing,
                                    isLocationAvailable = true,
                                    error = null,
                                    locationName = formatter.locationInfo(location)
                                )
                            }
                            trackAlignment(currQiblaState.qiblaAngle)
                        } else {
                            _uiState.update { it.copy(isLocationAvailable = false, error = "Location not found") }
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i("QiblaViewModel", "Observation Job was cancelled as expected.")
                } else {
                    Log.e("QiblaViewModel", "Error observing Qibla state", e)
                    _uiState.update { it.copy(error = e.message, isLocationAvailable = false) }
                }
            }
        }
    }

    private fun stopQiblaObservation() {
        observationJob?.cancel()
        observationJob = null
        calculateQiblaUseCase.stop()
    }

    private fun toggleLockPortrait() {
        val newValue = !_uiState.value.lockPortrait
        viewModelScope.launch {
            updateLockPortraitUseCase(newValue)
        }
    }

    private fun toggleCompassAutoRotate() {
        val newValue = !_uiState.value.compassAutoRotate
        viewModelScope.launch {
            updateCompassAutoRotateUseCase(newValue)
        }
    }

    private fun trackAlignment(qiblaAngle: Float) {
        val aligned = kotlin.math.abs(qiblaAngle) <= ALIGNMENT_THRESHOLD_DEGREES
        if (aligned && !hasLoggedAlignment) {
            hasLoggedAlignment = true
            analyticsTracker.logEvent(
                AnalyticsEvents.QIBLA_ALIGNED,
                mapOf(AnalyticsParams.DEGREES_OFF to qiblaAngle)
            )
        } else if (!aligned) {
            hasLoggedAlignment = false
        }
    }

    private companion object {
        const val ALIGNMENT_THRESHOLD_DEGREES = 5f
    }

    override fun onCleared() {
        super.onCleared()
        stopQiblaObservation()
        stopSettingsObservation()
    }
}
