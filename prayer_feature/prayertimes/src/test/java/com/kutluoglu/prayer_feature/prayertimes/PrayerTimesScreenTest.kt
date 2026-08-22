package com.kutluoglu.prayer_feature.prayertimes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerTimesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedMonth = YearMonth(2026, 8)

    private val prayers = listOf(
        Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    private val successState = PrayerTimesUiState.Success(
        monthlyPrayers = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = prayers
            )
        ),
        currentDayOfMonth = 1,
        selectedMonth = selectedMonth,
        isCurrentMonth = true,
        timeState = TimeUiState(gregorianShortDate = "August 2026"),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    @Test
    fun `renders prayer names and month header for success state`() {
        composeTestRule.setContent {
            PayerTimesScreen(
                uiState = successState,
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("month_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fajr").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeTestRule.onNodeWithText("Isha").assertIsDisplayed()
    }
}
