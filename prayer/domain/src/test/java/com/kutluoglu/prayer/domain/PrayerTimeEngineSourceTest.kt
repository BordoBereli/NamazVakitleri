package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerTimeEngineSourceTest {

    private val engine = mockk<PrayerTimeEngine>()
    private val source = PrayerTimeEngineSource(engine)
    private val date = LocalDateTime(2026, 9, 4, 0, 0)
    private val zoneId = ZoneId.of("Europe/Istanbul")

    private val prayers = listOf(
        Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 4))
    )

    @Test
    fun `wraps engine result in success`() = runTest {
        every {
            engine.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returns prayers

        val result = source.getDailyPrayerTimes(
            date, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, false
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(prayers)
    }

    @Test
    fun `propagates engine exception as failure`() = runTest {
        val exception = RuntimeException("engine failed")
        every {
            engine.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } throws exception

        val result = source.getDailyPrayerTimes(
            date, 41.0, 29.0, zoneId,
            CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, false
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}
