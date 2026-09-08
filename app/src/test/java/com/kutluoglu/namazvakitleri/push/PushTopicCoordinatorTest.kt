package com.kutluoglu.namazvakitleri.push

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_notifications.push.TopicSubscriptionManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushTopicCoordinatorTest {

    private val topicSubscriptionManager = mockk<TopicSubscriptionManager>(relaxed = true)
    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)

    private val locationsFlow = MutableStateFlow(LocationsState())

    private lateinit var coordinator: PushTopicCoordinator

    @BeforeEach
    fun setUp() {
        every { locationsCoordinator.observeState() } returns locationsFlow
        coordinator = PushTopicCoordinator(topicSubscriptionManager, locationsCoordinator, debounceMillis = 0)
    }

    private fun location(city: String) = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = city,
        county = null
    )

    private fun stateWith(vararg entries: LocationEntry, selectedId: String? = null) =
        LocationsState(entries = entries.toList(), gpsEnabled = true, selectedId = selectedId)

    @Test
    fun `registers global topic on start`() = runTest {
        coordinator.start(backgroundScope)
        runCurrent()

        coVerify { topicSubscriptionManager.registerGlobal() }
    }

    @Test
    fun `selected location change syncs topics`() = runTest {
        coordinator.start(backgroundScope)
        runCurrent()

        locationsFlow.value = stateWith(
            LocationEntry("loc-1", location("Istanbul"), displayName = "Istanbul"),
            selectedId = "loc-1"
        )
        runCurrent()

        coVerify { topicSubscriptionManager.syncForLocation(location("Istanbul")) }
    }

    @Test
    fun `gps location update syncs topics`() = runTest {
        coordinator.start(backgroundScope)
        runCurrent()

        locationsFlow.value = stateWith(
            LocationEntry("gps", location("Ankara"), isAutoGps = true, displayName = "GPS"),
            selectedId = "gps"
        )
        runCurrent()

        coVerify { topicSubscriptionManager.syncForLocation(location("Ankara")) }
    }

    @Test
    fun `empty initial state does not sync topics`() = runTest {
        coordinator.start(backgroundScope)
        runCurrent()

        coVerify(exactly = 0) { topicSubscriptionManager.syncForLocation(any()) }
    }
}