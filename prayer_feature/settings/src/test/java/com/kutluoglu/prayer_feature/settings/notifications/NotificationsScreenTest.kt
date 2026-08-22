package com.kutluoglu.prayer_feature.settings.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
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
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val getUseCase = mockk<GetNotificationSettingsUseCase>(relaxed = true)
    private val updateUseCase = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)
    private val notificationManager = mockk<PrayerNotificationManager>(relaxed = true)

    private fun launchScreen(settings: NotificationSettings) {
        coEvery { getUseCase() } returns settings
        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager)
        composeTestRule.setContent {
            NotificationsRoute(
                onNavigateBack = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders pre-prayer minutes chips when reminder enabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                prePrayerReminderEnabled = true,
                dailyReminderEnabled = true
            )
        )

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("60").assertIsDisplayed()
    }

    @Test
    fun `renders daily reminder time when enabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                prePrayerReminderEnabled = true,
                dailyReminderEnabled = true,
                dailyReminderHour = 7,
                dailyReminderMinute = 30
            )
        )

        composeTestRule.onNodeWithText("07:30").assertIsDisplayed()
    }

    @Test
    fun `clicking a pre-prayer minutes chip persists the selection`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                prePrayerReminderEnabled = true,
                dailyReminderEnabled = true
            )
        )

        composeTestRule.onNodeWithText("30").performClick()
        composeTestRule.waitForIdle()

        coVerify { updateUseCase(match { it.prePrayerMinutes == 30 }) }
    }
}
