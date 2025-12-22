package com.kutluoglu.prayer_feature.home

import android.R.attr.name
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_feature.common.PrayerFormatter
import com.kutluoglu.prayer_location.LocationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.temporal.TemporalQueries.zoneId
import kotlin.Result.Companion.success

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
        val fajrPrayer = spyk(Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = testDate))
        val initialPrayerList = listOf(fajrPrayer)
        val sabahPrayer = spyk(Prayer(name = "Sabah", arabicName = "الفجر", time = LocalTime(5, 0), date = testDate, isCurrent = false))
        val localizedPrayerList = listOf(sabahPrayer)
        val finalPrayerList = listOf(sabahPrayer.copy(isCurrent = true))


        // 1. Mock UseCases and Services
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns success(initialPrayerList)
        coEvery { getSavedLocationUseCase() } returns success(mockk(relaxed = true))

        // 2. Mock Formatters
        every { formatter.withLocalizedNames(initialPrayerList) } returns localizedPrayerList
        every { formatter.getInitialTimeInfo(any()) } returns mockk(relaxed = true)
        every { formatter.locationInfo(any()) } returns "Mock Location"

        // 3. Mock Calculator
        every { calculator.findCurrentAndNextPrayer(localizedPrayerList) } returns Pair(sabahPrayer, null)

        // 4. Mock the data class 'copy' method
        every { sabahPrayer.copy(isCurrent = true) } returns finalPrayerList.first()


        // Act / When
        viewModel.loadPrayerTimesForCurrentLocation()

        // Assert / Then
        viewModel.uiState.test {
            // Await the final Success state
            val successState = awaitItem()
            assertThat(successState).isInstanceOf(HomeUiState.Success::class.java)

            // Assert the data within the success state
            val prayerState = (successState as HomeUiState.Success).prayerState
            assertThat(prayerState.prayers).isEqualTo(finalPrayerList)
            assertThat(prayerState.prayers.first().isCurrent).isTrue()

            // Ensure no other events are emitted
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
        coEvery { getSavedLocationUseCase() } returns success(mockLocation)

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