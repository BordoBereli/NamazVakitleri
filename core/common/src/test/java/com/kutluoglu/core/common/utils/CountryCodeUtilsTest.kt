package com.kutluoglu.core.common.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CountryCodeUtilsTest {

    @Test
    fun `maps known timezones to country codes`() {
        assertThat(countryCodeFromTimeZone("Europe/Istanbul")).isEqualTo("TR")
        assertThat(countryCodeFromTimeZone("Europe/Berlin")).isEqualTo("DE")
        assertThat(countryCodeFromTimeZone("Europe/London")).isEqualTo("GB")
        assertThat(countryCodeFromTimeZone("Europe/Paris")).isEqualTo("FR")
        assertThat(countryCodeFromTimeZone("Asia/Jakarta")).isEqualTo("ID")
        assertThat(countryCodeFromTimeZone("Asia/Riyadh")).isEqualTo("SA")
    }

    @Test
    fun `returns null for unknown timezones`() {
        assertThat(countryCodeFromTimeZone("Pacific/Auckland")).isNull()
    }
}
