package com.kutluoglu.prayer_feature.home.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.prayerUtils.PrayerFormatter
import com.kutluoglu.core.designsystem.states.TimeUiState
import com.kutluoglu.prayer.domain.DailyPrayerTimes
import com.kutluoglu.prayer.domain.DailyPrayerTimesLoader
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.resolveZoneId
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.Result.Companion.success

class PrayerTimesLoaderTest {

    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = mockk()
    private val dailyLoader: DailyPrayerTimesLoader = mockk(relaxed = true)
    private val calculator: PrayerLogicEngine = mockk(relaxed = true)
    private val formatter: PrayerFormatter = mockk(relaxed = true)

    private val location = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-08-02T12:00:00Z"),
        ZoneId.of("Europe/Istanbul")
    )

    @Test
    fun `load builds prayerState timeState locationState on success`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns success(
            DailyPrayerTimes(
                prayers = listOf(fajr, dhuhr),
                currentPrayer = fajr,
                nextPrayer = dhuhr,
                currentPrayerEpochMillis = 0L,
                nextPrayerEpochMillis = 0L,
                isJumuah = false
            )
        )
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns success(emptyList())
        every { formatter.withLocalizedNames(any()) } returns listOf(fajr, dhuhr)
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns TimeUiState(gregorianFullDate = "02 Ağustos 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        val result = loader.load(location, CalculationMethod.TURKEY_DIYANET)

        assertThat(result.isSuccess).isTrue()
        val loaded = result.getOrThrow()
        assertThat(loaded.prayerState.currentPrayer).isEqualTo(fajr)
        assertThat(loaded.prayerState.nextPrayer).isEqualTo(dhuhr)
        assertThat(loaded.prayerState.prayers[0].isCurrent).isTrue()
        assertThat(loaded.timeState.gregorianFullDate).isEqualTo("02 Ağustos 2026")
        assertThat(loaded.locationState.locationInfoText).isEqualTo("Istanbul, TR")
    }

    @Test
    fun `load maps current and next to localized prayers when names diverge`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val rawFajr = Prayer(name = "Imsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val rawDhuhr = Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        val localizedFajr = rawFajr.copy(name = "İmsak")
        val localizedDhuhr = rawDhuhr.copy(name = "Öğle")

        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns success(
            DailyPrayerTimes(
                prayers = listOf(rawFajr, rawDhuhr),
                currentPrayer = rawFajr,
                nextPrayer = rawDhuhr,
                currentPrayerEpochMillis = 0L,
                nextPrayerEpochMillis = 0L,
                isJumuah = false
            )
        )
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns success(emptyList())
        every { formatter.withLocalizedNames(any()) } returns listOf(localizedFajr, localizedDhuhr)
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns TimeUiState()
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        val loaded = loader.load(location, CalculationMethod.TURKEY_DIYANET).getOrThrow()

        assertThat(loaded.prayerState.currentPrayer?.name).isEqualTo("İmsak")
        assertThat(loaded.prayerState.nextPrayer?.name).isEqualTo("Öğle")
        assertThat(loaded.prayerState.prayers[0].isCurrent).isTrue()
        assertThat(loaded.prayerState.prayers[1].isCurrent).isFalse()
    }

    @Test
    fun `load maps failure to a failed Result`() = runTest {
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("fetch failed"))

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        val result = loader.load(location, CalculationMethod.TURKEY_DIYANET)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("fetch failed")
    }

    @Test
    fun `load passes hijri adjustment to formatter`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns success(
            DailyPrayerTimes(
                prayers = listOf(fajr),
                currentPrayer = fajr,
                nextPrayer = null,
                currentPrayerEpochMillis = 0L,
                nextPrayerEpochMillis = 0L,
                isJumuah = false
            )
        )
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns success(emptyList())
        every { formatter.withLocalizedNames(any()) } returns listOf(fajr)
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        loader.load(location, CalculationMethod.TURKEY_DIYANET, hijriAdjustment = 7)

        verify { formatter.getInitialTimeInfo(any(), any(), any(), 7) }
    }

    @Test
    fun `computePrayerState marks only the current prayer as isCurrent`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(dhuhr, null)

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        val zoneId = resolveZoneId(location)
        val state = loader.computePrayerState(listOf(fajr, dhuhr), zoneId)

        assertThat(state.prayers[0].isCurrent).isFalse()
        assertThat(state.prayers[1].isCurrent).isTrue()
        assertThat(state.currentPrayer).isEqualTo(dhuhr)
    }

    @Test
    fun `load resolves next imsak time from tomorrow prayers`() = runTest {
        val today = LocalDate(2026, 8, 2)
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val todayImsak = Prayer(name = "İmsak", arabicName = "الإمساك", time = LocalTime(4, 50), date = today, isImsak = true)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = today)
        val tomorrowImsak = Prayer(name = "İmsak", arabicName = "الإمساك", time = LocalTime(4, 49), date = tomorrow, isImsak = true)

        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns success(
            DailyPrayerTimes(
                prayers = listOf(todayImsak, dhuhr),
                currentPrayer = todayImsak,
                nextPrayer = dhuhr,
                currentPrayerEpochMillis = 0L,
                nextPrayerEpochMillis = 0L,
                isJumuah = false
            )
        )
        coEvery {
            getPrayerTimesUseCase.invoke(
                match<LocalDateTime> { it.date == tomorrow },
                any(), any(), any(), any(), any(), any()
            )
        } returns success(listOf(tomorrowImsak))

        every { formatter.withLocalizedNames(any()) } returns listOf(todayImsak, dhuhr)
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns TimeUiState()
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter, fixedClock)
        val loaded = loader.load(location, CalculationMethod.TURKEY_DIYANET).getOrThrow()

        assertThat(loaded.nextImsakTime).isEqualTo(LocalTime(4, 49))
    }

    @Test
    fun `load resolves zone from stored timeZoneId over country heuristic`() = runTest {
        val usLocation = location.copy(
            countryCode = "US",
            timeZoneId = "America/Los_Angeles"
        )
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        coEvery { dailyLoader.load(any(), any(), any(), any(), any(), any(), any()) } returns success(
            DailyPrayerTimes(
                prayers = listOf(fajr),
                currentPrayer = fajr,
                nextPrayer = null,
                currentPrayerEpochMillis = 0L,
                nextPrayerEpochMillis = 0L,
                isJumuah = false
            )
        )
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns success(emptyList())
        every { formatter.withLocalizedNames(any()) } returns listOf(fajr)
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns TimeUiState()
        every { formatter.locationInfo(any()) } returns "Los Angeles, US"

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, dailyLoader, calculator, formatter)
        loader.load(usLocation, CalculationMethod.TURKEY_DIYANET)

        coVerify {
            dailyLoader.load(
                any(),
                any(),
                any(),
                eq(ZoneId.of("America/Los_Angeles")),
                any(),
                any(),
                any()
            )
        }
    }
}
