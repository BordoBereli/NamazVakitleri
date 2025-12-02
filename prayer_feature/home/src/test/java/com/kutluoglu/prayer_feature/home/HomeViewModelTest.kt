package com.kutluoglu.prayer_feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.common.Result.*
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_feature.home.common.PrayerFormatter
import com.kutluoglu.prayer_location.LocationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineRule::class)
class HomeViewModelTest {

    private lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase
    private lateinit var getRandomVerseUseCase: GetRandomVerseUseCase
    private lateinit var saveLocationUseCase: SaveLocationUseCase
    private lateinit var getSavedLocationUseCase: GetSavedLocationUseCase

    private lateinit var calculator: PrayerLogicEngine
    private lateinit var formatter: PrayerFormatter
    private lateinit var locationService: LocationService
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setUp() {
        // Initialize all mocks before each test
        getPrayerTimesUseCase = mockk()
        calculator = mockk(relaxed = true)
        formatter = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        getRandomVerseUseCase = mockk(relaxed = true)
        saveLocationUseCase = mockk(relaxed = true)
        getSavedLocationUseCase = mockk(relaxed = true)

        viewModel = HomeViewModel(
            getPrayerTimesUseCase, // ViewModel calls loadPrayerTimes in init
            getRandomVerseUseCase,
            saveLocationUseCase,
            getSavedLocationUseCase,
            calculator,
            formatter,
            locationService
        )
    }

    @Test
    fun `loadPrayerTimes success should update uiState with prayer list`() = runTest {
        // Arrange / Given
        val testDate = LocalDate(2024, 1, 1)
        val initialPrayerList = listOf(
            Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = testDate)
        )
        // This is the list *after* localization, which the ViewModel should expose
        val localizedPrayerList = listOf(
            Prayer(name = "Sabah", arabicName = "الفجر", time = LocalTime(5, 0), date = testDate)
        )
        // 1. Mock the use case to return the initial list
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns Result.success(initialPrayerList)

        // 2. Mock the formatter to perform the name change
        every { formatter.withLocalizedNames(initialPrayerList) } returns localizedPrayerList

        // 3. Mock location service to avoid null pointers
        coEvery { locationService.getCurrentLocation() } returns null
        val mockLocation = mockk<LocationData>(relaxed = true)
        coEvery { getSavedLocationUseCase() } returns Success(mockLocation)

        // Act / When
        viewModel.loadPrayerTimesForCurrentLocation()

        // Assert / Then
        viewModel.uiState.test {
            val successState = awaitItem() // Await the emission after the successful fetch
            assertThat(successState).isInstanceOf(HomeUiState.Success::class.java)

            // Cast to Success to access its data
            val data = (successState as HomeUiState.Success).data
            assertThat(data.prayers).isEqualTo(localizedPrayerList)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPrayerTimes failure should update uiState with error message`() = runTest {
        // Arrange
        val errorMessage = "Failed to fetch times"
        val exception = RuntimeException(errorMessage)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns Result.failure(exception)
        // Mock location service to avoid null pointers and reach the use case call
        coEvery { locationService.getCurrentLocation() } returns null
        val mockLocation = mockk<LocationData>(relaxed = true)
        coEvery { getSavedLocationUseCase() } returns Success(mockLocation)

        // Act
        viewModel.loadPrayerTimesForCurrentLocation()

        // Assert
        viewModel.uiState.test {
            // Similar to the success test, the first emission we receive will be the final Error state.
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(HomeUiState.Error::class.java)

            // Assert the message within the Error state
            assertThat((errorState as HomeUiState.Error).message).isEqualTo(errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}