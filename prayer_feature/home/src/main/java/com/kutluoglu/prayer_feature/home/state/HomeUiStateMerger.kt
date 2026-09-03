package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import kotlinx.datetime.LocalTime

/**
 * Maps low-level failures to user-facing messages.
 * Same messages as the previous HomeViewModel implementation.
 */
object HomeErrorMapper {
    fun getUserFriendlyErrorMessage(exception: Throwable?): String {
        return when {
            exception == null -> "Konum alınamadı. Lütfen GPS'i etkinleştirin ve uygulamayı yeniden başlatın."
            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "İstek zaman aşımına uğradı. Lütfen tekrar deneyin."
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Ağ hatası. Lütfen bağlantınızı kontrol edin."
            exception.message?.contains("location", ignoreCase = true) == true ->
                "Konum servisi kullanılamıyor. Lütfen GPS'i etkinleştirin."
            else -> "Konum alınamadı. Lütfen tekrar deneyin."
        }
    }
}

/**
 * Pure aggregation of the per-concern flows into the single HomeUiState the screen consumes.
 * On Ready the prayerState/timeState references are reused (NOT copied) so that a
 * per-second countdown tick does not invalidate DailyPrayers' inputs.
 */
fun mergeToHomeUiState(
    gate: HomeScreenGate,
    location: LocationUiState?,
    time: TimeUiState?,
    prayer: PrayerUiState?,
    countdown: CountdownUiState,
    quran: QuranUiState,
    nextImsakTime: LocalTime? = null
): HomeUiState {
    return when (gate) {
        HomeScreenGate.Loading -> HomeUiState.Loading
        HomeScreenGate.Empty -> HomeUiState.Empty
        is HomeScreenGate.Error -> HomeUiState.Error(gate.message)
        HomeScreenGate.Ready -> {
            if (time == null || prayer == null || location == null) {
                HomeUiState.Loading
            } else {
                HomeUiState.Success(
                    timeState = time,
                    prayerState = prayer,
                    locationState = location,
                    countdownState = countdown,
                    quranVerse = quran.verse,
                    isVerseDetailSheetVisible = quran.isSheetVisible,
                    isVerseSaved = quran.isSaved,
                    nextImsakTime = nextImsakTime
                )
            }
        }
    }
}
