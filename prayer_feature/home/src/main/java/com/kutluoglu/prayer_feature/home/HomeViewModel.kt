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
            while (countdownJob?.isActive != false) updateCountdown()
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
                        HomeUiState.Success(
                            data = HomeDataUiState(
                                prayers = langDetectedPrayerTimes,
                                timeInfo = getTimeInfo()
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

    private fun withLocalizedNames(prayerTimes: List<Prayer>): List<Prayer> {
        val prayerNames = resProvider.getStringArray(array.prayers)
        val now = java.time.LocalTime.now(ZoneId.systemDefault())
        val langDetectedPrayerTimes = prayerTimes.mapIndexed { index, prayer ->
            val isCurrent = findCurrentPrayer(prayerTimes, now) == prayer
            prayer.copy(name = prayerNames[index], isCurrent = isCurrent)
        }
        return langDetectedPrayerTimes
    }

    private fun getTimeInfo(): TimeInfo {
        // 1. Get the current Gregorian date dynamically instead of using a hardcoded string.
        val today = LocalDate.now(ZoneId.systemDefault())
        // 2. Convert the Gregorian date to HijrahDate.
        val hijrahDate = HijrahDate.from(today)
        // 3. Format the HijrahDate object using the hijriFormatter.
        val formattedHijrihDate = hijrahDate.format(hijriFormatter)
        val formattedGregorianDate = today.format(gregorianFormatter)

        return TimeInfo(
            hijriDate = formattedHijrihDate,
            gregorianDate = formattedGregorianDate,
            currentTime = getCurrentTime()
        )
    }
    private fun getCurrentTime(): String {
        // 1. Get the current time.
        val now = java.time.LocalTime.now(ZoneId.systemDefault())

        // 2. Format and return the time string.
        return now.format(timeFormatter)
    }

    private suspend fun updateCountdown() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val now = java.time.LocalTime.now() // Using java.time.LocalTime for comparison
            val (currentPrayer, nextPrayer) =
                findCurrentAndNextPrayer(currentState.data.prayers, now)

            val timeRemainingString = if (nextPrayer != null) {
                // This now correctly returns a kotlin.time.Duration
                val duration = calculateTimeRemaining(nextPrayer.time)

                // Use the built-in formatting for kotlin.time.Duration
                duration.toKotlinDuration().toComponents { hours, minutes, seconds, _ ->
                    String.format("%02d:%02d:%02d", hours, minutes, seconds) // Format to HH:mm
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
                        currentTime = getCurrentTime() // Update currentTime here
                    )
                )
            )
            delay(1000) // The single loop runs every secon
        }
    }

    private fun findCurrentAndNextPrayer(
            prayers: List<Prayer>,
            currentTime: java.time.LocalTime
    ): Pair<Prayer?, Prayer?> {
        var currentPrayer: Prayer? = null
        var nextPrayer: Prayer? = null

        // Find the last prayer that has already passed
        currentPrayer = findCurrentPrayer(prayers, currentTime)

        // The next prayer is the one immediately after the current one in the list
        if (currentPrayer != null) {
            val currentIndex = prayers.indexOf(currentPrayer)
            nextPrayer = prayers.getOrNull(currentIndex + 1)
        }

        // Special case: If no prayer has passed today (e.g., before Fajr),
        // current is Isha of yesterday (conceptually), and next is Fajr.
        if (currentPrayer == null) {
            nextPrayer = prayers.firstOrNull()
        }
        // Special case: If the current prayer is Isha, the next prayer is Fajr of the next day.
        else if (nextPrayer == null) {
            nextPrayer = prayers.firstOrNull()
        }


        return Pair(currentPrayer, nextPrayer)
    }

    private fun findCurrentPrayer(
            prayers: List<Prayer>,
            currentTime: java.time.LocalTime
    ): Prayer? = prayers.lastOrNull { prayer ->
        !prayer.time.toJavaLocalTime().isAfter(currentTime)
    }

    private fun calculateTimeRemaining(nextPrayerTime: LocalTime): Duration {
        val now = LocalDateTime.now()
        var duration = Duration.between(
            now.time.toJavaLocalTime(),
            nextPrayerTime.toJavaLocalTime()
        )

        // The rest of your logic remains the same, but is much cleaner
        if (duration.isNegative()) {
            duration = duration.plus(Duration.parse("24h"))
        }
        return duration
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
    val timeInfo: TimeInfo = TimeInfo()
)

data class TimeInfo(
        val hijriDate: String = "",
        val gregorianDate: String = "",
        val currentTime: String = ""
)