package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DailyPrayerTimesLoaderTest {

    private val zoneId = ZoneId.of("Europe/Istanbul")
    private val date = LocalDateTime(2026, 9, 4, 0, 0)

    private val prayers = listOf(
        Prayer("Imsak", "الإمساك", LocalTime.parse("04:50"), LocalDate(2026, 9, 4), isImsak = true),
        Prayer("Sunrise", "الشروق", LocalTime.parse("06:00"), LocalDate(2026, 9, 4)),
        Prayer("Dhuhr", "الظهر", LocalTime.parse("13:00"), LocalDate(2026, 9, 4)),
        Prayer("Asr", "العصر", LocalTime.parse("16:30"), LocalDate(2026, 9, 4)),
        Prayer("Maghrib", "المغرب", LocalTime.parse("19:00"), LocalDate(2026, 9, 4)),
        Prayer("Isha", "العشاء", LocalTime.parse("20:30"), LocalDate(2026, 9, 4))
    )

    private fun loaderAt(utcInstant: String, source: DailyPrayerTimesSource): DailyPrayerTimesLoader {
        val logicEngine = PrayerLogicEngine(
            Clock.fixed(Instant.parse(utcInstant), ZoneId.of("UTC"))
        )
        return DailyPrayerTimesLoader(source, logicEngine)
    }

    private class FakeSource(
        var result: Result<List<Prayer>>,
        var receivedPersistDailyCache: Boolean? = null
    ) : DailyPrayerTimesSource {
        override suspend fun getDailyPrayerTimes(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod,
            juristicMethod: JuristicMethod,
            persistDailyCache: Boolean
        ): Result<List<Prayer>> {
            receivedPersistDailyCache = persistDailyCache
            return result
        }
    }

    @Test
    fun `load maps prayers to current and next prayer`() = runTest {
        // 12:00 UTC = 15:00 Istanbul -> current Dhuhr, next Asr
        val source = FakeSource(Result.success(prayers))
        val loader = loaderAt("2026-09-04T12:00:00Z", source)

        val result = loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD)

        assertThat(result.isSuccess).isTrue()
        val daily = result.getOrThrow()
        assertThat(daily.prayers).isEqualTo(prayers)
        assertThat(daily.currentPrayer?.name).isEqualTo("Dhuhr")
        assertThat(daily.nextPrayer?.name).isEqualTo("Asr")
    }

    @Test
    fun `load computes current and next prayer epoch millis`() = runTest {
        val source = FakeSource(Result.success(prayers))
        val loader = loaderAt("2026-09-04T12:00:00Z", source)

        val daily = loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD).getOrThrow()

        // Dhuhr 13:00 +03:00 = 10:00 UTC
        assertThat(daily.currentPrayerEpochMillis)
            .isEqualTo(Instant.parse("2026-09-04T10:00:00Z").toEpochMilli())
        // Asr 16:30 +03:00 = 13:30 UTC
        assertThat(daily.nextPrayerEpochMillis)
            .isEqualTo(Instant.parse("2026-09-04T13:30:00Z").toEpochMilli())
    }

    @Test
    fun `load marks jumuah when next prayer is friday dhuhr`() = runTest {
        // 07:00 UTC = 10:00 Istanbul -> current Sunrise, next Dhuhr (Friday)
        val source = FakeSource(Result.success(prayers))
        val loader = loaderAt("2026-09-04T07:00:00Z", source)

        val daily = loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD).getOrThrow()

        assertThat(daily.nextPrayer?.name).isEqualTo("Dhuhr")
        assertThat(daily.isJumuah).isTrue()
    }

    @Test
    fun `load forwards persistDailyCache to source`() = runTest {
        val source = FakeSource(Result.success(prayers))
        val loader = loaderAt("2026-09-04T12:00:00Z", source)

        loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD, persistDailyCache = false)

        assertThat(source.receivedPersistDailyCache).isFalse()
    }

    @Test
    fun `load propagates source failure`() = runTest {
        val exception = RuntimeException("source failed")
        val source = FakeSource(Result.failure(exception))
        val loader = loaderAt("2026-09-04T12:00:00Z", source)

        val result = loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `load returns zero epoch millis and isJumuah false when next is null`() = runTest {
        val source = FakeSource(Result.success(emptyList()))
        val loader = loaderAt("2026-09-04T12:00:00Z", source)

        val daily = loader.load(date, 41.0, 29.0, zoneId, CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD).getOrThrow()

        assertThat(daily.currentPrayer).isNull()
        assertThat(daily.nextPrayer).isNull()
        assertThat(daily.currentPrayerEpochMillis).isEqualTo(0L)
        assertThat(daily.nextPrayerEpochMillis).isEqualTo(0L)
        assertThat(daily.isJumuah).isFalse()
    }
}
