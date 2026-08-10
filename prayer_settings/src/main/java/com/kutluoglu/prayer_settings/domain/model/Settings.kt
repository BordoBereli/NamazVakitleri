package com.kutluoglu.prayer_settings.domain.model

data class Settings(
    val location: LocationSettings = LocationSettings(),
    val calculationMethod: String = "TURKEY_DIYANET",
    val language: String = "tr",
    val hijriAdjustment: Int = 0
)

data class LocationSettings(
    val latitude: Double = 41.0082,
    val longitude: Double = 28.9784,
    val cityName: String = "Istanbul",
    val district: String? = null,
    val country: String = "Turkey",
    val timeZone: String = "Europe/Istanbul"
) {
    fun displayName(): String = when {
        district != null && cityName != district -> "$district, $cityName, $country"
        else -> "$cityName, $country"
    }
}
