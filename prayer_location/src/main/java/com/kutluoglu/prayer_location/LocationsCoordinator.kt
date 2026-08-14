package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.data.LocationsDataStore
import com.kutluoglu.prayer_location.data.LocationsMigration
import com.kutluoglu.prayer_location.data.LocationsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class LocationsCoordinator(
    private val locationsDataStore: LocationsDataStore,
    private val locationService: LocationService,
    private val activeLocationProvider: ActiveLocationProvider,
    private val locationsMigration: LocationsMigration
) {
    private val _gpsLocation = MutableStateFlow<LocationData?>(null)

    fun observeState(): Flow<LocationsState> =
        combine(locationsDataStore.observeLocations(), _gpsLocation) { state, gps ->
            if (state.gpsEnabled && gps != null) {
                state.copy(entries = listOf(gps.toEntry()) + state.entries)
            } else {
                state
            }
        }

    fun setGpsLocation(location: LocationData?) {
        _gpsLocation.value = location
    }

    suspend fun resolveInitial(): LocationData? {
        locationsMigration.migrateIfNeeded()
        val state = locationsDataStore.getLocations()
        if (state.selectedId == GPS_LOCATION_ID && state.gpsEnabled) {
            return resolveGps()
        }
        val selected = state.entries.firstOrNull { it.id == state.selectedId }
            ?: state.entries.firstOrNull()
        if (selected != null) {
            activeLocationProvider.set(selected.location)
            return selected.location
        }
        if (state.gpsEnabled) {
            return resolveGps()
        }
        return null
    }

    suspend fun resolveSelected(): LocationData? {
        val state = locationsDataStore.getLocations()
        if (state.selectedId == GPS_LOCATION_ID && state.gpsEnabled) {
            return resolveGps()
        }
        val selected = state.entries.firstOrNull { it.id == state.selectedId }
            ?: state.entries.firstOrNull()
        if (selected != null) {
            activeLocationProvider.set(selected.location)
            return selected.location
        }
        if (state.gpsEnabled) {
            return resolveGps()
        }
        return null
    }

    private suspend fun resolveGps(): LocationData? {
        val gps = refreshGps()
        if (gps != null) activeLocationProvider.set(gps)
        return gps
    }

    suspend fun refreshGps(): LocationData? {
        val gps = locationService.getCurrentLocation() ?: return null
        _gpsLocation.value = gps
        return gps
    }

    suspend fun selectLocation(id: String) {
        if (id == GPS_LOCATION_ID) {
            locationsDataStore.setSelectedLocation(GPS_LOCATION_ID)
            val gps = _gpsLocation.value ?: refreshGps()
            if (gps != null) activeLocationProvider.set(gps)
            return
        }
        locationsDataStore.setSelectedLocation(id)
        val entry = locationsDataStore.getLocations().entries.firstOrNull { it.id == id }
        entry?.let { activeLocationProvider.set(it.location) }
    }

    suspend fun addLocation(entry: LocationEntry) {
        locationsDataStore.addLocation(entry)
        val state = locationsDataStore.getLocations()
        if (state.selectedId == null) {
            locationsDataStore.setSelectedLocation(entry.id)
            activeLocationProvider.set(entry.location)
        }
    }

    suspend fun removeLocation(id: String) {
        locationsDataStore.removeLocation(id)
    }

    suspend fun reorderLocations(ids: List<String>) {
        locationsDataStore.reorderLocations(ids)
    }

    suspend fun setGpsEnabled(enabled: Boolean) {
        locationsDataStore.setGpsEnabled(enabled)
    }

    private fun LocationData.toEntry(): LocationEntry =
        LocationEntry(
            id = GPS_LOCATION_ID,
            location = this,
            isAutoGps = true,
            displayName = listOfNotNull(city, country).joinToString(", ").ifBlank { "GPS" }
        )

    companion object {
        const val GPS_LOCATION_ID = "gps"
    }
}
