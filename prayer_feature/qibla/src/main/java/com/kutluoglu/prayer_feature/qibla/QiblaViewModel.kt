package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import android.util.Log
import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.usecases.qibla.CalculateQiblaUseCase
import com.kutluoglu.prayer_location.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import kotlin.Double

data class QiblaUiState(
    val qiblaBearing: Double = 0.0, // Kıble'nin Kuzey'e göre açısı
    val deviceAzimuth: Float = 0.0f,  // Cihazın Kuzey'e göre açısı
    val qiblaAngle: Float = 0.0f,     // Cihaz ve Kıble arasındaki son açı
    val isLocationAvailable: Boolean = false,
    val error: String? = null,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
)

@KoinViewModel
class QiblaViewModel(
    private val locationService: LocationService,
    private val calculateQiblaUseCase: CalculateQiblaUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QiblaUiState()
        )

    private var observationJob: Job? = null

    fun onEvent(event: QiblaEvent) {
        when (event) {
            QiblaEvent.OnStart -> startQiblaObservation()
            QiblaEvent.OnStop -> stopQiblaObservation()
        }
    }

    private fun startQiblaObservation() {
        if (observationJob?.isActive == true) return

        observationJob = viewModelScope.launch {
            try {
                val location = locationService.getCurrentLocation()
                location?.let {
                    calculateQiblaUseCase.observeQiblaDirection(it.latitude, it.longitude)
                        .collectLatest { currQiblaState ->
                            _uiState.update {
                                it.copy(
                                    qiblaAngle = currQiblaState.qiblaAngle,
                                    deviceAzimuth = currQiblaState.deviceAzimuth,
                                    sensorAccuracy = currQiblaState.sensorAccuracy,
                                    qiblaBearing = currQiblaState.qiblaBearing,
                                    isLocationAvailable = true,
                                    error = null
                                )
                            }
                        }
                } ?: throw Exception("Location not found")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Job iptal edildiğinde bu bir hata değildir, log'a yazıp geçebiliriz.
                    Log.i("QiblaViewModel", "Observation Job was cancelled as expected.")
                } else {
                    // Diğer hataları log'layıp UI'a bildirelim.
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


    override fun onCleared() {
        super.onCleared()
        stopQiblaObservation()
    }
}
