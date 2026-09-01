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
        "tr" -> city.cityTr ?: (city.city ?: city.name)
        "ar" -> city.cityAr ?: (city.city ?: city.name)
        "fa" -> city.cityFa ?: (city.city ?: city.name)
        else -> city.city ?: city.name
    }
}
