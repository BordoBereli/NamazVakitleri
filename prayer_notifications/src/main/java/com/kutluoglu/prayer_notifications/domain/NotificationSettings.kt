package com.kutluoglu.prayer_notifications.domain

data class NotificationSettings(
    val enabled: Boolean = false,
    val prayerToggles: Map<String, Boolean> = defaultPrayerToggles(),
    val adhanEnabled: Boolean = false,
    val adhanPrayerToggles: Map<String, Boolean> = defaultAdhanPrayerToggles(),
    val adhanVolume: Int = 100,
    val adhanStyles: Map<String, String> = emptyMap(),
    val countdownEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 8,
    val dailyReminderMinute: Int = 0,
    val prePrayerReminderEnabled: Boolean = false,
    val prePrayerMinutes: Int = 15,
    val jumuahEnabled: Boolean = true,
    val specialDaysEnabled: Boolean = true,
    val ramadanEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    companion object {
        val PRAYER_KEYS = listOf("Imsak", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun defaultPrayerToggles(): Map<String, Boolean> =
            PRAYER_KEYS.associateWith { true }

        fun defaultAdhanPrayerToggles(): Map<String, Boolean> =
            PRAYER_KEYS.associateWith { it == "Imsak" || it == "Dhuhr" || it == "Asr" || it == "Maghrib" || it == "Isha" }
    }
}
