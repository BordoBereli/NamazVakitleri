package com.kutluoglu.prayer_location

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val countryCode: String?,
    val city: String?,
    val county: String?
)