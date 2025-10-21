package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalTime

class PrayerRepositoryTest {

    // 1. Declare the dependencies and the class under test
    private lateinit var prayerCalculationService: PrayerCalculationService
    private lateinit var repository: PrayerRepository

    @BeforeEach
    fun setUp() {
        // 2. Create a mock of the dependency
        prayerCalculationService = mockk()
        // 3. Initialize the class under test with the mock
        repository = PrayerRepository(prayerCalculationService)
    }

    @Test
    fun `getPrayerTimes should call calculationService and return its result`() = runTest {
        // Arrange (Given)
        val testDate = LocalDate.of(2024, 1, 1)
        val testLatitude = 41.0
        val testLongitude = 29.0
        val mockPrayerList = listOf(
            Prayer(
                name = "Fajr",
                arabicName = "الفجر",
                time = LocalTime.of(5, 0),
                date = testDate,
                isCurrent = false,
                notificationEnabled = false,
                calculationMethod = CalculationMethod.TURKEY_DIYANET,
                adjustmentMinutes = 0
            )
        )

        // Stub the mock: When prayerCalculationService.calculatePrayerTimes is called with ANY arguments,
        // it should return our mockPrayerList.
        coEvery { prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any()) } returns mockPrayerList

        // Act (When)
        val result = repository.getPrayerTimes(testDate, testLatitude, testLongitude)

        // Assert (Then)
        // Verify that the service was called exactly once.
        coVerify(exactly = 1) { prayerCalculationService.calculateDailyPrayerTimes(testLatitude, testLongitude, testDate, any(), any()) }

        // Verify that the result from the repository is the same as the one we told the mock to return.
        assertThat(result).isEqualTo(mockPrayerList)
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Fajr")
    }
}