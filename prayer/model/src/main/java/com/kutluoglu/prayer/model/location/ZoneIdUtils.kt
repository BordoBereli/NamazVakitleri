package com.kutluoglu.prayer.model.location

import java.time.ZoneId
import java.util.TimeZone

/**
 * Resolves the IANA time-zone for a [LocationData].
 *
 * Resolution order:
 * 1. The stored [LocationData.timeZoneId] if it is a valid IANA zone ID.
 * 2. A zone derived from [LocationData.countryCode] via the available-ID prefix heuristic.
 * 3. The JVM/device default zone.
 */
fun resolveZoneId(location: LocationData): ZoneId {
    val storedZoneId = location.timeZoneId
    if (!storedZoneId.isNullOrBlank()) {
        runCatching { ZoneId.of(storedZoneId) }.getOrNull()?.let { return it }
    }

    val countryCode = location.countryCode
    if (!countryCode.isNullOrBlank()) {
        val timeZoneIds = TimeZone.getAvailableIDs().filter {
            it.startsWith(countryCode, true)
        }
        if (timeZoneIds.isNotEmpty()) {
            return ZoneId.of(timeZoneIds[0])
        }
    }

    return ZoneId.systemDefault()
}
