package com.kutluoglu.prayer_settings.domain.repository

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun `observeSettings should return Settings flow`() = runTest {
        // Arrange
        val expectedSettings = Settings()
        coEvery { repository.observeSettings() } returns flowOf(expectedSettings)

        // Act
        val result = repository.observeSettings()

        // Assert
        assertThat(result).isNotNull()
    }

    @Test
    fun `getSettings should return Settings`() = runTest {
        // Arrange
        val expectedSettings = Settings(
            calculationMethod = "ISNA",
            language = "en",
            hijriAdjustment = 1
        )
        coEvery { repository.getSettings() } returns expectedSettings

        // Act
        val result = repository.getSettings()

        // Assert
        assertThat(result.calculationMethod).isEqualTo("ISNA")
        assertThat(result.language).isEqualTo("en")
        assertThat(result.hijriAdjustment).isEqualTo(1)
    }

    @Test
    fun `updateLocation should call repository`() = runTest {
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
        repository.updateLocation(location)

        // Assert
        coVerify { repository.updateLocation(location) }
    }

    @Test
    fun `updateCalculationMethod should call repository`() = runTest {
        // Arrange
        coEvery { repository.updateCalculationMethod(any()) } returns Unit

        // Act
        repository.updateCalculationMethod("ISNA")

        // Assert
        coVerify { repository.updateCalculationMethod("ISNA") }
    }

    @Test
    fun `updateLanguage should call repository`() = runTest {
        // Arrange
        coEvery { repository.updateLanguage(any()) } returns Unit

        // Act
        repository.updateLanguage("ar")

        // Assert
        coVerify { repository.updateLanguage("ar") }
    }

    @Test
    fun `updateHijriAdjustment should call repository`() = runTest {
        // Arrange
        coEvery { repository.updateHijriAdjustment(any()) } returns Unit

        // Act
        repository.updateHijriAdjustment(1)

        // Assert
        coVerify { repository.updateHijriAdjustment(1) }
    }
}
