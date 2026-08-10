package com.kutluoglu.prayer_settings.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateHijriAdjustmentUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: UpdateHijriAdjustmentUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateHijriAdjustmentUseCase(repository)
    }

    @Test
    fun `invoke should call repository updateHijriAdjustment with positive days`() = runTest {
        // Arrange
        coEvery { repository.updateHijriAdjustment(any()) } returns Unit

        // Act
        useCase(1)

        // Assert
        coVerify { repository.updateHijriAdjustment(1) }
    }

    @Test
    fun `invoke should call repository updateHijriAdjustment with negative days`() = runTest {
        // Arrange
        coEvery { repository.updateHijriAdjustment(any()) } returns Unit

        // Act
        useCase(-1)

        // Assert
        coVerify { repository.updateHijriAdjustment(-1) }
    }

    @Test
    fun `invoke should call repository updateHijriAdjustment with zero`() = runTest {
        // Arrange
        coEvery { repository.updateHijriAdjustment(any()) } returns Unit

        // Act
        useCase(0)

        // Assert
        coVerify { repository.updateHijriAdjustment(0) }
    }

    @Test
    fun `invoke should update with correct adjustment value`() = runTest {
        // Arrange
        val adjustment = 5

        // Act
        useCase(adjustment)

        // Assert
        coVerify {
            repository.updateHijriAdjustment(
                withArg {
                    assertThat(it).isEqualTo(5)
                }
            )
        }
    }
}
