package com.kutluoglu.prayer_feature.home.domain

import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PrayerTimesLoaderJuristicTest {

    private val useCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
    private val calculator = mockk<PrayerLogicEngine>(relaxed = true)
    private val formatter = mockk<PrayerFormatter>(relaxed = true)

    @Test
    fun `load forwards juristic method to use case`() = runTest {
        coEvery { useCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(emptyList())
        every { formatter.withLocalizedNames(any()) } returns emptyList()
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, null)
        val loader = PrayerTimesLoader(useCase, calculator, formatter)
        loader.load(
            location = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null),
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.HANAFI
        )
        coVerify { useCase.invoke(any(), any(), any(), any(), any(), juristicMethod = JuristicMethod.HANAFI, any()) }
    }
}
