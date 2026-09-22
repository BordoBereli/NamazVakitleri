package com.kutluoglu.prayer_location.data.legacy

import com.kutluoglu.prayer.model.location.LocationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LegacyLocationMapperTest {

    private val mapper = LegacyLocationMapper()

    @Test
    fun `mapToDomain passes timeZoneId through`() {
        val model = LegacyLocationDataModel(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null,
            timeZoneId = "Europe/Istanbul"
        )

        val domain = mapper.mapToDomain(model)

        assertEquals("Europe/Istanbul", domain.timeZoneId)
    }

    @Test
    fun `mapToDomain defaults timeZoneId to null for old data`() {
        val model = LegacyLocationDataModel(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )

        assertNull(mapper.mapToDomain(model).timeZoneId)
    }

    @Test
    fun `mapFromDomain passes timeZoneId through`() {
        val domain = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null,
            timeZoneId = "Europe/Istanbul"
        )

        val model = mapper.mapFromDomain(domain)

        assertEquals("Europe/Istanbul", model.timeZoneId)
    }

    @Test
    fun `mapFromDomain defaults timeZoneId to null when domain has none`() {
        val domain = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )

        val model = mapper.mapFromDomain(domain)

        assertNull(model.timeZoneId)
    }
}
