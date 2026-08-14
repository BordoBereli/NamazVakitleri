package com.kutluoglu.prayer_location.data

import com.kutluoglu.prayer.data.mapper.location.LocationMapper
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer.model.location.LocationEntry
import org.koin.core.annotation.Factory
import java.util.UUID

@Factory
class LocationsMigration(
    private val locationsDataStore: LocationsDataStore,
    private val legacyLocationDataStore: LocationDataStore,
    private val locationMapper: LocationMapper = LocationMapper()
) {
    suspend fun migrateIfNeeded() {
        val state = locationsDataStore.getLocations()
        if (state.entries.isNotEmpty()) return
        val legacy = legacyLocationDataStore.getSavedLocation() ?: return
        val location = locationMapper.mapToDomain(legacy)
        locationsDataStore.addLocation(
            LocationEntry(
                id = UUID.randomUUID().toString(),
                location = location,
                displayName = listOfNotNull(location.city, location.country)
                    .joinToString(", ").ifBlank { "My Location" }
            )
        )
    }
}
