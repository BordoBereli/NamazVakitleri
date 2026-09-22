package com.kutluoglu.prayer_location.data

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer.model.location.timeZoneIdFor
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationDataStore
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationMapper
import org.koin.core.annotation.Factory
import java.util.UUID

@Factory
class LocationsMigration(
    private val locationsDataStore: LocationsDataStore,
    private val legacyLocationDataStore: LegacyLocationDataStore,
    private val locationMapper: LegacyLocationMapper = LegacyLocationMapper()
) {
    suspend fun migrateIfNeeded() {
        val state = locationsDataStore.getLocations()
        if (state.entries.isEmpty()) {
            migrateLegacyLocation()
            return
        }
        backfillTimeZones(state.entries)
    }

    private suspend fun migrateLegacyLocation() {
        val legacy = legacyLocationDataStore.getSavedLocation() ?: return
        val location = locationMapper.mapToDomain(legacy).withBackfilledTimeZone()
        locationsDataStore.addLocation(
            LocationEntry(
                id = UUID.randomUUID().toString(),
                location = location,
                displayName = listOfNotNull(location.city, location.country)
                    .joinToString(", ").ifBlank { "My Location" }
            )
        )
    }

    private suspend fun backfillTimeZones(entries: List<LocationEntry>) {
        val updated = entries.map { entry ->
            if (entry.location.timeZoneId.isNullOrBlank()) {
                entry.copy(location = entry.location.withBackfilledTimeZone())
            } else {
                entry
            }
        }
        if (updated != entries) {
            locationsDataStore.replaceAll(updated)
        }
    }

    private fun LocationData.withBackfilledTimeZone(): LocationData {
        if (!timeZoneId.isNullOrBlank()) return this
        val zoneId = timeZoneIdFor(latitude, longitude, countryCode)
        return if (zoneId != null) copy(timeZoneId = zoneId) else this
    }
}
