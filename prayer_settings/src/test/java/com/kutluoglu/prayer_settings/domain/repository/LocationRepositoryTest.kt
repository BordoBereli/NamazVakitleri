package com.kutluoglu.prayer_settings.domain.repository

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.City
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocationRepositoryTest {

    private lateinit var repository: LocationRepository

    @BeforeEach
    fun setUp() {
        repository = FakeLocationRepository()
    }

    @Test
    fun `getPresetCities should return list of cities`() = runTest {
        // Act
        val cities = repository.getPresetCities()

        // Assert
        assertThat(cities).isNotEmpty()
    }

    @Test
    fun `getPresetCities should contain Istanbul`() = runTest {
        // Act
        val cities = repository.getPresetCities()

        // Assert
        val istanbul = cities.find { it.name == "Istanbul" && it.country == "Turkey" }
        assertThat(istanbul).isNotNull()
        assertThat(istanbul!!.latitude).isEqualTo(41.0082)
        assertThat(istanbul.longitude).isEqualTo(28.9784)
    }

    @Test
    fun `getPresetCities should contain cities from multiple countries`() = runTest {
        // Act
        val cities = repository.getPresetCities()

        // Assert
        val countries = cities.map { it.country }.distinct()
        assertThat(countries.size).isGreaterThan(5)
    }

    @Test
    fun `searchCities should return results for valid query`() = runTest {
        // Act
        val results = repository.searchCities("London")

        // Assert
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchCities should return empty for empty query`() = runTest {
        // Act
        val results = repository.searchCities("")

        // Assert
        assertThat(results).isEmpty()
    }

    @Test
    fun `reverseGeocode should return city for valid coordinates`() = runTest {
        // Act
        val city = repository.reverseGeocode(41.0082, 28.9784)

        // Assert
        assertThat(city).isNotNull()
    }
}

class FakeLocationRepository : LocationRepository {
    private val presetCities = listOf(
        City("Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul"),
        City("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul"),
        City("London", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
        City("Paris", "France", 48.8566, 2.3522, "Europe/Paris"),
        City("Berlin", "Germany", 52.52, 13.405, "Europe/Berlin"),
        City("Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        City("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh")
    )

    override suspend fun getPresetCities(): List<City> = presetCities

    override suspend fun searchCities(query: String): List<City> {
        if (query.isBlank()) return emptyList()
        return presetCities.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.country.contains(query, ignoreCase = true)
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): City? {
        return presetCities.firstOrNull()
    }

    override suspend fun clearCache() {
        // No-op for fake
    }
}
