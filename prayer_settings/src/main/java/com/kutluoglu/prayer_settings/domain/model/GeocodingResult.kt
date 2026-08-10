package com.kutluoglu.prayer_settings.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResult(
    val lat: String,
    val lon: String,
    val display_name: String,
    @SerialName("address")
    val address: GeocodingAddress? = null
)

@Serializable
data class GeocodingAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val state: String? = null,
    val county: String? = null,
    @SerialName("state_district")
    val stateDistrict: String? = null,
    val country: String? = null,
    val country_code: String? = null
) {
    fun getCityName(): String = city ?: town ?: village ?: ""
    fun getCountyName(): String = county ?: stateDistrict ?: state ?: ""
}
