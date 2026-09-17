package com.kutluoglu.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first

class TileDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        fun create(context: Context): TileDataStore {
            return TileDataStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    produceFile = { context.preferencesDataStoreFile("watch_tile") }
                )
            )
        }

        private val KEY_JSON = stringPreferencesKey("tile_json")
    }

    suspend fun save(json: String) {
        dataStore.edit { it[KEY_JSON] = json }
    }

    suspend fun read(): String? =
        dataStore.data.first()[KEY_JSON]
}
