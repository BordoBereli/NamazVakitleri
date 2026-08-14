package com.kutluoglu.prayer_location.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.location.LocationEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class LocationsDataStore(
    @Named("locations") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val LOCATIONS = stringPreferencesKey("locations")
        val GPS_ENABLED = booleanPreferencesKey("gps_enabled")
        val SELECTED_ID = stringPreferencesKey("selected_location_id")
    }

    fun observeLocations(): Flow<LocationsState> = dataStore.data.map { prefs ->
        LocationsState(
            entries = decodeEntries(prefs[Keys.LOCATIONS]),
            gpsEnabled = prefs[Keys.GPS_ENABLED] ?: false,
            selectedId = prefs[Keys.SELECTED_ID]
        )
    }

    suspend fun getLocations(): LocationsState = observeLocations().first()

    suspend fun addLocation(entry: LocationEntry) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS])
            prefs[Keys.LOCATIONS] = json.encodeToString(current + entry)
        }
    }

    suspend fun removeLocation(id: String) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS]).filterNot { it.id == id }
            prefs[Keys.LOCATIONS] = json.encodeToString(current)
            if (prefs[Keys.SELECTED_ID] == id) prefs.remove(Keys.SELECTED_ID)
        }
    }

    suspend fun reorderLocations(ids: List<String>) {
        dataStore.edit { prefs ->
            val current = decodeEntries(prefs[Keys.LOCATIONS])
            val byId = current.associateBy { it.id }
            val reordered = ids.mapNotNull { byId[it] }
            prefs[Keys.LOCATIONS] = json.encodeToString(reordered)
        }
    }

    suspend fun setGpsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.GPS_ENABLED] = enabled }
    }

    suspend fun setSelectedLocation(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.SELECTED_ID) else prefs[Keys.SELECTED_ID] = id
        }
    }

    suspend fun replaceAll(entries: List<LocationEntry>) {
        dataStore.edit { prefs -> prefs[Keys.LOCATIONS] = json.encodeToString(entries) }
    }

    private fun decodeEntries(raw: String?): List<LocationEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LocationEntry>>(raw) }
            .getOrDefault(emptyList())
    }
}
