package com.kutluoglu.prayer.model.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val countryCode: String?,
    val city: String?,
    val county: String?
)
