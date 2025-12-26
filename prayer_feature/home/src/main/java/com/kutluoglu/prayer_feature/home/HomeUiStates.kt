package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState

/**
 * Created by F.K. on 28.10.2025.
 *
 */

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(
        val timeState: TimeUiState = TimeUiState(),
        val prayerState: PrayerUiState = PrayerUiState(),
        val locationState: LocationUiState,

        val quranVerse: AyahData? = null,
        val isVerseDetailSheetVisible: Boolean = false,
        val showLocationUpdatePrompt: Boolean = false
    ) : HomeUiState()
}

data class PrayerUiState(
        val prayers: List<Prayer> = emptyList(),
        val currentPrayer: Prayer? = null,
        val nextPrayer: Prayer? = null,
        val timeRemaining: String = "--:--:--"
)

