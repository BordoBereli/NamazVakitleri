package com.kutluoglu.prayer_feature.settings.notifications

import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class NotificationsViewModelAdhanStyleTest {

    private val getSettings = mockk<GetNotificationSettingsUseCase>(relaxed = true)
    private val updateSettings = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)
    private val notificationDisplayer = mockk<NotificationDisplayer>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    @Test
    fun `SetAdhanStyle persists style for prayer`() = runTest {
        coEvery { getSettings() } returns NotificationSettings()
        val vm = NotificationsViewModel(getSettings, updateSettings, notificationDisplayer, alarmScheduler)
        vm.onEvent(NotificationsEvent.SetAdhanStyle("Dhuhr", "makkah"))
        coVerify { updateSettings(match { it.adhanStyles["Dhuhr"] == "makkah" }) }
    }
}
