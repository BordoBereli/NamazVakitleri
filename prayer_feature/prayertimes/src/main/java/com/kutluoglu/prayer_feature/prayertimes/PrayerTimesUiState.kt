package com.kutluoglu.prayer_feature.prayertimes

import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState

/**
 * Created by F.K. on 20.12.2025.
 *
 * Defines the UI state for the PrayerTimesScreen.
 */
/**
 * Copyright © 2025 F.K. All rights reserved.
 */
sealed class PrayerTimesUiState {
    data object Loading : PrayerTimesUiState()
    data class Error(val message: String) : PrayerTimesUiState()
    data class Success(
            val prayers: List<Prayer> = emptyList(),
            val timeState: TimeUiState,
            val locationState: LocationUiState
    ) : PrayerTimesUiState()
}