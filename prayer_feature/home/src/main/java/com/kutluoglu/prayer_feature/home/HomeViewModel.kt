package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class HomeViewModel(
//    private val prayerRepository: PrayerRepository,
//    private val locationRepository: LocationRepository,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadPrayerTimes()
//        observeLocationChanges()
    }

    fun startPrayerCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000)
            }
        }
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Using placeholder location values for now
            val latitude = 41.0082
            val longitude = 28.9784

            getPrayerTimesUseCase(LocalDateTime.now(), latitude, longitude)
                .onSuccess { prayerTimes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prayers = prayerTimes,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "An unknown error occurred"
                        )
                    }
                }
        }
    }

    /*private fun loadPrayerTimes() {
        viewModelScope.launch {
            try {
//                val location = locationRepository.getCurrentLocation()
//                val prayers = getPrayerTimesUseCase(location)
                val prayers = getPrayerTimesUseCase()

                _uiState.update { currentState ->
                    currentState.copy(
                        prayers = prayers,
                        currentPrayer = prayers.find { it.isCurrent },
                        nextPrayer = findNextPrayer(prayers),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }*/

    private suspend fun updateCountdown() {
        val nextPrayer = _uiState.value.nextPrayer ?: return
//        val timeRemaining = calculateTimeRemaining(nextPrayer.time)

//        _uiState.update { it.copy(timeRemaining = timeRemaining) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

// HomeUiState.kt - UI state data class
data class HomeUiState(
    val prayers: List<Prayer> = emptyList(),
    val currentPrayer: Prayer? = null,
    val nextPrayer: Prayer? = null,
    val timeRemaining: String = "",
    val hijriDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)