package com.kutluoglu.prayer_feature.settings.notifications

import android.Manifest
import android.content.Context
import android.os.PowerManager
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
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.settings.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
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
    private val notificationManager = mockk<NotificationDisplayer>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    private fun launchScreen(settings: NotificationSettings) {
        coEvery { getUseCase() } returns settings
        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        composeTestRule.setContent {
            NotificationsRoute(
                onNavigateBack = {},
                viewModel = viewModel
            )
        }
        composeTestRule.waitForIdle()
    }

    @Before
    fun grantPermissions() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, true)
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
    fun `enabling notifications via viewmodel shows exact alarm dialog`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        coEvery { getUseCase() } returns NotificationSettings(enabled = false)
        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        composeTestRule.setContent {
            NotificationsRoute(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        viewModel.onEvent(NotificationsEvent.SetEnabled(true))
        composeTestRule.waitForIdle()

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

    @Test
    fun `shows battery optimization banner when not ignoring battery optimization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        launchScreen(NotificationSettings(enabled = true))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_hint)
        ).assertIsDisplayed()
    }

    @Test
    fun `enabling notifications without battery optimization shows dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertIsDisplayed()
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `open battery settings launches ignore battery optimization settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_open_settings)
        ).performClick()
        composeTestRule.waitForIdle()

        val startedIntent = shadowOf(composeTestRule.activity).nextStartedActivity
        assertThat(startedIntent?.action).isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    @Test
    fun `not now dismisses battery optimization dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_not_now)
        ).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertDoesNotExist()
    }

    @Test
    fun `shows battery optimization dialog on entry when enabled without exemption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = true))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertIsDisplayed()
    }

    @Test
    fun `renders test adhan section in debug build`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("Test Adhan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Schedule Adhan test").assertIsDisplayed()
    }

    @Test
    fun `shows adhan-off warning when adhan is disabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = false
            )
        )

        composeTestRule
            .onNodeWithText(
                "Adhan toggle is OFF — the test will show a notification but will NOT play sound."
            )
            .assertIsDisplayed()
    }

    @Test
    fun `hides adhan-off warning when adhan is enabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule
            .onNodeWithText(
                "Adhan toggle is OFF — the test will show a notification but will NOT play sound."
            )
            .assertDoesNotExist()
    }

    @Test
    fun `scheduling a test adhan schedules an alarm with the selected delay`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("10m").performClick()
        composeTestRule.onNodeWithText("Schedule Adhan test").performClick()

        verify { alarmScheduler.scheduleTestAdhan(10) }
    }

    @Test
    fun `does not schedule a test adhan without exact alarm permission`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("Schedule Adhan test").performClick()

        verify(exactly = 0) { alarmScheduler.scheduleTestAdhan(any()) }
    }
}
