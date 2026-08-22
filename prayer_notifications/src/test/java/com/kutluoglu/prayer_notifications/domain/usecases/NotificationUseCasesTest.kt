package com.kutluoglu.prayer_notifications.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
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
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler)
        useCase(NotificationSettings(enabled = true))
        coVerify { dataStore.updateEnabled(true) }
        coVerify { dataStore.updateAdhanEnabled(true) }
        coVerify { dataStore.updateCountdownEnabled(true) }
        coVerify { dataStore.updateJumuahEnabled(true) }
        coVerify { dataStore.updateSpecialDaysEnabled(true) }
        coVerify { dataStore.updateSoundEnabled(true) }
        coVerify { dataStore.updateVibrationEnabled(true) }
        coVerify { scheduler.scheduleAll() }
    }

    @Test
    fun `UpdateNotificationSettingsUseCase persists prayer toggles`() = runTest {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler)
        useCase(
            NotificationSettings(
                enabled = true,
                prayerToggles = mapOf("Fajr" to false, "Dhuhr" to true)
            )
        )
        coVerify { dataStore.updatePrayerToggle("Fajr", false) }
        coVerify { dataStore.updatePrayerToggle("Dhuhr", true) }
    }

    @Test
    fun `UpdateNotificationSettingsUseCase cancels when disabled`() = runTest {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler)
        useCase(NotificationSettings(enabled = false))
        verify { scheduler.cancelAll() }
        verify(exactly = 0) { scheduler.scheduleAll() }
    }

    @Test
    fun `ScheduleNotificationsUseCase schedules all`() = runTest {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val useCase = ScheduleNotificationsUseCase(scheduler)
        useCase()
        verify { scheduler.scheduleAll() }
    }

    @Test
    fun `CancelNotificationsUseCase cancels all`() = runTest {
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val useCase = CancelNotificationsUseCase(scheduler)
        useCase()
        verify { scheduler.cancelAll() }
    }
}
