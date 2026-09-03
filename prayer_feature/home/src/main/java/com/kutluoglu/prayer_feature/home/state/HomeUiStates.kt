package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private const val DHUHR_ARABIC_NAME = "الظهر"

/**
 * Created by F.K. on 28.10.2025.
 *
 */

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(
        val timeState: TimeUiState = TimeUiState(),
        val prayerState: PrayerUiState = PrayerUiState(),
        val locationState: LocationUiState,
        val countdownState: CountdownUiState = CountdownUiState(),

        val quranVerse: AyahData? = null,
        val isVerseDetailSheetVisible: Boolean = false,
        val isVerseSaved: Boolean = false,
        val nextImsakTime: LocalTime? = null
    ) : HomeUiState()
}

data class PrayerUiState(
        val prayers: List<Prayer> = emptyList(),
        val currentPrayer: Prayer? = null,
        val nextPrayer: Prayer? = null
) {
    fun isJumuahCountdown(): Boolean =
        nextPrayer?.let { it.arabicName == DHUHR_ARABIC_NAME && it.date.dayOfWeek == DayOfWeek.FRIDAY } ?: false
}

