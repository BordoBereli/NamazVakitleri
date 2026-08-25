package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class UpdateNotificationSettingsUseCase(
    private val dataStore: NotificationSettingsDataStore,
    private val scheduler: PrayerNotificationScheduler,
    private val notificationManager: PrayerNotificationManager
) {
    suspend operator fun invoke(settings: NotificationSettings) {
        dataStore.updateEnabled(settings.enabled)
        dataStore.updateAdhanEnabled(settings.adhanEnabled)
        dataStore.updateAdhanVolume(settings.adhanVolume)
        dataStore.updateCountdownEnabled(settings.countdownEnabled)
        dataStore.updateDailyReminder(
            settings.dailyReminderEnabled,
            settings.dailyReminderHour,
            settings.dailyReminderMinute
        )
        dataStore.updatePrePrayerReminder(
            settings.prePrayerReminderEnabled,
            settings.prePrayerMinutes
        )
        dataStore.updateJumuahEnabled(settings.jumuahEnabled)
        dataStore.updateSpecialDaysEnabled(settings.specialDaysEnabled)
        dataStore.updateSoundEnabled(settings.soundEnabled)
        dataStore.updateVibrationEnabled(settings.vibrationEnabled)
        settings.prayerToggles.forEach { (key, enabled) ->
            dataStore.updatePrayerToggle(key, enabled)
        }
        if (settings.enabled) {
            notificationManager.createChannels(settings)
            scheduler.scheduleAll()
        } else {
            scheduler.cancelAll()
        }
    }
}
