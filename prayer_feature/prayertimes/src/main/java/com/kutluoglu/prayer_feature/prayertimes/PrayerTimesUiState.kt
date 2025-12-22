package com.kutluoglu.prayer_feature.prayertimes

import com.kutluoglu.prayer.model.prayer.Prayer

/**
 * Created by F.K. on 20.12.2025.
 * Copyright © 2025 F.K. All rights reserved.
 *
 * Defines the UI state for the PrayerTimesScreen.
 */
sealed class PrayerTimesUiState {
    data object Loading : PrayerTimesUiState()
    data class Error(val message: String) : PrayerTimesUiState()
    data class Success(
            val prayers: List<Prayer> = emptyList(),
            val gregorianDate: String = ""
    ) : PrayerTimesUiState()
}