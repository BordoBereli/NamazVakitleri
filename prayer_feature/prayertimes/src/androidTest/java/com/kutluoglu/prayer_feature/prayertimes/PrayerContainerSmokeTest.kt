package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import org.junit.Rule
import org.junit.Test

class PrayerContainerSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateRendersTheLoadingIndicator() {
        composeRule.setContent {
            PrayerContainer(PrayerTimesUiState.Loading) {}
        }
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }
}
