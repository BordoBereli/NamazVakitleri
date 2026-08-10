package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateLanguageUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: UpdateLanguageUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateLanguageUseCase(repository)
    }

    @Test
    fun `invoke should call repository updateLanguage with tr`() = runTest {
        // Arrange
        coEvery { repository.updateLanguage(any()) } returns Unit

        // Act
        useCase("tr")

        // Assert
        coVerify { repository.updateLanguage("tr") }
    }

    @Test
    fun `invoke should call repository updateLanguage with en`() = runTest {
        // Arrange
        coEvery { repository.updateLanguage(any()) } returns Unit

        // Act
        useCase("en")

        // Assert
        coVerify { repository.updateLanguage("en") }
    }

    @Test
    fun `invoke should call repository updateLanguage with ar`() = runTest {
        // Arrange
        coEvery { repository.updateLanguage(any()) } returns Unit

        // Act
        useCase("ar")

        // Assert
        coVerify { repository.updateLanguage("ar") }
    }
}
