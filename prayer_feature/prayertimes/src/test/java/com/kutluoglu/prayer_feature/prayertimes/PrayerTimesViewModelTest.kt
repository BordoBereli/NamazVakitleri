package com.kutluoglu.prayer_feature.prayertimes

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class PrayerTimesViewModelTest {

    private lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase
    private lateinit var activeLocationProvider: ActiveLocationProvider
    private lateinit var calculator: PrayerLogicEngine
    private lateinit var formatter: PrayerFormatter
    private lateinit var viewModel: PrayerTimesViewModel

    private val mockLocation = LocationData(
        latitude = 41.0082,
        longitude = 28.9784,
        country = "Turkey",
        countryCode = "TR",
        city = "Istanbul",
        county = null
    )

    private val mockPrayerList = listOf(
        Prayer(name = "Fajr", arabicName = "الفجر", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0

        getPrayerTimesUseCase = mockk()
        activeLocationProvider = ActiveLocationProvider()
        activeLocationProvider.set(mockLocation)
        calculator = mockk(relaxed = true)
        formatter = mockk(relaxed = true)

        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns success(mockPrayerList)
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, null)
        every { formatter.withLocalizedNames(any()) } returns mockPrayerList
        every { formatter.getInitialTimeInfo(any(), any(), any()) } returns TimeUiState(
            gregorianDayAndName = "1 Monday",
            hijriDate = "1 Muharram 1448"
        )
        every { formatter.getInitialTimeInfo(any()) } returns TimeUiState(gregorianShortDate = "August 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            activeLocationProvider,
            calculator,
            formatter
        )
    }

    @Test
    fun `initial load computes the current month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            assertThat(success.selectedMonth).isEqualTo(currentMonth)
            assertThat(success.isCurrentMonth).isTrue()
            assertThat(success.monthlyPrayers.size).isEqualTo(currentMonth.numberOfDays)
            assertThat(success.currentDayOfMonth).isEqualTo(LocalDateTime.now(zoneId).day)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNextMonth loads the adjacent month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
            assertThat(success.selectedMonth).isEqualTo(nextMonth)
            assertThat(success.isCurrentMonth).isFalse()
            assertThat(success.monthlyPrayers.size).isEqualTo(nextMonth.numberOfDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPreviousMonth loads the adjacent month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnPreviousMonth)

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            val previousMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
            assertThat(success.selectedMonth).isEqualTo(previousMonth)
            assertThat(success.isCurrentMonth).isFalse()
            assertThat(success.monthlyPrayers.size).isEqualTo(previousMonth.numberOfDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToday returns to the current month`() = runTest {
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)
        viewModel.onEvent(PrayerTimesEvent.OnToday)

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            assertThat(success.selectedMonth).isEqualTo(currentMonth)
            assertThat(success.isCurrentMonth).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `revisiting a cached month does not recompute`() = runTest {
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)
        viewModel.onEvent(PrayerTimesEvent.OnToday)

        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
        val expectedCalls = currentMonth.numberOfDays + nextMonth.numberOfDays
        assertThat(callCount).isEqualTo(expectedCalls)
    }

    @Test
    fun `navigation during an in-flight load is not dropped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } coAnswers {
            callCount++
            if (callCount == 1) gate.await()
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)

        gate.complete(Unit)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            val zoneId = getZoneIdFromLocation("TR")
            val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
            val secondNextMonth = currentMonth.plus(2, DateTimeUnit.MONTH)
            assertThat(success.selectedMonth).isEqualTo(secondNextMonth)
            assertThat(success.isCurrentMonth).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `month cache is keyed per location`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()
        val callsForA = callCount

        activeLocationProvider.set(locB)
        viewModel.loadMonthlyPrayerTimes()
        val callsForB = callCount

        // Switching locations reloads (not served from locA's cache)
        assertThat(callsForB).isGreaterThan(callsForA)

        // Switching back to locA serves from its cache (no new calls)
        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()
        assertThat(callCount).isEqualTo(callsForB)
    }
}
