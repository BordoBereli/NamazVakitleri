package com.kutluoglu.namazvakitleri

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.namazvakitleri.ui.viewModel.HomeViewModel
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineRule::class)
class HomeViewModelTest {

    private lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase
    private lateinit var viewModel: HomeViewModel

    @Test
    fun `loadPrayerTimes success should update uiState with prayer list`() = runTest {
        // Arrange
        val testDate = LocalDate(2024, 1, 1)
        val mockPrayerList = listOf(
            Prayer(
                name = "Fajr",
                arabicName = "الفجر",
                time = LocalTime.parse("16:30"),
                date = testDate,
                isCurrent = false,
                notificationEnabled = false
            )
        )
        getPrayerTimesUseCase = mockk()
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any()) } returns Result.success(mockPrayerList)

        // Act
        viewModel = HomeViewModel(getPrayerTimesUseCase) // ViewModel calls loadPrayerTimes in init

        // Assert
        viewModel.uiState.test {
            val successState = awaitItem() // Await the emission after the successful fetch
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.prayers).isEqualTo(mockPrayerList)
            assertThat(successState.error).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPrayerTimes failure should update uiState with error message`() = runTest {
        // Arrange
        val errorMessage = "Failed to fetch times"
        getPrayerTimesUseCase = mockk()
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any()) } returns Result.failure(RuntimeException(errorMessage))

        // Act
        viewModel = HomeViewModel(getPrayerTimesUseCase)

        // Assert
        viewModel.uiState.test {
            val errorState = awaitItem() // Await the emission after the failed fetch
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.prayers).isEmpty()
            assertThat(errorState.error).isEqualTo(errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}