package com.kutluoglu.prayer_feature.settings.location

import com.kutluoglu.prayer.model.location.City

object CityLocalizer {

    fun localizedName(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.nameTr ?: city.name
        "ar" -> city.nameAr ?: city.name
        "fa" -> city.nameFa ?: city.name
        else -> city.name
    }

    fun localizedCountry(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.countryTr ?: city.country
        "ar" -> city.countryAr ?: city.country
        "fa" -> city.countryFa ?: city.country
        else -> city.country
    }

    fun localizedProvince(city: City, languageCode: String): String = when (languageCode) {
        "tr" -> city.cityTr ?: localizedName(city, "tr")
        "ar" -> city.cityAr ?: localizedName(city, "ar")
        "fa" -> city.cityFa ?: localizedName(city, "fa")
        else -> city.city ?: city.name
    }
}
