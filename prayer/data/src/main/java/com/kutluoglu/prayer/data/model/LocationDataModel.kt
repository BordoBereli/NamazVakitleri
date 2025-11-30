package com.kutluoglu.prayer.data.model

import kotlinx.serialization.Serializable

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Serializable
data class LocationDataModel(
        val latitude: Double,
        val longitude: Double,
        val country: String?,
        val countryCode: String?,
        val city: String?,
        val county: String?
)
