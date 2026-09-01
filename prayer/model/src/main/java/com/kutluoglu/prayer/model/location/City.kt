package com.kutluoglu.prayer.model.location

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val city: String? = null,
    val county: String? = null,
    val nameTr: String? = null,
    val nameAr: String? = null,
    val nameFa: String? = null,
    val countryTr: String? = null,
    val countryAr: String? = null,
    val countryFa: String? = null,
    val cityTr: String? = null,
    val cityAr: String? = null,
    val cityFa: String? = null
) {
    val province: String get() = city ?: name

    fun displayName(): String = when {
        city != null && county != null -> "$name, $county, $city, $country"
        city != null -> "$name, $city, $country"
        county != null -> "$name, $county, $country"
        else -> "$name, $country"
    }
}

@Serializable
data class CityList(
    val cities: List<City>
)
