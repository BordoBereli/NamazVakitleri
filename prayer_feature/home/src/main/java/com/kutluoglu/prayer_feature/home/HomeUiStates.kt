package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.Prayer

/**
 * Created by F.K. on 28.10.2025.
 *
 */

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(val data: HomeDataUiState) : HomeUiState()
}

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

