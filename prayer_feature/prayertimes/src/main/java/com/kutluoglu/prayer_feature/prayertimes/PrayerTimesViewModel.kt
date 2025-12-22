package com.kutluoglu.prayer_feature.prayertimes

/**
 * Created by F.K. on 20.12.2025.
 *
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer_feature.common.PrayerFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getSavedLocationUseCase: GetSavedLocationUseCase,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerTimesUiState>(PrayerTimesUiState.Loading)
    val uiState: StateFlow<PrayerTimesUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = PrayerTimesUiState.Loading
            getSavedLocationUseCase()
                .onSuccess { savedLocation ->
                    val zoneId = getZoneIdFromLocation(savedLocation.countryCode)
                    val locationDateTime = LocalDateTime.now(zoneId)

                    getPrayerTimesUseCase(
                        date = locationDateTime,
                        latitude = savedLocation.latitude,
                        longitude = savedLocation.longitude,
                        zoneId = zoneId
                    ).onSuccess { prayerTimes ->
                        val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
                        val (currentPrayer, _) = calculator.findCurrentAndNextPrayer(
                            langDetectedPrayerTimes
                        )

                        val prayersWithCurrent = langDetectedPrayerTimes.map {
                            it.copy(isCurrent = it.name == currentPrayer?.name)
                        }

                        _uiState.value = PrayerTimesUiState.Success(
                            prayers = prayersWithCurrent,
                            gregorianDate = formatter.getInitialTimeInfo(zoneId).gregorianDate
                        )
                    }.onFailure {
                        _uiState.value =
                            PrayerTimesUiState.Error(it.message ?: "Failed to load prayer times.")
                    }
                }.onFailure {
                    _uiState.value =
                        PrayerTimesUiState.Error(it.message ?: "Failed to get saved location.")
                }
        }
    }
}