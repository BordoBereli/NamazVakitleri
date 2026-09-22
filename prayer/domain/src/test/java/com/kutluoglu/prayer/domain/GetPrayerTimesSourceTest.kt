package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.ZoneId

class GetPrayerTimesSourceTest {

    private val useCase = mockk<GetPrayerTimesUseCase>()
    private val source = GetPrayerTimesSource(useCase)
    private val date = LocalDateTime(2026, 9, 4, 0, 0)
    private val zoneId = ZoneId.of("Europe/Istanbul")

    private val prayers = listOf(
        Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 4))
    )

    @Test
    fun `delegates to use case and returns its result`() = runTest {
        coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(prayers)

        val result = source.getDailyPrayerTimes(
            date, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, false
        )

        assertThat(result.getOrNull()).isEqualTo(prayers)
        coVerify(exactly = 1) {
            useCase.invoke(
                date, 41.0, 29.0, zoneId,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, false
            )
        }
    }

    @Test
    fun `forwards persistDailyCache to use case`() = runTest {
        coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(emptyList())

        source.getDailyPrayerTimes(
            date, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, true
        )

        coVerify(exactly = 1) {
            useCase.invoke(
                date, 41.0, 29.0, zoneId,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, true
            )
        }
    }

    @Test
    fun `propagates use case failure`() = runTest {
        val exception = RuntimeException("use case failed")
        coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(exception)

        val result = source.getDailyPrayerTimes(
            date, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, false
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}
