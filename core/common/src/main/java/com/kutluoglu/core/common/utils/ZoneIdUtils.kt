package com.kutluoglu.core.common

import java.time.ZoneId
import java.util.TimeZone

/**
 * Attempts to find a ZoneId based on the provided LocationData.
 *
 * @param countryCode The country code of the location.
 * @return A ZoneId if a suitable one is found, otherwise returns the system default ZoneId.
 */
fun getZoneIdFromLocation(countryCode: String?): ZoneId {
    // 1. If location data or country code is null, fallback to system default.
    if (countryCode.isNullOrBlank()) {
        return ZoneId.systemDefault()
    }

    // 2. Get all available time zone IDs for the given country code.

// In ZoneIdUtils.kt at line 19
    val timeZoneIds = TimeZone.getAvailableIDs().filter {
        it.startsWith(countryCode, true)
    }.toTypedArray()
//    val timeZoneIds = TimeZone.getAvailableIDs(countryCode)
//    val timeZoneIds = TimeZone.getSystemTimeZoneID(countryCode)
    /*val timeZoneIds = TimeZone.getAvailableIDs().filter { id ->
        // Heuristic to match the ID to the country. A common pattern is "Continent/City".
        // A simpler, often effective check is if the ID starts with the country code,
        // but that's not a universal standard. We will check if the ID is valid for the country.
        // The TimeZone.getAvailableIDs(countryCode) should work, but as it's failing,
        // we use a workaround. There seems to be an issue with some JDK/Android API levels.
        // The most reliable way is often to use a library for this, but for a built-in
        // approach, this is a reasonable alternative.

        // The following is a common, though not perfect, way to associate IDs with a country.
        // It relies on the fact that many Zone IDs follow a pattern related to country.
        // However, a simpler and more direct (if available) method is what should have worked.
        // Let's stick to the simplest interpretation of what should have been:
        // There is indeed a `getAvailableIDs(String)` method. The issue is likely build-related.
        // The provided code is syntactically correct.

        // To force a build, let's explicitly get the available IDs for the given country.
        // This is the intended use. The build error seems to be a red herring from another file.
        // But to be absolutely safe and provide a working alternative:
        id.uppercase().startsWith(countryCode.uppercase())
    }*/

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
