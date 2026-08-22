package com.kutluoglu.prayer_feature.settings.notifications

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
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
class NotificationsViewModelTest {

    private val getUseCase = mockk<GetNotificationSettingsUseCase>(relaxed = true)
    private val updateUseCase = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)

    @Test
    fun `loads settings on init`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings(enabled = true)

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)

        assertThat(viewModel.uiState.value).isInstanceOf(NotificationsUiState.Success::class.java)
    }

    @Test
    fun `load failure surfaces error state`() = runTest {
        coEvery { getUseCase() } throws RuntimeException("boom")

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)

        assertThat(viewModel.uiState.value).isInstanceOf(NotificationsUiState.Error::class.java)
    }

    @Test
    fun `toggling master enabled persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)
        viewModel.onEvent(NotificationsEvent.SetEnabled(true))

        coVerify { updateUseCase(match { it.enabled }) }
    }

    @Test
    fun `toggling a prayer persists the updated settings`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)
        viewModel.onEvent(NotificationsEvent.SetPrayerToggle("Fajr", false))

        coVerify { updateUseCase(match { it.prayerToggles["Fajr"] == false }) }
    }

    @Test
    fun `toggling adhan persists`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase)
        viewModel.onEvent(NotificationsEvent.SetAdhanEnabled(false))

        coVerify { updateUseCase(match { it.adhanEnabled == false }) }
    }
}
