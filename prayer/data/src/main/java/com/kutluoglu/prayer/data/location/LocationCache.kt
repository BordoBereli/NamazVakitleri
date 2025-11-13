package com.kutluoglu.prayer.data.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

// Define the DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_cache")

@Single
class LocationCache(private val context: Context) {

    companion object {
        // Define a key for storing the location data as a JSON string
        private val KEY_LOCATION_DATA = stringPreferencesKey("prayer_location_data")
    }

    /**
     * Saves the provided LocationData to DataStore.
     * It serializes the object into a JSON string for storage.
     */
    suspend fun saveLocation(locationData: LocationData) {
        context.dataStore.edit { preferences ->
            val jsonString = Json.Default.encodeToString(locationData)
            preferences[KEY_LOCATION_DATA] = jsonString
        }
    }

    /**
     * Retrieves the last saved LocationData from DataStore.
     * It reads the JSON string and deserializes it back into a LocationData object.
     * Returns null if no location has been saved yet.
     */
    suspend fun getSavedLocation(): LocationData? {
        return context.dataStore.data
            .map { preferences ->
                preferences[KEY_LOCATION_DATA]?.let { jsonString ->
                    try {
                        Json.Default.decodeFromString<LocationData>(jsonString)
                    } catch (e: Exception) {
                        // Handle potential deserialization errors, e.g., if the data class changes
                        null
                    }
                }
            }.firstOrNull()
    }
}