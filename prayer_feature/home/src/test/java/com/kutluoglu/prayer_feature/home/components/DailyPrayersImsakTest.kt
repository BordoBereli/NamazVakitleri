package com.kutluoglu.prayer_feature.home.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyPrayersImsakTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders imsak card with imsak time`() {
        val imsak = Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 9, 2), isImsak = true)
        val sunrise = Prayer("Sunrise", "الشروق", LocalTime(6, 0), LocalDate(2026, 9, 2))
        composeRule.setContent {
            DailyPrayers(
                prayerState = PrayerUiState(prayers = listOf(imsak, sunrise)),
                isRefreshing = false,
                onRefresh = {},
                onViewAllClicked = {}
            )
        }
        composeRule.onNodeWithText("04:50").assertExists()
    }
}
