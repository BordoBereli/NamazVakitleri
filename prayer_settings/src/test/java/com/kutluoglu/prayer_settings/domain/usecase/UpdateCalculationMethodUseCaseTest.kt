package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateCalculationMethodUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: UpdateCalculationMethodUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateCalculationMethodUseCase(repository)
    }

    @Test
    fun `invoke should call repository updateCalculationMethod with ISNA`() = runTest {
        // Arrange
        coEvery { repository.updateCalculationMethod(any()) } returns Unit

        // Act
        useCase("ISNA")

        // Assert
        coVerify { repository.updateCalculationMethod("ISNA") }
    }

    @Test
    fun `invoke should call repository updateCalculationMethod with MUSLIM_WORLD_LEAGUE`() = runTest {
        // Arrange
        coEvery { repository.updateCalculationMethod(any()) } returns Unit

        // Act
        useCase("MUSLIM_WORLD_LEAGUE")

        // Assert
        coVerify { repository.updateCalculationMethod("MUSLIM_WORLD_LEAGUE") }
    }

    @Test
    fun `invoke should call repository updateCalculationMethod with TURKEY_DIYANET`() = runTest {
        // Arrange
        coEvery { repository.updateCalculationMethod(any()) } returns Unit

        // Act
        useCase("TURKEY_DIYANET")

        // Assert
        coVerify { repository.updateCalculationMethod("TURKEY_DIYANET") }
    }
}
