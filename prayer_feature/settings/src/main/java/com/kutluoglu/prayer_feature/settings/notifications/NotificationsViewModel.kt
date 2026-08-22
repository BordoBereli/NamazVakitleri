package com.kutluoglu.prayer_feature.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.usecases.GetNotificationSettingsUseCase
import com.kutluoglu.prayer_notifications.domain.usecases.UpdateNotificationSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NotificationsViewModel(
    private val getSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateSettingsUseCase: UpdateNotificationSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: NotificationsEvent) {
        when (event) {
            NotificationsEvent.Load -> load()
            is NotificationsEvent.SetEnabled -> update { it.copy(enabled = event.enabled) }
            is NotificationsEvent.SetPrayerToggle -> update {
                it.copy(prayerToggles = it.prayerToggles + (event.prayerKey to event.enabled))
            }
            is NotificationsEvent.SetAdhanEnabled -> update { it.copy(adhanEnabled = event.enabled) }
            is NotificationsEvent.SetCountdownEnabled -> update { it.copy(countdownEnabled = event.enabled) }
            is NotificationsEvent.SetPrePrayerReminder -> update {
                it.copy(prePrayerReminderEnabled = event.enabled, prePrayerMinutes = event.minutes)
            }
            is NotificationsEvent.SetDailyReminder -> update {
                it.copy(
                    dailyReminderEnabled = event.enabled,
                    dailyReminderHour = event.hour,
                    dailyReminderMinute = event.minute
                )
            }
            is NotificationsEvent.SetJumuahEnabled -> update { it.copy(jumuahEnabled = event.enabled) }
            is NotificationsEvent.SetSpecialDaysEnabled -> update { it.copy(specialDaysEnabled = event.enabled) }
            is NotificationsEvent.SetSoundEnabled -> update { it.copy(soundEnabled = event.enabled) }
            is NotificationsEvent.SetVibrationEnabled -> update { it.copy(vibrationEnabled = event.enabled) }
            NotificationsEvent.SendTest -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                _uiState.value = NotificationsUiState.Success(getSettingsUseCase())
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    private fun update(transform: (NotificationSettings) -> NotificationSettings) {
        val current = (_uiState.value as? NotificationsUiState.Success)?.settings ?: return
        val updated = transform(current)
        _uiState.value = NotificationsUiState.Success(updated)
        viewModelScope.launch {
            updateSettingsUseCase(updated)
        }
    }
}
