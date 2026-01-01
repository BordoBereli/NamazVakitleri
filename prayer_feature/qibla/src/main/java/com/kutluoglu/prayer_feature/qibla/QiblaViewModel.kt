package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.usecases.qibla.CalculateQiblaUseCase
import com.kutluoglu.prayer_location.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val qiblaState = calculateQiblaUseCase.observeSensorStateAs()
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QiblaUiState()
        )

    init {
        calculateQiblaDirection()
    }

    private fun calculateQiblaDirection() {
        viewModelScope.launch {
            try {
                val location = locationService.getCurrentLocation()
                location?.let {
                    calculateQiblaUseCase.invoke()
                    calculateQiblaUseCase.calculateQiblaBearing(
                        location.latitude, location.longitude)
                    qiblaState.collect { currQiblaState ->
                        _uiState.update {
                            it.copy(
                                qiblaAngle = currQiblaState.qiblaAngle,
                                deviceAzimuth = currQiblaState.deviceAzimuth,
                                sensorAccuracy = currQiblaState.accuracy,
                                qiblaBearing = currQiblaState.qiblaBearing,
                                isLocationAvailable = true,
                                error = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLocationAvailable = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            calculateQiblaUseCase.stopCompass()
        }
    }
}
