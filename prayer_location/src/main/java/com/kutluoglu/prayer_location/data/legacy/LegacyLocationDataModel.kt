package com.kutluoglu.prayer_location.data.legacy

import kotlinx.serialization.Serializable

/**
 * Legacy persistence DTO for a single saved location. Retained for migration
 * from the old single-location store into the modern multi-location store.
 */
@Serializable
data class LegacyLocationDataModel(
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val countryCode: String?,
    val city: String?,
    val county: String?,
    val timeZoneId: String? = null
)
