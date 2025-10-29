package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.home.common.PrayerFormatter
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
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadPrayerTimes()
//        observeLocationChanges()
    }

    /**
     * Handles UI events such as pull-to-refresh.
     *
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.OnRefresh -> {
                // When a refresh event is received, simply call loadPrayerTimes again.
                // This will show the loading indicator and refetch all data.
                loadPrayerTimes()
            }
            HomeEvent.OnCountDown -> {
                startPrayerCountdown()
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
                    val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                    val successState = HomeUiState.Success(
                        data = HomeDataUiState(
                            prayers = langDetectedPrayerTimes,
                            timeInfo = formatter.getInitialTimeInfo()
                        )
                    )
                    _uiState.value = successState
                }
                .onFailure { error ->
                    val errorState = HomeUiState.Error(message = error.message ?: "An unknown error occurred")
                    _uiState.value = errorState
                }
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

            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    prayers = prayersWithCurrent,
                    currentPrayer = currentPrayer,
                    nextPrayer = nextPrayer,
                    timeRemaining = timeRemainingString,
                    timeInfo = currentState.data.timeInfo.copy(
                        currentTime = formatter.getFormattedCurrentTime()
                    )
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
