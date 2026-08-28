package com.kutluoglu.prayer_feature.home.state

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test

class PrayerUiStateTest {

    private fun prayer(name: String, arabicName: String, date: LocalDate) =
        Prayer(name = name, arabicName = arabicName, time = LocalTime(12, 30), date = date)

    @Test
    fun `isJumuahCountdown true when next prayer is Dhuhr on Friday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("Öğle", "الظهر", LocalDate(2026, 8, 28)) // Friday
        )
        assertThat(state.isJumuahCountdown()).isTrue()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is Dhuhr on Monday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("Öğle", "الظهر", LocalDate(2026, 8, 24)) // Monday
        )
        assertThat(state.isJumuahCountdown()).isFalse()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is Asr on Friday`() {
        val state = PrayerUiState(
            nextPrayer = prayer("İkindi", "العصر", LocalDate(2026, 8, 28)) // Friday
        )
        assertThat(state.isJumuahCountdown()).isFalse()
    }

    @Test
    fun `isJumuahCountdown false when next prayer is null`() {
        val state = PrayerUiState(nextPrayer = null)
        assertThat(state.isJumuahCountdown()).isFalse()
    }
}
