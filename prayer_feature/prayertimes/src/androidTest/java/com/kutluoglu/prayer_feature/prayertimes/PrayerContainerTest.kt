package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.gregorianShortFormatter
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerTimesTestTags
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrayerContainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val prayers = listOf(
        Prayer(name = "Imsak", arabicName = "الإمساك", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1), isImsak = true),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    private val dailyPrayers = (1..31).map { day ->
        DailyPrayer(
            dayOfMonth = day,
            gregorianDate = "$day August",
            hijriDate = "$day Muharram 1448",
            prayers = prayers
        )
    }

    private val successState = PrayerTimesUiState.Success(
        monthlyPrayers = dailyPrayers,
        currentDayOfMonth = 17,
        selectedMonth = YearMonth(2026, 8),
        isCurrentMonth = true,
        timeState = TimeUiState(gregorianShortDate = "August 2026"),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    // The month label is rendered via gregorianShortFormatter ("MMMM yyyy") with the device
    // locale, so compute the expected label the same way to keep the assertion locale-independent.
    private val expectedMonthLabel: String =
        java.time.YearMonth.of(2026, 8).format(gregorianShortFormatter())

    @Test
    fun successStateRendersMonthLabelAndAllPrayerNamesInHeader() {
        composeRule.setContent { PrayerContainer(successState) {} }
        composeRule.onNodeWithText(expectedMonthLabel).assertIsDisplayed()
        prayers.forEach { prayer ->
            composeRule.onNodeWithText(prayer.name).assertIsDisplayed()
        }
    }

    @Test
    fun successStateRendersAllPrayerTimesForTheFirstDay() {
        composeRule.setContent { PrayerContainer(successState) {} }
        prayers.forEach { prayer ->
            assertAnyNodeWithTextDisplayed(prayer.time.toString())
        }
    }

    // The monthly list auto-scrolls to the current day, so the same prayer time appears in
    // several composed cards, some of which are off-screen. Assert that at least one visible
    // node renders the given text.
    private fun assertAnyNodeWithTextDisplayed(text: String) {
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val anyDisplayed = composeRule.onAllNodesWithText(text)
            .fetchSemanticsNodes()
            .any { node ->
                val bounds = node.boundsInRoot
                bounds.left < rootBounds.right &&
                    bounds.right > rootBounds.left &&
                    bounds.top < rootBounds.bottom &&
                    bounds.bottom > rootBounds.top
            }
        assertTrue("Expected at least one visible node with text '$text'", anyDisplayed)
    }

    @Test
    fun errorStateRendersTheErrorMessage() {
        composeRule.setContent { PrayerContainer(PrayerTimesUiState.Error("boom")) {} }
        composeRule.onNodeWithText("boom").assertIsDisplayed()
    }

    @Test
    fun clickingNextMonthEmitsOnNextMonth() {
        val events = mutableListOf<PrayerTimesEvent>()
        composeRule.setContent { PrayerContainer(successState) { events.add(it) } }
        composeRule.onNodeWithTag(PrayerTimesTestTags.NextMonth).performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnNextMonth)
    }

    @Test
    fun clickingPreviousMonthEmitsOnPreviousMonth() {
        val events = mutableListOf<PrayerTimesEvent>()
        composeRule.setContent { PrayerContainer(successState) { events.add(it) } }
        composeRule.onNodeWithTag(PrayerTimesTestTags.PreviousMonth).performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnPreviousMonth)
    }

    @Test
    fun nonCurrentMonthShowsTodayButtonThatEmitsOnToday() {
        val events = mutableListOf<PrayerTimesEvent>()
        val otherMonth = successState.copy(isCurrentMonth = false)
        composeRule.setContent { PrayerContainer(otherMonth) { events.add(it) } }
        composeRule.onNodeWithTag(PrayerTimesTestTags.Today).performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.OnToday)
    }
}
