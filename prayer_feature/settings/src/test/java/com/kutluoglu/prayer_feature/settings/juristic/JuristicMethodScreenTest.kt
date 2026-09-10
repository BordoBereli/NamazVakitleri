package com.kutluoglu.prayer_feature.settings.juristic

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer_feature.settings.R
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateJuristicMethodUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class JuristicMethodScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateJuristicMethodUseCase = mockk<UpdateJuristicMethodUseCase>(relaxed = true)

    @Before
    fun setUp() {
        coEvery { getSettingsUseCase() } returns Settings(juristicMethod = "STANDARD")
    }

    private fun launchScreen() {
        val viewModel = JuristicMethodViewModel(getSettingsUseCase, updateJuristicMethodUseCase)
        composeTestRule.setContent {
            JuristicMethodRoute(
                onNavigateBack = {},
                onMethodSelected = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders explanation for standard method`() {
        launchScreen()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.juristic_standard_description)
        ).assertIsDisplayed()
    }

    @Test
    fun `renders explanation for hanafi method`() {
        launchScreen()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.juristic_hanafi_description)
        ).assertIsDisplayed()
    }
}
