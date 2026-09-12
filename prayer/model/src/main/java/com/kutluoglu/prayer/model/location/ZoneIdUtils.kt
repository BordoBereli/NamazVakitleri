package com.kutluoglu.prayer.model.location

import java.time.DateTimeException
import java.time.ZoneId
import java.util.TimeZone

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
