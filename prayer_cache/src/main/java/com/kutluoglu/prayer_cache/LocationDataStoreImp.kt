package com.kutluoglu.prayer_cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

// Define the DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_cache")

@Single
class LocationDataStoreImp(
    private val context: Context
): LocationDataStore {

    companion object {
        // Define a key for storing the location data as a JSON string
        private val KEY_LOCATION_DATA = stringPreferencesKey("prayer_location_data")
    }

    /**
     * Saves the provided [LocationDataModel] to DataStore.
     * It serializes the object into a JSON string for storage.
     */
    override suspend fun saveLocation(locationDataModel: LocationDataModel) {
        context.dataStore.edit { preferences ->
            val jsonString = Json.Default.encodeToString(locationDataModel)
            preferences[KEY_LOCATION_DATA] = jsonString
        }
    }

    /**
     * Retrieves the last saved [LocationDataModel] from DataStore.
     * It reads the JSON string and deserializes it back into a LocationData object.
     * Returns null if no location has been saved yet.
     */
    override suspend fun getSavedLocation(): LocationDataModel? {
        return context.dataStore.data
            .map { preferences ->
                preferences[KEY_LOCATION_DATA]?.let { jsonString ->
                    try {
                        Json.Default.decodeFromString<LocationDataModel>(jsonString)
                    } catch (e: Exception) {
                        // Handle potential deserialization errors, e.g., if the data class changes
                        null
                    }
                }
            }.firstOrNull()
    }
}