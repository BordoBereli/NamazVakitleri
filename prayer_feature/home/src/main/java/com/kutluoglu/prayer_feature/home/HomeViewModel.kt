package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.now
import com.kutluoglu.core.ui.R.*
import com.kutluoglu.core.ui.theme.StringResourcesProvider
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.android.annotation.KoinViewModel
import kotlin.collections.List

@KoinViewModel
class HomeViewModel(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val resProvider: StringResourcesProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
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
            _uiState.value = HomeUiState.Loading

            // Using placeholder location values for now 41.03145023904377, 28.80314290541189
            val latitude = 41.03145023904377 // 41.0082
            val longitude = 28.80314290541189 //28.9784

            getPrayerTimesUseCase(LocalDateTime.now(), latitude, longitude)
                .onSuccess { prayerTimes ->
                    val langDetectedPrayerTimes = withLocalizedNames(prayerTimes)

                    _uiState.value =
                        HomeUiState.Success(data = HomeDataUiState(prayers = langDetectedPrayerTimes))
                }
                .onFailure { error ->
                    _uiState.value =
                        HomeUiState.Error(message = error.message ?: "An unknown error occurred")
                }
        }
    }

    private fun withLocalizedNames(prayerTimes: List<Prayer>): List<Prayer> {
        val prayerNames = resProvider.getStringArray(array.prayers)
        val langDetectedPrayerTimes = prayerTimes.mapIndexed { index, prayer ->
            prayer.copy(name = prayerNames[index], isCurrent = index == 4)
        }
        return langDetectedPrayerTimes
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
//        val nextPrayer = _uiState.value.nextPrayer ?: return
//        val timeRemaining = calculateTimeRemaining(nextPrayer.time)

//        _uiState.update { it.copy(timeRemaining = timeRemaining) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

sealed class HomeUiState {
    data object Loading: HomeUiState()
    data class Error(val message: String): HomeUiState()
    data class Success(val data: HomeDataUiState): HomeUiState()
}
// HomeUiState.kt - UI state data class
data class HomeDataUiState(
    val prayers: List<Prayer> = emptyList(),
    val currentPrayer: Prayer? = null,
    val nextPrayer: Prayer? = null,
    val timeRemaining: String = "",
    val hijriDate: String = ""
)