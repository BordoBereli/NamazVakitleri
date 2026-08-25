package com.kutluoglu.prayer_notifications.domain

data class NotificationSettings(
    val enabled: Boolean = false,
    val prayerToggles: Map<String, Boolean> = defaultPrayerToggles(),
    val adhanEnabled: Boolean = false,
    val adhanVolume: Int = 30,
    val countdownEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 8,
    val dailyReminderMinute: Int = 0,
    val prePrayerReminderEnabled: Boolean = false,
    val prePrayerMinutes: Int = 15,
    val jumuahEnabled: Boolean = true,
    val specialDaysEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    companion object {
        val PRAYER_KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun defaultPrayerToggles(): Map<String, Boolean> =
            PRAYER_KEYS.associateWith { true }
    }
}
