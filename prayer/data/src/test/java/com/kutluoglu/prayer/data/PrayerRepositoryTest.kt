package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.data.prayer.PrayerRepository
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerRepositoryTest {

    // 1. Declare the dependencies and the class under test
    private lateinit var prayerDataStore: PrayerDataStore
    private lateinit var repository: PrayerRepository

    @BeforeEach
    fun setUp() {
        // 2. Create a mock of the dependency
        prayerDataStore = mockk()
        // 3. Initialize the class under test with the mock
        repository = PrayerRepository(prayerDataStore)
    }

    @Test
    fun `getPrayerTimes should call data store and return its result`() = runTest {
        // Arrange (Given)
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val testLatitude = 41.0
        val testLongitude = 29.0
        val zoneId = ZoneId.systemDefault()
        val mockPrayerList = listOf(
            Prayer(
                name = "Imsak",
                arabicName = "الإمساك",
                time = LocalTime.parse("05:00"),
                date = testDate.date,
                isCurrent = false,
                notificationEnabled = false,
                isImsak = true
            )
        )

        // Stub the mock: When prayerDataStore.getPrayerTimes is called with ANY arguments,
        // it should return our mockPrayerList.
        coEvery { prayerDataStore.getPrayerTimes(any(), any(), any(), any(), any(), any(), any()) } returns mockPrayerList

        // Act (When)
        val result = repository.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId, CalculationMethod.TURKEY_DIYANET)

        // Assert (Then)
        // Verify that the data store was called exactly once.
        coVerify(exactly = 1) { prayerDataStore.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, true) }

        // Verify that the result from the repository is the same as the one we told the mock to return.
        assertThat(result).isEqualTo(mockPrayerList)
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Imsak")
    }

    @Test
    fun `clearCache should delegate to the data store`() = runTest {
        coEvery { prayerDataStore.clearCache() } returns Unit

        repository.clearCache()

        coVerify(exactly = 1) { prayerDataStore.clearCache() }
    }

    @Test
    fun `getMonthlyPrayerTimes should call data store and return its result`() = runTest {
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val mockMonth = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Imsak", "الإمساك", LocalTime.parse("05:00"), LocalDate(2024, 1, 1), isImsak = true)
                )
            )
        )
        coEvery { prayerDataStore.getMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) } returns mockMonth

        val result = repository.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET)

        coVerify(exactly = 1) {
            prayerDataStore.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD)
        }
        assertThat(result).isEqualTo(mockMonth)
    }

    @Test
    fun `saveMonthlyPrayerTimes should delegate to the data store`() = runTest {
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val monthToSave = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Imsak", "الإمساك", LocalTime.parse("05:00"), LocalDate(2024, 1, 1), isImsak = true)
                )
            )
        )
        coEvery { prayerDataStore.saveMonthlyPrayerTimes(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        repository.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, monthToSave)

        coVerify(exactly = 1) {
            prayerDataStore.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, monthToSave)
        }
    }
}