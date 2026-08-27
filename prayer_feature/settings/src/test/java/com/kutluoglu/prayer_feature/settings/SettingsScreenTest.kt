package com.kutluoglu.prayer_feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.core.common.AppVersion
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.usecases.prayer.ClearPrayerTimesCacheUseCase
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.ClearLocationCacheUseCase
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLocationUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val updateLocationUseCase = mockk<UpdateLocationUseCase>(relaxed = true)
    private val updateCalculationMethodUseCase = mockk<UpdateCalculationMethodUseCase>(relaxed = true)
    private val updateLanguageUseCase = mockk<UpdateLanguageUseCase>(relaxed = true)
    private val updateHijriAdjustmentUseCase = mockk<UpdateHijriAdjustmentUseCase>(relaxed = true)
    private val clearLocationCacheUseCase = mockk<ClearLocationCacheUseCase>(relaxed = true)
    private val clearPrayerTimesCacheUseCase = mockk<ClearPrayerTimesCacheUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val appVersion = AppVersion(name = "2.0.0", code = 200)

    private fun launchScreen(settings: Settings = Settings()) {
        coEvery { getSettingsUseCase() } returns settings
        val viewModel = SettingsViewModel(
            getSettingsUseCase,
            updateLocationUseCase,
            updateCalculationMethodUseCase,
            updateLanguageUseCase,
            updateHijriAdjustmentUseCase,
            clearLocationCacheUseCase,
            clearPrayerTimesCacheUseCase,
            analyticsTracker,
            appVersion
        )
        composeTestRule.setContent {
            SettingsScreen(
                onNavigateToMyLocations = {},
                onNavigateToCalculationMethod = {},
                onNavigateToHijriAdjustment = {},
                onNavigateToLanguage = {},
                onNavigateToNotifications = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders all settings items`() {
        launchScreen()

        composeTestRule.onNodeWithText("Location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculation Method").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hijri Adjustment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun `renders version footer with name and code`() {
        launchScreen()

        composeTestRule.onNodeWithText("Version 2.0.0 (200)").assertIsDisplayed()
    }
}
