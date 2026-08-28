package com.kutluoglu.prayer_notifications.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NotificationSettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        fun create(context: Context, name: String = "notification_settings_store"): NotificationSettingsDataStore {
            return NotificationSettingsDataStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    produceFile = { context.preferencesDataStoreFile(name) }
                )
            )
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PRAYER_TOGGLES = stringPreferencesKey("prayer_toggles")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val ADHAN_VOLUME = intPreferencesKey("adhan_volume")
        val COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val PRE_PRAYER_ENABLED = booleanPreferencesKey("pre_prayer_enabled")
        val PRE_PRAYER_MINUTES = intPreferencesKey("pre_prayer_minutes")
        val JUMUAH_ENABLED = booleanPreferencesKey("jumuah_enabled")
        val SPECIAL_DAYS_ENABLED = booleanPreferencesKey("special_days_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    fun observeSettings(): Flow<NotificationSettings> = dataStore.data.map { it.toSettings() }

    suspend fun getSettings(): NotificationSettings = dataStore.data.first().toSettings()

    suspend fun updateEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ENABLED] = enabled }

    suspend fun updatePrayerToggle(prayerKey: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.PRAYER_TOGGLES].orEmpty()
                .split(",").filter { it.isNotBlank() }.toMutableSet()
            if (enabled) current.remove(prayerKey) else current.add(prayerKey)
            prefs[Keys.PRAYER_TOGGLES] = current.joinToString(",")
        }
    }

    suspend fun updateAdhanEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ADHAN_ENABLED] = enabled }
    suspend fun updateAdhanVolume(volume: Int) = dataStore.edit { it[Keys.ADHAN_VOLUME] = volume }
    suspend fun updateCountdownEnabled(enabled: Boolean) = dataStore.edit { it[Keys.COUNTDOWN_ENABLED] = enabled }
    suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) = dataStore.edit {
        it[Keys.DAILY_REMINDER_ENABLED] = enabled
        it[Keys.DAILY_REMINDER_HOUR] = hour
        it[Keys.DAILY_REMINDER_MINUTE] = minute
    }
    suspend fun updatePrePrayerReminder(enabled: Boolean, minutes: Int) = dataStore.edit {
        it[Keys.PRE_PRAYER_ENABLED] = enabled
        it[Keys.PRE_PRAYER_MINUTES] = minutes
    }
    suspend fun updateJumuahEnabled(enabled: Boolean) = dataStore.edit { it[Keys.JUMUAH_ENABLED] = enabled }
    suspend fun updateSpecialDaysEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SPECIAL_DAYS_ENABLED] = enabled }
    suspend fun updateSoundEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    suspend fun updateVibrationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }

    private fun Preferences.toSettings(): NotificationSettings {
        val disabled = prayerToggles().filterValues { !it }.keys
        val toggles = NotificationSettings.defaultPrayerToggles().toMutableMap()
        disabled.forEach { toggles[it] = false }
        return NotificationSettings(
            enabled = this[Keys.ENABLED] ?: false,
            prayerToggles = toggles,
            adhanEnabled = this[Keys.ADHAN_ENABLED] ?: false,
            adhanVolume = this[Keys.ADHAN_VOLUME] ?: 100,
            countdownEnabled = this[Keys.COUNTDOWN_ENABLED] ?: true,
            dailyReminderEnabled = this[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderHour = this[Keys.DAILY_REMINDER_HOUR] ?: 8,
            dailyReminderMinute = this[Keys.DAILY_REMINDER_MINUTE] ?: 0,
            prePrayerReminderEnabled = this[Keys.PRE_PRAYER_ENABLED] ?: false,
            prePrayerMinutes = this[Keys.PRE_PRAYER_MINUTES] ?: 15,
            jumuahEnabled = this[Keys.JUMUAH_ENABLED] ?: true,
            specialDaysEnabled = this[Keys.SPECIAL_DAYS_ENABLED] ?: true,
            soundEnabled = this[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = this[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    private fun Preferences.prayerToggles(): Map<String, Boolean> {
        val stored = this[Keys.PRAYER_TOGGLES].orEmpty()
        if (stored.isBlank()) return NotificationSettings.defaultPrayerToggles()
        val disabledKeys = stored.split(",").filter { it.isNotBlank() }.toSet()
        return NotificationSettings.PRAYER_KEYS.associateWith { it !in disabledKeys }
    }
}
