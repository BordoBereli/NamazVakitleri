package com.kutluoglu.prayer_widget

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRefresherTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())
    private val locationsFlow = MutableStateFlow(LocationsState())

    private var refreshCount = 0
    private lateinit var refresher: WidgetRefresher

    @BeforeEach
    fun setUp() {
        every { settingsRepository.observeSettings() } returns settingsFlow
        every { locationsCoordinator.observeState() } returns locationsFlow
        refreshCount = 0
        refresher = WidgetRefresher(
            settingsRepository = settingsRepository,
            locationsCoordinator = locationsCoordinator,
            refreshWidgets = { refreshCount++ },
            debounceMillis = 0
        )
    }

    @Test
    fun `settings change refreshes widgets`() = runTest {
        refresher.start(backgroundScope)
        runCurrent()

        settingsFlow.value = Settings(calculationMethod = "ISNA")
        runCurrent()

        assertThat(refreshCount).isEqualTo(1)
    }

    @Test
    fun `location change refreshes widgets`() = runTest {
        refresher.start(backgroundScope)
        runCurrent()

        locationsFlow.value = LocationsState(
            entries = listOf(
                LocationEntry(
                    id = "loc-1",
                    location = LocationData(
                        latitude = 41.0082,
                        longitude = 28.9784,
                        country = "Turkey",
                        countryCode = "TR",
                        city = "Istanbul",
                        county = null
                    ),
                    isAutoGps = false,
                    displayName = "Istanbul, Turkey"
                )
            ),
            gpsEnabled = true,
            selectedId = "loc-1"
        )
        runCurrent()

        assertThat(refreshCount).isEqualTo(1)
    }

    @Test
    fun `initial emission does not refresh`() = runTest {
        refresher.start(backgroundScope)
        runCurrent()

        assertThat(refreshCount).isEqualTo(0)
    }

    @Test
    fun `language change refreshes widgets`() = runTest {
        refresher.start(backgroundScope)
        runCurrent()

        settingsFlow.value = Settings(language = "tr")
        runCurrent()

        assertThat(refreshCount).isEqualTo(1)
    }
}
