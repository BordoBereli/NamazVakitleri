package com.kutluoglu.prayer.usecases.prayer

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

class SaveMonthlyPrayerTimesUseCaseTest {

    @Test
    fun `invoke persists the month via the repository`() = runTest {
        // GIVEN a repository
        val repository = mockk<IPrayerRepository>()
        coEvery { repository.saveMonthlyPrayerTimes(any(), any(), any(), any(), any(), any()) } returns Unit
        val useCase = SaveMonthlyPrayerTimesUseCase(repository)
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val monthToSave = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))
                )
            )
        )

        // WHEN saving the month
        useCase(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, monthToSave)

        // THEN the repository's saveMonthlyPrayerTimes is called exactly once
        coVerify(exactly = 1) {
            repository.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, monthToSave)
        }
    }
}
