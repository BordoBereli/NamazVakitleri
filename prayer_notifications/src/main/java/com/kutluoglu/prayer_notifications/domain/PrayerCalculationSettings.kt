package com.kutluoglu.prayer_notifications.domain

/**
 * The subset of app settings the notification scheduler needs to compute prayer
 * times and special days. Defined here (in the consumer module) so that
 * [PrayerNotificationScheduler] does not depend on `prayer_settings` directly.
 */
data class PrayerCalculationSettings(
    val calculationMethod: String,
    val juristicMethod: String,
    val hijriAdjustment: Int
)

/**
 * Port for the app settings required to compute prayer times. Implemented by the
 * composition root (`:app`), which delegates to `prayer_settings`'s settings use case.
 */
interface PrayerCalculationSettingsProvider {
    suspend fun getSettings(): PrayerCalculationSettings
}
