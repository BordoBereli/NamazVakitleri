package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

class GetPrayerTimesUseCaseTest {
    private lateinit var prayerRepository: IPrayerRepository
    private lateinit var useCase: GetPrayerTimesUseCase
    private lateinit var testDate: LocalDateTime
    private lateinit var zoneId: ZoneId

    @BeforeEach
    fun setUp() {
        prayerRepository = mockk()
        useCase = GetPrayerTimesUseCase(prayerRepository)
        testDate = LocalDateTime.createBy(2024, 1, 1)
        zoneId = ZoneId.systemDefault()
    }

    @Test
    fun `invoke should return Success Result when repository succeeds`() = runTest {
        // Arrange
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
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any(), zoneId) } returns mockPrayerList

        // Act
        val result = useCase(testDate, 41.0, 29.0, zoneId)

        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockPrayerList)
    }

    @Test
    fun `invoke should return Failure Result when repository throws exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Database error")
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any(), zoneId) } throws exception

        // Act
        val result = useCase(testDate, 41.0, 29.0, zoneId)

        // Assert
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}