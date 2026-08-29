package com.kutluoglu.namazvakitleri.notifications

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationReschedulerTest {

    private val scheduler = mockk<AlarmScheduler>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)

    private val settingsFlow = MutableStateFlow(Settings())
    private val locationsFlow = MutableStateFlow(LocationsState())

    private lateinit var rescheduler: NotificationRescheduler

    @BeforeEach
    fun setUp() {
        every { settingsRepository.observeSettings() } returns settingsFlow
        every { locationsCoordinator.observeState() } returns locationsFlow
        rescheduler = NotificationRescheduler(scheduler, settingsRepository, locationsCoordinator, debounceMillis = 0)
    }

    @Test
    fun `settings change reschedules notifications`() = runTest {
        rescheduler.start(backgroundScope)
        runCurrent()

        settingsFlow.value = Settings(calculationMethod = "ISNA")
        runCurrent()

        verify { scheduler.scheduleAll() }
    }

    @Test
    fun `location change reschedules notifications`() = runTest {
        rescheduler.start(backgroundScope)
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

        verify { scheduler.scheduleAll() }
    }

    @Test
    fun `gps location update reschedules notifications`() = runTest {
        rescheduler.start(backgroundScope)
        runCurrent()

        locationsFlow.value = LocationsState(
            entries = listOf(
                LocationEntry(
                    id = "gps",
                    location = LocationData(
                        latitude = 39.9,
                        longitude = 32.8,
                        country = "Turkey",
                        countryCode = "TR",
                        city = "Ankara",
                        county = null
                    ),
                    isAutoGps = true,
                    displayName = "GPS"
                )
            ),
            gpsEnabled = true,
            selectedId = "gps"
        )
        runCurrent()

        verify { scheduler.scheduleAll() }
    }

    @Test
    fun `initial emission does not reschedule`() = runTest {
        rescheduler.start(backgroundScope)
        runCurrent()

        verify(exactly = 0) { scheduler.scheduleAll() }
    }

    @Test
    fun `language only change does not reschedule`() = runTest {
        rescheduler.start(backgroundScope)
        runCurrent()

        settingsFlow.value = Settings(language = "tr")
        runCurrent()

        verify(exactly = 0) { scheduler.scheduleAll() }
    }
}
