package com.kutluoglu.prayer_settings.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetSettingsUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: GetSettingsUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetSettingsUseCase(repository)
    }

    @Test
    fun `invoke should return Settings from repository`() = runTest {
        // Arrange
        val expectedSettings = Settings(
            calculationMethod = "ISNA",
            language = "en",
            hijriAdjustment = 1
        )
        coEvery { repository.getSettings() } returns expectedSettings

        // Act
        val result = useCase()

        // Assert
        assertThat(result.calculationMethod).isEqualTo("ISNA")
        assertThat(result.language).isEqualTo("en")
        assertThat(result.hijriAdjustment).isEqualTo(1)
    }

    @Test
    fun `invoke should return default Settings when repository returns default`() = runTest {
        // Arrange
        val defaultSettings = Settings()
        coEvery { repository.getSettings() } returns defaultSettings

        // Act
        val result = useCase()

        // Assert
        assertThat(result.calculationMethod).isEqualTo("TURKEY_DIYANET")
        assertThat(result.language).isEqualTo("system")
        assertThat(result.hijriAdjustment).isEqualTo(0)
    }
}
