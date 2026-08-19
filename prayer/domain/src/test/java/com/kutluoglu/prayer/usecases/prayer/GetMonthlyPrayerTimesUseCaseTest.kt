package com.kutluoglu.prayer.usecases.prayer

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.jupiter.api.Test
import java.time.ZoneId

class GetMonthlyPrayerTimesUseCaseTest {

    @Test
    fun `invoke returns the cached month from the repository`() = runTest {
        // GIVEN a repository with a cached month
        val repository = mockk<IPrayerRepository>()
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val cachedMonth = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))
                )
            )
        )
        coEvery { repository.getMonthlyPrayerTimes(any(), any(), any(), any(), any()) } returns cachedMonth
        val useCase = GetMonthlyPrayerTimesUseCase(repository)

        // WHEN requesting the cached month
        val result = useCase(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET)

        // THEN the repository's cached month is returned
        assertThat(result).isEqualTo(cachedMonth)
        coVerify(exactly = 1) {
            repository.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET)
        }
    }

    @Test
    fun `invoke returns null when the month is not cached`() = runTest {
        // GIVEN a repository without a cached month
        val repository = mockk<IPrayerRepository>()
        coEvery { repository.getMonthlyPrayerTimes(any(), any(), any(), any(), any()) } returns null
        val useCase = GetMonthlyPrayerTimesUseCase(repository)

        // WHEN requesting the cached month
        val result = useCase(YearMonth(2024, 1), 41.0, 29.0, ZoneId.of("Europe/Istanbul"))

        // THEN null is returned
        assertThat(result).isNull()
    }
}
