package com.kutluoglu.prayer_settings.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.City
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchLocationUseCaseTest {

    private lateinit var repository: LocationRepository
    private lateinit var useCase: SearchLocationUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SearchLocationUseCase(repository)
    }

    @Test
    fun `invoke should return empty list for blank query`() = runTest {
        // Act
        val result = useCase("")

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `invoke should combine preset and API results without duplicates`() = runTest {
        // Arrange
        val presetCities = listOf(
            City("London", "United Kingdom", 51.5074, -0.1278, "Europe/London")
        )
        val apiCities = listOf(
            City("London", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
            City("London", "Canada", 42.9849, -81.2453, "America/Toronto")
        )
        
        coEvery { repository.getPresetCities() } returns presetCities
        coEvery { repository.searchCities("London") } returns apiCities

        // Act
        val result = useCase("London")

        // Assert
        assertThat(result.size).isEqualTo(2)
    }
}
