package com.kutluoglu.prayer_feature.settings.hijri

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h240dp")
class HijriAdjustmentScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateHijriAdjustmentUseCase = mockk<UpdateHijriAdjustmentUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    @Before
    fun setUp() {
        coEvery { getSettingsUseCase() } returns Settings(hijriAdjustment = 0)
    }

    private fun launchScreen() {
        val viewModel = HijriAdjustmentViewModel(
            getSettingsUseCase,
            updateHijriAdjustmentUseCase,
            analyticsTracker
        )
        composeTestRule.setContent {
            HijriAdjustmentRoute(
                onNavigateBack = {},
                onAdjustmentSelected = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `bottom action is reachable by scrolling on short viewport`() {
        launchScreen()

        composeTestRule.onNodeWithText("No changes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `confirm button is reachable by scrolling on short viewport`() {
        launchScreen()

        composeTestRule.onNodeWithContentDescription("Increase").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Confirm: +1 days").performScrollTo().assertIsDisplayed()
    }
}