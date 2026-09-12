package com.kutluoglu.prayer.model.location

import java.time.ZoneId
import java.util.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class ZoneIdUtilsTest {

    private val originalDefault = TimeZone.getDefault()

    @BeforeEach
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalDefault)
    }

    @Test
    fun `stored timezone wins for multi-zone country`() {
        val location = LocationData(
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States",
            countryCode = "US",
            city = "New York",
            county = null,
            timeZoneId = "America/New_York"
        )

        assertEquals(ZoneId.of("America/New_York"), resolveZoneId(location))
    }

    @Test
    fun `stored timezone wins for single-zone country`() {
        val location = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null,
            timeZoneId = "Europe/Istanbul"
        )

        assertEquals(ZoneId.of("Europe/Istanbul"), resolveZoneId(location))
    }

    @Test
    fun `falls back to country heuristic when no stored timezone`() {
        val location = LocationData(
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States",
            countryCode = "US",
            city = "New York",
            county = null,
            timeZoneId = null
        )

        val zone = resolveZoneId(location)

        assertTrue(zone.id.startsWith("US"), "expected US-prefixed zone but was ${zone.id}")
        assertNotEquals(ZoneId.systemDefault(), zone)
    }

    @Test
    fun `TR country code with no stored timezone falls back to system default`() {
        val location = LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null,
            timeZoneId = null
        )

        assertEquals(ZoneId.systemDefault(), resolveZoneId(location))
    }

    @Test
    fun `falls back to system default when no timezone and no country code`() {
        val location = LocationData(
            latitude = 0.0,
            longitude = 0.0,
            country = null,
            countryCode = null,
            city = null,
            county = null,
            timeZoneId = null
        )

        assertEquals(ZoneId.systemDefault(), resolveZoneId(location))
    }

    @Test
    fun `invalid stored timezone falls back to country heuristic without throwing`() {
        val location = LocationData(
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States",
            countryCode = "US",
            city = "New York",
            county = null,
            timeZoneId = "Not/AZone"
        )

        val zone = resolveZoneId(location)

        assertTrue(zone.id.startsWith("US"), "expected US-prefixed zone but was ${zone.id}")
    }

    @Test
    fun `invalid stored timezone with no country falls back to system default`() {
        val location = LocationData(
            latitude = 0.0,
            longitude = 0.0,
            country = null,
            countryCode = null,
            city = null,
            county = null,
            timeZoneId = "Not/AZone"
        )

        assertEquals(ZoneId.systemDefault(), resolveZoneId(location))
    }

    @Test
    fun `timeZoneIdFor returns IANA zone for known country code`() {
        assertEquals("Europe/Istanbul", timeZoneIdFor(41.0082, 28.9784, "TR"))
        assertEquals("America/New_York", timeZoneIdFor(40.7128, -74.0060, "US"))
    }

    @Test
    fun `timeZoneIdFor is case-insensitive for country code`() {
        assertEquals("Europe/Istanbul", timeZoneIdFor(41.0082, 28.9784, "tr"))
    }

    @Test
    fun `timeZoneIdFor falls back to longitude offset for unknown country code`() {
        assertEquals("UTC+6", timeZoneIdFor(41.0082, 28.9784, "ZZ"))
    }

    @Test
    fun `timeZoneIdFor falls back to longitude offset for null country code`() {
        assertEquals("UTC+6", timeZoneIdFor(41.0082, 28.9784, null))
    }

    @Test
    fun `timeZoneIdFor returns UTC for zero offset`() {
        assertEquals("UTC", timeZoneIdFor(0.0, -160.0, null))
    }
}
