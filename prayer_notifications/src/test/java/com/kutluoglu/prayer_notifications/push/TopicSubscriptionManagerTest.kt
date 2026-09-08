package com.kutluoglu.prayer_notifications.push

import com.kutluoglu.prayer.model.location.LocationData
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopicSubscriptionManagerTest {

    private val client = mockk<FcmClient>(relaxed = true)
    private lateinit var manager: TopicSubscriptionManager

    @BeforeEach
    fun setUp() {
        manager = TopicSubscriptionManager(client)
    }

    private fun location(
        country: String = "Turkey",
        countryCode: String = "TR",
        city: String? = "Istanbul"
    ) = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = country,
        countryCode = countryCode,
        city = city,
        county = null
    )

    @Test
    fun `registerGlobal subscribes to announcements topic`() = runTest {
        manager.registerGlobal()

        coVerify { client.subscribe("announcements") }
    }

    @Test
    fun `syncForLocation subscribes country and city topics`() = runTest {
        manager.syncForLocation(location())

        coVerify { client.subscribe("country_tr") }
        coVerify { client.subscribe("city_istanbul") }
    }

    @Test
    fun `syncForLocation falls back to normalized country name when countryCode is blank`() = runTest {
        manager.syncForLocation(location(countryCode = ""))

        coVerify { client.subscribe("country_turkey") }
        coVerify(exactly = 0) { client.subscribe("country_tr") }
    }

    @Test
    fun `syncForLocation skips city topic for gps location without city`() = runTest {
        manager.syncForLocation(location(city = null))

        coVerify { client.subscribe("country_tr") }
        coVerify(exactly = 0) { client.subscribe("city_istanbul") }
    }

    @Test
    fun `location change unsubscribes removed topics and subscribes new ones`() = runTest {
        manager.syncForLocation(location())
        manager.syncForLocation(location(city = "Ankara"))

        coVerify { client.unsubscribe("city_istanbul") }
        coVerify { client.subscribe("city_ankara") }
        coVerify(exactly = 1) { client.subscribe("city_istanbul") }
    }

    @Test
    fun `moving to gps-only location keeps country topic and unsubscribes city`() = runTest {
        manager.syncForLocation(location())
        manager.syncForLocation(location(city = null))

        coVerify { client.unsubscribe("city_istanbul") }
        coVerify(exactly = 1) { client.subscribe("country_tr") }
    }

    @Test
    fun `syncForLocation with null location unsubscribes all location topics`() = runTest {
        manager.syncForLocation(location())
        manager.syncForLocation(null)

        coVerify { client.unsubscribe("country_tr") }
        coVerify { client.unsubscribe("city_istanbul") }
    }

    @Test
    fun `same location does not resubscribe`() = runTest {
        manager.syncForLocation(location())
        manager.syncForLocation(location())

        coVerify(exactly = 1) { client.subscribe("country_tr") }
        coVerify(exactly = 1) { client.subscribe("city_istanbul") }
        coVerify(exactly = 0) { client.unsubscribe(any()) }
    }
}