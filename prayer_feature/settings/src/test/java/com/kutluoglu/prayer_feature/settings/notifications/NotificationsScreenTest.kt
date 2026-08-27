package com.kutluoglu.prayer_feature.settings.notifications

import android.Manifest
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.settings.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

    @Before
    fun grantExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
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

    @Test
    fun `toggling on without permission requests notification permission`() {
        shadowOf(composeTestRule.activity).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        assertThat(shadowOf(composeTestRule.activity).lastRequestedPermission.requestedPermissions)
            .asList()
            .contains(Manifest.permission.POST_NOTIFICATIONS)
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `toggling adhan on without permission requests notification permission`() {
        shadowOf(composeTestRule.activity).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = true, adhanEnabled = false))

        composeTestRule.onAllNodes(isToggleable())[6].performClick()
        composeTestRule.waitForIdle()

        assertThat(shadowOf(composeTestRule.activity).lastRequestedPermission.requestedPermissions)
            .asList()
            .contains(Manifest.permission.POST_NOTIFICATIONS)
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `shows grant permission action when rationale shown and permission missing`() {
        composeTestRule.setContent {
            NotificationPermissionRationale(
                showRationale = true,
                hasPermission = false,
                permanentlyDenied = false,
                onGrantPermission = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Grant permission").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.notification_permission_rationale)
        ).assertIsDisplayed()
    }

    @Test
    fun `shows open settings action when permanently denied`() {
        composeTestRule.setContent {
            NotificationPermissionRationale(
                showRationale = true,
                hasPermission = false,
                permanentlyDenied = true,
                onGrantPermission = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Open Settings").assertIsDisplayed()
    }

    @Test
    fun `hides rationale when permission is granted`() {
        composeTestRule.setContent {
            NotificationPermissionRationale(
                showRationale = true,
                hasPermission = true,
                permanentlyDenied = false,
                onGrantPermission = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Grant permission").assertDoesNotExist()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.notification_permission_rationale)
        ).assertDoesNotExist()
    }

    @Test
    fun `shouldShowNotificationRationale returns true when rationale shown and permission missing`() {
        assertThat(shouldShowNotificationRationale(showRationale = true, hasPermission = false)).isTrue()
    }

    @Test
    fun `shouldShowNotificationRationale returns false when permission granted`() {
        assertThat(shouldShowNotificationRationale(showRationale = true, hasPermission = true)).isFalse()
    }

    @Test
    fun `shouldShowNotificationRationale returns false when no rationale`() {
        assertThat(shouldShowNotificationRationale(showRationale = false, hasPermission = false)).isFalse()
    }

    @Test
    fun `shows adhan volume slider when adhan enabled`() {
        launchScreen(
            NotificationSettings(enabled = true, adhanEnabled = true, adhanVolume = 30)
        )

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.adhan_volume)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("30%").assertIsDisplayed()
    }

    @Test
    fun `hides adhan volume slider when adhan disabled`() {
        launchScreen(NotificationSettings(enabled = true, adhanEnabled = false))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.adhan_volume)
        ).assertDoesNotExist()
    }

    @Test
    fun `adjusting adhan volume slider emits SetAdhanVolume`() {
        launchScreen(
            NotificationSettings(enabled = true, adhanEnabled = true, adhanVolume = 30)
        )

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(30f, 0f..100f)))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(50f) }
        composeTestRule.waitForIdle()

        coVerify { updateUseCase(match { it.adhanVolume == 50 }) }
    }

    @Test
    fun `enabling notifications without exact alarm permission shows dialog`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_dialog_title)
        ).assertIsDisplayed()
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `granting exact alarm permission opens settings intent`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_grant)
        ).performClick()
        composeTestRule.waitForIdle()

        val startedIntent = shadowOf(composeTestRule.activity).nextStartedActivity
        assertThat(startedIntent?.action).isEqualTo(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
    }

    @Test
    fun `not now dismisses exact alarm dialog`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_not_now)
        ).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_dialog_title)
        ).assertDoesNotExist()
    }

    @Test
    fun `shows exact alarm dialog on entry when enabled without permission`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = true))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_dialog_title)
        ).assertIsDisplayed()
    }

    @Test
    fun `granting permission on return applies pending enable action`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_grant)
        ).performClick()
        composeTestRule.waitForIdle()

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()

        coVerify { updateUseCase(match { it.enabled }) }
    }
}
