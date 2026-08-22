package com.kutluoglu.prayer_notifications.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class NotificationUseCasesTest {

    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)

    @Test
    fun `GetNotificationSettingsUseCase returns settings`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        val useCase = GetNotificationSettingsUseCase(dataStore)
        assertThat(useCase().enabled).isTrue()
    }

    @Test
    fun `UpdateNotificationSettingsUseCase persists and reschedules`() = runTest {
        val scheduler = mockk<com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler>(relaxed = true)
        val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler)
        useCase(NotificationSettings(enabled = true))
        coVerify { dataStore.updateEnabled(true) }
        coVerify { scheduler.scheduleAll() }
    }
}
