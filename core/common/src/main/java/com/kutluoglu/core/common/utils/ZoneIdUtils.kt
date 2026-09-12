package com.kutluoglu.core.common

import java.time.ZoneId
import java.util.TimeZone

/**
 * Attempts to find a ZoneId based on the provided country code.
 *
 * @param countryCode The country code of the location.
 * @return A ZoneId if a suitable one is found, otherwise returns the system default ZoneId.
 *
 * NOTE: The country-prefix heuristic below is duplicated in
 * `prayer/model/.../location/ZoneIdUtils.kt` (`resolveZoneId`). Module layering
 * prevents sharing this logic, so any fix to the heuristic MUST be applied in both places.
 */
fun getZoneIdFromLocation(countryCode: String?): ZoneId {
    // 1. If country code is null or blank, fallback to system default.
    if (countryCode.isNullOrBlank()) {
        return ZoneId.systemDefault()
    }

    // 2. Get all available time zone IDs for the given country code.
    val timeZoneIds = TimeZone.getAvailableIDs().filter {
        it.startsWith(countryCode, true)
    }

    // 3. If no IDs are found for the country, fallback to system default.
    if (timeZoneIds.isEmpty()) {
        return ZoneId.systemDefault()
    }

    // 4. A common strategy: return the first time zone ID for that country.
    // This works well for countries with a single time zone (like Turkey, Germany, UK).
    // For countries with multiple time zones (like USA, Australia), this is a simplification
    // but a reasonable default. A more advanced solution would require city-to-timezone mapping.
    return ZoneId.of(timeZoneIds[0])
}
