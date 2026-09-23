package com.kutluoglu.prayer.settings

import kotlinx.coroutines.flow.Flow

/**
 * The subset of app settings the feature consumers need. Defined here (in the
 * shared consumer-facing domain) so that feature modules do not depend on
 * `prayer_settings` directly.
 */
data class AppSettings(
    val calculationMethod: String,
    val juristicMethod: String,
    val hijriAdjustment: Int,
    val language: String,
    val lockPortrait: Boolean,
    val compassAutoRotate: Boolean,
    val location: AppLocation
)

data class AppLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val district: String?,
    val country: String,
    val timeZone: String
)

/**
 * Port for the app settings required by feature consumers. Implemented by
 * `prayer_settings`, which delegates to [SettingsRepository].
 */
interface SettingsProvider {
    suspend fun getSettings(): AppSettings
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateLockPortrait(enabled: Boolean)
    suspend fun updateCompassAutoRotate(enabled: Boolean)
}
