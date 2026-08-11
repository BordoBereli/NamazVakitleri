package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.data.prayer.PrayerRepository
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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
                name = "Fajr",
                arabicName = "الفجر",
                time = LocalTime.parse("05:00"),
                date = testDate.date,
                isCurrent = false,
                notificationEnabled = false
            )
        )

        // Stub the mock: When prayerDataStore.getPrayerTimes is called with ANY arguments,
        // it should return our mockPrayerList.
        coEvery { prayerDataStore.getPrayerTimes(any(), any(), any(), any()) } returns mockPrayerList

        // Act (When)
        val result = repository.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId)

        // Assert (Then)
        // Verify that the data store was called exactly once.
        coVerify(exactly = 1) { prayerDataStore.getPrayerTimes(testDate, testLatitude, testLongitude, zoneId) }

        // Verify that the result from the repository is the same as the one we told the mock to return.
        assertThat(result).isEqualTo(mockPrayerList)
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Fajr")
    }

    @Test
    fun `clearCache should delegate to the data store`() = runTest {
        coEvery { prayerDataStore.clearCache() } returns Unit

        repository.clearCache()

        coVerify(exactly = 1) { prayerDataStore.clearCache() }
    }
}