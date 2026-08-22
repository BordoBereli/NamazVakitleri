package com.kutluoglu.prayer_feature.settings.notifications

import com.kutluoglu.prayer_notifications.domain.NotificationSettings

sealed class NotificationsUiState {
    data object Loading : NotificationsUiState()
    data class Success(val settings: NotificationSettings) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

sealed class NotificationsEvent {
    data object Load : NotificationsEvent()
    data class SetEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetPrayerToggle(val prayerKey: String, val enabled: Boolean) : NotificationsEvent()
    data class SetAdhanEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetCountdownEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetPrePrayerReminder(val enabled: Boolean, val minutes: Int) : NotificationsEvent()
    data class SetDailyReminder(val enabled: Boolean, val hour: Int, val minute: Int) : NotificationsEvent()
    data class SetJumuahEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetSpecialDaysEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetSoundEnabled(val enabled: Boolean) : NotificationsEvent()
    data class SetVibrationEnabled(val enabled: Boolean) : NotificationsEvent()
    data object SendTest : NotificationsEvent()
}
