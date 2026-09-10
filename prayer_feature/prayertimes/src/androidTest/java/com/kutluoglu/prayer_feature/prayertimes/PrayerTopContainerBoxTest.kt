package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.components.TopContainer
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test

class PrayerTopContainerBoxTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val successState = PrayerTimesUiState.Success(
        currentDayOfMonth = 1,
        selectedMonth = YearMonth(2026, 9),
        isCurrentMonth = true,
        timeState = TimeUiState(
            gregorianShortDate = "September 2026",
            hijriDate = "1 Muharram 1448",
            gregorianDayAndName = "Thursday",
            currentTime = "14:32"
        ),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    @Test
    fun boxShowsMonthHijriDayTimeAndLocation() {
        composeRule.setContent {
            Box(modifier = Modifier.width(360.dp).height(600.dp)) {
                TopContainer(
                    painter = painterResource(R.drawable.image_prayers),
                    uiState = successState
                )
            }
        }
        composeRule.waitForIdle()

        listOf("September 2026", "1 Muharram 1448", "Thursday", "14:32", "Istanbul, TR")
            .forEach { text ->
                composeRule.onNodeWithText(text).assertExists()
            }
    }

    @Test
    fun metaRowAlignsLocationLeftAndTimeRight() {
        composeRule.setContent {
            Box(modifier = Modifier.width(360.dp).height(600.dp)) {
                TopContainer(
                    painter = painterResource(R.drawable.image_prayers),
                    uiState = successState
                )
            }
        }
        composeRule.waitForIdle()

        val rootRight = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.right
        val locationLeft = composeRule.onNodeWithText("Istanbul, TR")
            .fetchSemanticsNode().boundsInRoot.left
        val timeRight = composeRule.onNodeWithText("14:32")
            .fetchSemanticsNode().boundsInRoot.right

        assertThat(locationLeft).isLessThan(rootRight / 2f)
        assertThat(timeRight).isGreaterThan(rootRight * 0.8f)
    }

    @Test
    fun contentFitsInPortraitTopContainerOnSmallScreen() {
        // Simulates the real portrait layout: TopContainer = 35% of a 640dp-tall screen;
        // the location/date box is 45% of TopContainer's height.
        composeRule.setContent {
            Box(modifier = Modifier.width(320.dp).height(224.dp)) {
                TopContainer(
                    painter = painterResource(R.drawable.image_prayers),
                    uiState = successState
                )
            }
        }
        composeRule.waitForIdle()

        val boxBounds = composeRule.onNodeWithTag("location_date_box")
            .fetchSemanticsNode().boundsInRoot
        val timeBottom = composeRule.onNodeWithText("14:32")
            .fetchSemanticsNode().boundsInRoot.bottom
        val monthTop = composeRule.onNodeWithText("September 2026")
            .fetchSemanticsNode().boundsInRoot.top

        assertThat(timeBottom).isAtMost(boxBounds.bottom)
        assertThat(monthTop).isAtLeast(boxBounds.top)
    }
}
