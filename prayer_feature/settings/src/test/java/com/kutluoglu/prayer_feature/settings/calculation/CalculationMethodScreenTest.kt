package com.kutluoglu.prayer_feature.settings.calculation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class CalculationMethodScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val getSettingsUseCase: GetSettingsUseCase = mockk()
    private val updateCalculationMethodUseCase: UpdateCalculationMethodUseCase = mockk()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private fun launchScreen() {
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        coEvery { updateCalculationMethodUseCase(any()) } returns Unit
        val viewModel = CalculationMethodViewModel(getSettingsUseCase, updateCalculationMethodUseCase, analyticsTracker)
        composeTestRule.setContent {
            CalculationMethodRoute(
                onNavigateBack = {},
                onMethodSelected = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders all six supported calculation methods`() {
        launchScreen()

        composeTestRule.onNodeWithText("Türkiye (Diyanet)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Muslim World League").assertIsDisplayed()
        composeTestRule.onNodeWithText("Islamic Society of North America").assertIsDisplayed()
        composeTestRule.onNodeWithText("Egyptian General Authority").assertIsDisplayed()
        composeTestRule.onNodeWithText("Umm Al-Qura University").assertIsDisplayed()
        composeTestRule.onNodeWithText("University of Karachi").assertIsDisplayed()
        composeTestRule.onAllNodes(isSelectable()).assertCountEquals(6)

        composeTestRule.onNodeWithText("Jaafari (Imami Shiah)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Institute of Geophysics, University of Tehran").assertDoesNotExist()
    }

    @Test
    fun `selecting a method persists it and marks it selected`() {
        launchScreen()

        composeTestRule.onNodeWithText("Umm Al-Qura University").performClick()
        composeTestRule.waitForIdle()

        coVerify { updateCalculationMethodUseCase("MAKKAH") }
        composeTestRule.onNode(
            isSelectable() and hasParent(hasAnyDescendant(hasText("Umm Al-Qura University"))),
            useUnmergedTree = true
        ).assertIsSelected()
    }
}
