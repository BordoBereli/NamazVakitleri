package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.data.source.prayer.PrayerDataStoreImp
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerDataStoreImpTest {

    @Test
    fun `getPrayerTimes delegates to calculation service and returns its result`() = runTest {
        // GIVEN a calculation service and a data store using it
        val prayerCalculationService = mockk<PrayerCalculationService>()
        val dataStore = PrayerDataStoreImp(prayerCalculationService)
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val mockPrayerList = listOf(
            Prayer(
                name = "Fajr",
                arabicName = "الفجر",
                time = LocalTime.parse("05:00"),
                date = testDate.date
            )
        )
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returns mockPrayerList

        // WHEN requesting prayer times
        val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, ZoneId.systemDefault())

        // THEN it delegates to the calculation service with the standard Turkey method
        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, any(), testDate,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        assertThat(result).isEqualTo(mockPrayerList)
    }
}
