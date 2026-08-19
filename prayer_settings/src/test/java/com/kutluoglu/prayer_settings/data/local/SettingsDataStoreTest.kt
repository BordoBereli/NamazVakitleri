package com.kutluoglu.prayer_settings.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SettingsDataStoreTest {

    private lateinit var dataStore: SettingsDataStore
    private lateinit var preferencesDataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDirectory().toFile()
        preferencesDataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        dataStore = SettingsDataStore(preferencesDataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `updateLocation with null district should clear previously saved district`() = runBlocking {
        // Arrange: save a location that has a district
        dataStore.updateLocation(
            LocationSettings(
                latitude = 41.0082,
                longitude = 28.9784,
                cityName = "Trabzon",
                district = "Ortahisar",
                country = "Turkey",
                timeZone = "Europe/Istanbul"
            )
        )

        // Act: update with a province-level location that has no district
        dataStore.updateLocation(
            LocationSettings(
                latitude = 41.0082,
                longitude = 28.9784,
                cityName = "Trabzon",
                district = null,
                country = "Turkey",
                timeZone = "Europe/Istanbul"
            )
        )

        // Assert
        val settings = dataStore.getSettings()
        assertThat(settings.location.district).isNull()
    }

    @Test
    fun `updateLocation with non-null district should update district`() = runBlocking {
        // Act
        dataStore.updateLocation(
            LocationSettings(
                latitude = 41.0082,
                longitude = 28.9784,
                cityName = "Trabzon",
                district = "Ortahisar",
                country = "Turkey",
                timeZone = "Europe/Istanbul"
            )
        )

        // Assert
        val settings = dataStore.getSettings()
        assertThat(settings.location.district).isEqualTo("Ortahisar")
    }

    @Test
    fun `default language is system`() = runBlocking {
        val settings = dataStore.getSettings()
        assertThat(settings.language).isEqualTo("system")
    }
}
