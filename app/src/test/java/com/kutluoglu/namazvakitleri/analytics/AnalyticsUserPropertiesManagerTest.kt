package com.kutluoglu.namazvakitleri.analytics

import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.core.common.analytics.AnalyticsUserProperties
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsUserPropertiesManagerTest {

    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())
    private val locationsFlow = MutableStateFlow(LocationsState())

    private lateinit var manager: AnalyticsUserPropertiesManager

    @BeforeEach
    fun setUp() {
        every { settingsRepository.observeSettings() } returns settingsFlow
        every { locationsCoordinator.observeState() } returns locationsFlow
        manager = AnalyticsUserPropertiesManager(analyticsTracker, settingsRepository, locationsCoordinator)
    }

    @Test
    fun `start mirrors settings onto user properties`() = runTest(UnconfinedTestDispatcher()) {
        manager.start(backgroundScope)

        settingsFlow.value = Settings(
            language = "tr",
            calculationMethod = "TURKEY_DIYANET",
            hijriAdjustment = 2
        )

        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.LANGUAGE, "tr") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.CALCULATION_METHOD, "TURKEY_DIYANET") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.HIJRI_ADJUSTMENT, "2") }
    }

    @Test
    fun `start mirrors locations onto user properties`() = runTest(UnconfinedTestDispatcher()) {
        manager.start(backgroundScope)

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

        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.LOCATION_COUNT, "1") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.GPS_ENABLED, "true") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.ACTIVE_LOCATION_TYPE, "manual") }
    }

    @Test
    fun `start reports gps as active location type when selected entry is auto gps`() = runTest(UnconfinedTestDispatcher()) {
        manager.start(backgroundScope)

        locationsFlow.value = LocationsState(
            entries = listOf(
                LocationEntry(
                    id = "gps",
                    location = LocationData(
                        latitude = 41.0082,
                        longitude = 28.9784,
                        country = "Turkey",
                        countryCode = "TR",
                        city = "Istanbul",
                        county = null
                    ),
                    isAutoGps = true,
                    displayName = "GPS"
                )
            ),
            gpsEnabled = true,
            selectedId = "gps"
        )

        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.ACTIVE_LOCATION_TYPE, "gps") }
    }

    @Test
    fun `start defaults active location type to manual when no entry is selected`() = runTest(UnconfinedTestDispatcher()) {
        manager.start(backgroundScope)

        locationsFlow.value = LocationsState(
            entries = emptyList(),
            gpsEnabled = false,
            selectedId = null
        )

        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.LOCATION_COUNT, "0") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.GPS_ENABLED, "false") }
        verify { analyticsTracker.setUserProperty(AnalyticsUserProperties.ACTIVE_LOCATION_TYPE, "manual") }
    }
}
