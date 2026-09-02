package com.kutluoglu.prayer_feature.settings.notifications

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class NotificationsViewModelTest {

    private val getUseCase = mockk<GetNotificationSettingsUseCase>(relaxed = true)
    private val updateUseCase = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)
    private val notificationManager = mockk<NotificationDisplayer>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    @Test
    fun `loads settings on init`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings(enabled = true)

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)

        assertThat(viewModel.uiState.value).isInstanceOf(NotificationsUiState.Success::class.java)
    }

    @Test
    fun `load failure surfaces error state`() = runTest {
        coEvery { getUseCase() } throws RuntimeException("boom")

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)

        assertThat(viewModel.uiState.value).isInstanceOf(NotificationsUiState.Error::class.java)
    }

    @Test
    fun `toggling master enabled persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetEnabled(true))

        coVerify { updateUseCase(match { it.enabled }) }
    }

    @Test
    fun `toggling a prayer persists the updated settings`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetPrayerToggle("Dhuhr", false))

        coVerify { updateUseCase(match { it.prayerToggles["Dhuhr"] == false }) }
    }

    @Test
    fun `toggling adhan persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetAdhanEnabled(false))

        coVerify { updateUseCase(match { it.adhanEnabled == false }) }
    }

    @Test
    fun `setting adhan volume persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetAdhanVolume(50))

        coVerify { updateUseCase(match { it.adhanVolume == 50 }) }
    }

    @Test
    fun `setting pre-prayer reminder persists minutes`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetPrePrayerReminder(true, 30))

        coVerify {
            updateUseCase(match { it.prePrayerReminderEnabled && it.prePrayerMinutes == 30 })
        }
    }

    @Test
    fun `setting daily reminder persists time`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SetDailyReminder(true, 7, 30))

        coVerify {
            updateUseCase(
                match {
                    it.dailyReminderEnabled &&
                        it.dailyReminderHour == 7 &&
                        it.dailyReminderMinute == 30
                }
            )
        }
    }

    @Test
    fun `send test notification creates channels and shows test notification`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings(enabled = true)

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.SendTest)

        verify { notificationManager.createChannels(any()) }
        verify { notificationManager.showTestNotification() }
    }

    @Test
    fun `scheduling a test adhan invokes the alarm scheduler`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.ScheduleTestAdhan(5))

        verify { alarmScheduler.scheduleTestAdhan(5) }
    }
}
