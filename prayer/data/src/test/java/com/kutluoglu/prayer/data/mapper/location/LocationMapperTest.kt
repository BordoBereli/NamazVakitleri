package com.kutluoglu.prayer.data.mapper.location

import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.model.location.LocationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocationMapperTest {

    private val mapper = LocationMapper()

    @Test
    fun `mapToDomain passes timeZoneId through`() {
        val model = LocationDataModel(
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
        val model = LocationDataModel(
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
}
