package com.kutluoglu.prayer_auto.data

import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer_remote.location.CitySearchRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

class MosqueSearcherTest {

    private lateinit var remoteDataSource: CitySearchRemoteDataSource
    private lateinit var searcher: MosqueSearcher

    @BeforeEach
    fun setUp() {
        remoteDataSource = mockk()
        searcher = MosqueSearcher(remoteDataSource)
    }

    @Test
    fun `search returns mosques sorted by distance ascending`() = runTest {
        val locationLat = 41.0082
        val locationLon = 28.9784
        coEvery { remoteDataSource.searchPlaces(any()) } returns listOf(
            city("Blue Mosque", 41.0054, 28.9768),
            city("Hagia Sophia Mosque", 41.0086, 28.9802),
            city("Suleymaniye Mosque", 41.0165, 28.9629),
        )

        val mosques = searcher.search(locationLat, locationLon)

        assertThat(mosques).hasSize(3)
        assertThat(mosques.map { it.name }).containsExactly(
            "Hagia Sophia Mosque",
            "Blue Mosque",
            "Suleymaniye Mosque",
        ).inOrder()

        val expected = mapOf(
            "Hagia Sophia Mosque" to 0.157449,
            "Blue Mosque" to 0.33906,
            "Suleymaniye Mosque" to 1.594715,
        )
        mosques.forEach { mosque ->
            assertWithMessage("distance for ${mosque.name}")
                .that(mosque.distanceKm.toDouble())
                .isWithin(1e-3)
                .of(expected.getValue(mosque.name))
        }
    }

    @Test
    fun `search keeps coordinates from search results`() = runTest {
        coEvery { remoteDataSource.searchPlaces(any()) } returns listOf(
            city("Blue Mosque", 41.0054, 28.9768),
        )

        val mosques = searcher.search(41.0082, 28.9784)

        assertThat(mosques).hasSize(1)
        assertThat(mosques.first().latitude).isEqualTo(41.0054)
        assertThat(mosques.first().longitude).isEqualTo(28.9768)
    }

    @Test
    fun `search queries remote data source`() = runTest {
        coEvery { remoteDataSource.searchPlaces(any()) } returns emptyList()

        searcher.search(41.0082, 28.9784)

        coVerify { remoteDataSource.searchPlaces("mosque near 41.0082,28.9784") }
    }

    @Test
    fun `search returns empty list on failure`() = runTest {
        coEvery { remoteDataSource.searchPlaces(any()) } throws RuntimeException("network down")

        val mosques = searcher.search(41.0082, 28.9784)

        assertThat(mosques).isEmpty()
    }

    private fun city(name: String, latitude: Double, longitude: Double): City = City(
        name = name,
        country = "Turkey",
        latitude = latitude,
        longitude = longitude,
        timezone = "Europe/Istanbul",
    )
}
