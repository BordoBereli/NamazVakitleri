package com.kutluoglu.prayer_feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.gregorianFormatter
import com.kutluoglu.core.common.hijriFormatter
import com.kutluoglu.core.common.now
import com.kutluoglu.core.common.timeFormatter
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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import org.koin.android.annotation.KoinViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.Duration
import kotlin.time.toKotlinDuration

@KoinViewModel
class HomeViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val resProvider: StringResourcesProvider,
        private val calculator: TimeAndPrayerCalculator
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadPrayerTimes()
//        observeLocationChanges()
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
                    _uiState.value = HomeUiState.Success(
                        data = HomeDataUiState(
                            prayers = langDetectedPrayerTimes,
                            timeInfo = calculator.getInitialTimeInfo()
                        )
                    )
                    startPrayerCountdown()
                }
                .onFailure { error ->
                    _uiState.value =
                        HomeUiState.Error(message = error.message ?: "An unknown error occurred")
                }
        }
    }
    fun startPrayerCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (countdownJob?.isActive != false) updateCountdown()
        }
    }
    private suspend fun updateCountdown() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val now = java.time.LocalTime.now() // Using java.time.LocalTime for comparison
            val (currentPrayer, nextPrayer) =
                calculator.findCurrentAndNextPrayer(
                    prayers = currentState.data.prayers,
                    currentTime = now
                )

            val timeRemainingString = if (nextPrayer != null) {
                val duration = calculator.calculateTimeRemaining(nextPrayer.time)
                duration.toKotlinDuration().toComponents { hours, minutes, seconds, _ ->
                    calculator.formatTimeRemaining(duration)
                }
            } else {
                "--:--" // Default or end state
            }

            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    currentPrayer = currentPrayer,
                    nextPrayer = nextPrayer,
                    timeRemaining = timeRemainingString,
                    timeInfo = currentState.data.timeInfo.copy(
                        currentTime = calculator.getCurrentTime()
                    )
                )
            )
            delay(1000)
        }
    }
    private fun withLocalizedNames(prayerTimes: List<Prayer>): List<Prayer> {
        val prayerNames = resProvider.getStringArray(array.prayers)
        val now = java.time.LocalTime.now(ZoneId.systemDefault())
        val langDetectedPrayerTimes = prayerTimes.mapIndexed { index, prayer ->
            val isCurrent = calculator.findCurrentPrayer(prayerTimes, now) == prayer
            prayer.copy(name = prayerNames[index], isCurrent = isCurrent)
        }
        return langDetectedPrayerTimes
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
