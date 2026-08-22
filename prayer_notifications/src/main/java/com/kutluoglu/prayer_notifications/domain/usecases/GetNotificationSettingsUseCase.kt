package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import org.koin.core.annotation.Factory

@Factory
class GetNotificationSettingsUseCase(
    private val dataStore: NotificationSettingsDataStore
) {
    suspend operator fun invoke(): NotificationSettings = dataStore.getSettings()
}
