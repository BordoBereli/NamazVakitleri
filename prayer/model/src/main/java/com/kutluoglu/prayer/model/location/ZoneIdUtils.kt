package com.kutluoglu.prayer.model.location

import java.time.DateTimeException
import java.time.ZoneId
import java.util.TimeZone

/**
 * Maps ISO-3166 alpha-2 country codes to a representative IANA time-zone ID.
 * Shared by [timeZoneIdFor] and the legacy `CitySearchRemoteDataSource` logic.
 */
private val COUNTRY_TIME_ZONE_IDS: Map<String, String> = mapOf(
    "TR" to "Europe/Istanbul",
    "SA" to "Asia/Riyadh",
    "EG" to "Africa/Cairo",
    "ID" to "Asia/Jakarta",
    "MY" to "Asia/Kuala_Lumpur",
    "PK" to "Asia/Karachi",
    "IN" to "Asia/Kolkata",
    "BD" to "Asia/Dhaka",
    "NG" to "Africa/Lagos",
    "MA" to "Africa/Casablanca",
    "DZ" to "Africa/Algiers",
    "TN" to "Africa/Tunis",
    "JO" to "Asia/Amman",
    "AE" to "Asia/Dubai",
    "KW" to "Asia/Kuwait",
    "QA" to "Asia/Qatar",
    "BH" to "Asia/Bahrain",
    "OM" to "Asia/Muscat",
    "GB" to "Europe/London",
    "US" to "America/New_York",
    "DE" to "Europe/Berlin",
    "FR" to "Europe/Paris"
)

/**
 * Returns an IANA time-zone ID for the given coordinates and country code.
 *
 * Resolution order:
 * 1. A zone from [countryCode] via [COUNTRY_TIME_ZONE_IDS] (e.g. "TR" -> "Europe/Istanbul").
 * 2. A longitude-offset fallback ("UTC", "UTC+3", ...) preserving the legacy
 *    behavior of the former `CitySearchRemoteDataSource` timezone logic.
 *
 * The offset fallback means this effectively always returns a value; the nullable
 * return type lets callers treat "no sensible zone" as null. [latitude] is currently
 * unused by the heuristic but kept for API symmetry and future precision.
 */
fun timeZoneIdFor(latitude: Double, longitude: Double, countryCode: String?): String? {
    val code = countryCode?.uppercase()
    if (code != null) {
        COUNTRY_TIME_ZONE_IDS[code]?.let { return it }
    }
    val offset = ((longitude + 180) / 30).toInt().coerceIn(-12, 12)
    return when {
        offset == 0 -> "UTC"
        offset > 0 -> "UTC+$offset"
        else -> "UTC$offset"
    }
}

/**
 * Resolves the IANA time-zone for a [LocationData].
 *
 * Resolution order:
 * 1. The stored [LocationData.timeZoneId] if it is a valid IANA zone ID.
 * 2. A zone derived from [LocationData.countryCode] via the available-ID prefix heuristic.
 * 3. The JVM/device default zone.
 *
 * NOTE: The country-prefix heuristic below is duplicated in
 * `core/common/.../utils/ZoneIdUtils.kt` (`getZoneIdFromLocation`). Module layering
 * prevents sharing this logic, so any fix to the heuristic MUST be applied in both places.
 */
fun resolveZoneId(location: LocationData): ZoneId {
    val storedZoneId = location.timeZoneId
    if (!storedZoneId.isNullOrBlank()) {
        try {
            return ZoneId.of(storedZoneId)
        } catch (_: DateTimeException) {
            // Invalid stored zone ID; fall through to the country heuristic.
        }
    }

    val countryCode = location.countryCode
    if (!countryCode.isNullOrBlank()) {
        val timeZoneIds = TimeZone.getAvailableIDs().filter {
            it.startsWith(countryCode, true)
        }
        if (timeZoneIds.isNotEmpty()) {
            // The first match is arbitrary for multi-zone countries (inherited behavior).
            return ZoneId.of(timeZoneIds[0])
        }
    }

    return ZoneId.systemDefault()
}
