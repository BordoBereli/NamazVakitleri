package com.kutluoglu.prayer_feature.home.state

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import org.junit.jupiter.api.Test

class HomeUiStateMergerTest {

    private val location = LocationUiState(
        locationData = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        ),
        locationInfoText = "Istanbul, TR"
    )
    private val time = TimeUiState(
        hijriDate = "1 Recep",
        gregorianFullDate = "12 Ağustos 2026",
        currentTime = "12:00"
    )
    private val prayer = PrayerUiState(
        prayers = emptyList(),
        currentPrayer = null,
        nextPrayer = null
    )
    private val countdown = CountdownUiState(timeRemaining = "--:--:--", currentTime = "12:00")
    private val quran = QuranUiState(verse = null, isSheetVisible = false)

    @Test
    fun `merge with Loading gate returns HomeUiState Loading`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Loading,
            location = null,
            time = null,
            prayer = null,
            countdown = countdown,
            quran = quran,
            prompt = false
        )
        assertThat(result).isEqualTo(HomeUiState.Loading)
    }

    @Test
    fun `merge with Error gate returns HomeUiState Error with message`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Error("boom"),
            location = null,
            time = null,
            prayer = null,
            countdown = countdown,
            quran = quran,
            prompt = false
        )
        assertThat(result).isEqualTo(HomeUiState.Error("boom"))
    }

    @Test
    fun `merge with Ready gate returns Success carrying all sub states`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Ready,
            location = location,
            time = time,
            prayer = prayer,
            countdown = countdown,
            quran = quran,
            prompt = true
        ) as HomeUiState.Success

        assertThat(result.locationState).isEqualTo(location)
        assertThat(result.timeState).isEqualTo(time)
        assertThat(result.prayerState).isEqualTo(prayer)
        assertThat(result.countdownState).isEqualTo(countdown)
        assertThat(result.quranVerse).isNull()
        assertThat(result.isVerseDetailSheetVisible).isFalse()
        assertThat(result.showLocationUpdatePrompt).isTrue()
    }

    @Test
    fun `merge on Ready passes the prayerState instance through un-copied`() {
        val result = mergeToHomeUiState(
            gate = HomeScreenGate.Ready,
            location = location,
            time = time,
            prayer = prayer,
            countdown = countdown,
            quran = quran,
            prompt = false
        ) as HomeUiState.Success

        assertThat(result.prayerState === prayer).isTrue()
    }

    @Test
    fun `Ready gate with null location throws`() {
        val result = runCatching {
            mergeToHomeUiState(
                gate = HomeScreenGate.Ready,
                location = null,
                time = time,
                prayer = prayer,
                countdown = countdown,
                quran = quran,
                prompt = false
            )
        }
        assertThat(result.isFailure).isTrue()
    }
}
