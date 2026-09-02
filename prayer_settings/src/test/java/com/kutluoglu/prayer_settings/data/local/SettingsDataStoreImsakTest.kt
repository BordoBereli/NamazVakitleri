package com.kutluoglu.prayer_settings.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SettingsDataStoreImsakTest {

    private lateinit var dataStore: SettingsDataStore
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDirectory().toFile()
        val preferencesDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "imsak_test.preferences_pb") }
        )
        dataStore = SettingsDataStore(preferencesDataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `imsak offset defaults to 10`() = runTest {
        assertThat(dataStore.getSettings().imsakOffsetMinutes).isEqualTo(10)
    }

    @Test
    fun `imsak offset persists round trip`() = runTest {
        dataStore.updateImsakOffsetMinutes(15)
        assertThat(dataStore.getSettings().imsakOffsetMinutes).isEqualTo(15)
    }
}
