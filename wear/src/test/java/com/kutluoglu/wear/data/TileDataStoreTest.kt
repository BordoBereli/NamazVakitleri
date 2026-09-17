package com.kutluoglu.wear.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class TileDataStoreTest {

    private lateinit var dataStore: TileDataStore
    private lateinit var preferencesDataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = createTempDirectory().toFile()
        preferencesDataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        dataStore = TileDataStore(preferencesDataStore)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `saves and reads json`() = runTest {
        dataStore.save("{\"locationName\":\"İstanbul\"}")

        assertThat(dataStore.read()).isEqualTo("{\"locationName\":\"İstanbul\"}")
    }

    @Test
    fun `read returns null when empty`() = runTest {
        assertThat(dataStore.read()).isNull()
    }
}
