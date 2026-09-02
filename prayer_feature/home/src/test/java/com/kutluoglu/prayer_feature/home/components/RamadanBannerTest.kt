package com.kutluoglu.prayer_feature.home.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer_feature.home.domain.RamadanCountdownState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RamadanBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders ramadan day and iftar countdown`() {
        composeRule.setContent {
            RamadanBanner(
                ramadanDay = 5,
                countdown = RamadanCountdownState.IftarIn(Duration.ofHours(2))
            )
        }
        composeRule.onNodeWithText("Ramadan Day 5").assertExists()
        composeRule.onNodeWithText("Iftar in 02:00:00").assertExists()
    }
}
