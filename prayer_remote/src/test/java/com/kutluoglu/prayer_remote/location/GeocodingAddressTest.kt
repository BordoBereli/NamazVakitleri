package com.kutluoglu.prayer_remote.location

import com.kutluoglu.prayer.model.location.GeocodingAddress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeocodingAddressTest {

    @Test
    fun `getCityName returns province when city is missing (Bursa schema)`() {
        val address = GeocodingAddress(
            town = "Osmangazi",
            province = "Bursa"
        )

        assertEquals("Bursa", address.getCityName())
    }

    @Test
    fun `getCountyName returns town when county and state are missing (Bursa schema)`() {
        val address = GeocodingAddress(
            town = "Osmangazi",
            province = "Bursa"
        )

        assertEquals("Osmangazi", address.getCountyName())
    }

    @Test
    fun `getCityName returns city when present (Istanbul schema)`() {
        val address = GeocodingAddress(
            city = "İstanbul",
            town = "Fatih",
            province = "İstanbul"
        )

        assertEquals("İstanbul", address.getCityName())
    }

    @Test
    fun `getCountyName returns town as district when city present (Istanbul schema)`() {
        val address = GeocodingAddress(
            city = "İstanbul",
            town = "Fatih",
            province = "İstanbul"
        )

        assertEquals("Fatih", address.getCountyName())
    }

    @Test
    fun `getCountyName prefers city_district over town`() {
        val address = GeocodingAddress(
            town = "Osmangazi",
            cityDistrict = "Kayıhan Mahallesi",
            province = "Bursa"
        )

        assertEquals("Kayıhan Mahallesi", address.getCountyName())
    }

    @Test
    fun `getCityName falls back to state`() {
        val address = GeocodingAddress(
            state = "California"
        )

        assertEquals("California", address.getCityName())
    }

    @Test
    fun `getCityName falls back to village`() {
        val address = GeocodingAddress(
            village = "Küçük Köy",
            province = "Bursa"
        )

        assertEquals("Bursa", address.getCityName())
    }

    @Test
    fun `getCountyName falls back to state`() {
        val address = GeocodingAddress(
            city = "Ankara",
            state = "Ankara"
        )

        assertEquals("Ankara", address.getCountyName())
    }
}
