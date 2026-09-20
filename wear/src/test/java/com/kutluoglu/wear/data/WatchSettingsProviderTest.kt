package com.kutluoglu.wear.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
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
class WatchSettingsProviderTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var provider: WatchSettingsProvider
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = createTempDirectory().toFile()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        provider = WatchSettingsProvider(dataStore)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `returns defaults when store empty`() = runTest {
        val settings = provider.getSettings()

        assertThat(settings.calculationMethod).isEqualTo(CalculationMethod.TURKEY_DIYANET)
        assertThat(settings.juristicMethod).isEqualTo(JuristicMethod.STANDARD)
    }

    @Test
    fun `returns persisted values after update`() = runTest {
        provider.updateSettings(
            WatchSettings(
                calculationMethod = CalculationMethod.MWL,
                juristicMethod = JuristicMethod.HANAFI
            )
        )

        val settings = provider.getSettings()

        assertThat(settings.calculationMethod).isEqualTo(CalculationMethod.MWL)
        assertThat(settings.juristicMethod).isEqualTo(JuristicMethod.HANAFI)
    }
}
