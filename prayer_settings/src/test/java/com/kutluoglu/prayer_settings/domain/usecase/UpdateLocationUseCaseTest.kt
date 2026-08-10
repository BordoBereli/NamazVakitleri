package com.kutluoglu.prayer_settings.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateLocationUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: UpdateLocationUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateLocationUseCase(repository)
    }

    @Test
    fun `invoke should call repository updateLocation`() = runTest {
        // Arrange
        val location = LocationSettings(
            latitude = 51.5074,
            longitude = -0.1278,
            cityName = "London",
            country = "United Kingdom",
            timeZone = "Europe/London"
        )
        coEvery { repository.updateLocation(any()) } returns Unit

        // Act
        useCase(location)

        // Assert
        coVerify { repository.updateLocation(location) }
    }

    @Test
    fun `invoke should update with correct location data`() = runTest {
        // Arrange
        val location = LocationSettings(
            latitude = 40.7128,
            longitude = -74.0060,
            cityName = "New York",
            country = "USA",
            timeZone = "America/New_York"
        )

        // Act
        useCase(location)

        // Assert
        coVerify {
            repository.updateLocation(
                withArg {
                    assertThat(it.latitude).isEqualTo(40.7128)
                    assertThat(it.longitude).isEqualTo(-74.0060)
                    assertThat(it.cityName).isEqualTo("New York")
                    assertThat(it.country).isEqualTo("USA")
                }
            )
        }
    }
}
