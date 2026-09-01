package com.kutluoglu.prayer_feature.settings.location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.City
import org.junit.jupiter.api.Test

class CityLocalizerTest {

    private val city = City(
        name = "Istanbul",
        country = "Turkey",
        latitude = 41.0082,
        longitude = 28.9784,
        timezone = "Europe/Istanbul",
        city = "Istanbul",
        nameTr = "İstanbul",
        nameAr = "إسطنبول",
        nameFa = "استانبول",
        countryTr = "Türkiye",
        countryAr = "تركيا",
        countryFa = "ترکیه",
        cityTr = "İstanbul",
        cityAr = "إسطنبول",
        cityFa = "استانبول"
    )

    @Test
    fun `localizedName returns Turkish name for tr`() {
        assertThat(CityLocalizer.localizedName(city, "tr")).isEqualTo("İstanbul")
    }

    @Test
    fun `localizedName returns Arabic name for ar`() {
        assertThat(CityLocalizer.localizedName(city, "ar")).isEqualTo("إسطنبول")
    }

    @Test
    fun `localizedName returns Farsi name for fa`() {
        assertThat(CityLocalizer.localizedName(city, "fa")).isEqualTo("استانبول")
    }

    @Test
    fun `localizedName falls back to English for unsupported language`() {
        assertThat(CityLocalizer.localizedName(city, "de")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedName falls back to English when localized field missing`() {
        val plain = city.copy(nameTr = null, nameAr = null, nameFa = null)
        assertThat(CityLocalizer.localizedName(plain, "tr")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedCountry returns localized country name`() {
        assertThat(CityLocalizer.localizedCountry(city, "tr")).isEqualTo("Türkiye")
        assertThat(CityLocalizer.localizedCountry(city, "ar")).isEqualTo("تركيا")
        assertThat(CityLocalizer.localizedCountry(city, "fa")).isEqualTo("ترکیه")
        assertThat(CityLocalizer.localizedCountry(city, "en")).isEqualTo("Turkey")
    }

    @Test
    fun `localizedProvince returns localized province name`() {
        assertThat(CityLocalizer.localizedProvince(city, "tr")).isEqualTo("İstanbul")
        assertThat(CityLocalizer.localizedProvince(city, "en")).isEqualTo("Istanbul")
    }

    @Test
    fun `localizedProvince falls back to name when city field missing`() {
        val noCity = city.copy(city = null)
        assertThat(CityLocalizer.localizedProvince(noCity, "en")).isEqualTo("Istanbul")
    }
}
