package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test

class PrayerContainerImsakTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun monthlyListShowsImsakColumnHeader() {
        val imsak = Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 9, 2), isImsak = true)
        val fajr = Prayer("Fajr", "الفجر", LocalTime(5, 0), LocalDate(2026, 9, 2))
        val daily = DailyPrayer(2, "2026-09-02", "20 Safer 1448", listOf(imsak, fajr))
        rule.setContent {
            PrayerContainer(
                uiState = PrayerTimesUiState.Success(
                    monthlyPrayers = listOf(daily),
                    currentDayOfMonth = 2,
                    selectedMonth = YearMonth(2026, 9),
                    isCurrentMonth = true,
                    timeState = TimeUiState(gregorianShortDate = "September 2026"),
                    locationState = LocationUiState(
                        locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
                        locationInfoText = "Istanbul, TR"
                    )
                ),
                onEvent = {}
            )
        }
        rule.onNodeWithText("Imsak").assertExists()
    }
}
