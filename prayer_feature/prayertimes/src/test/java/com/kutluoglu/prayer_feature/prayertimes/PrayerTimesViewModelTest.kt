package com.kutluoglu.prayer_feature.prayertimes

import android.util.Log
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsEvents
import com.kutluoglu.core.common.analytics.AnalyticsParams
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.core.common.gregorianDayAndNameFormatter
import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetMonthlyPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.prayer.SaveMonthlyPrayerTimesUseCase
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.yearMonth
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Result.Companion.success

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class PrayerTimesViewModelTest {

    private lateinit var getPrayerTimesUseCase: GetPrayerTimesUseCase
    private lateinit var getMonthlyPrayerTimesUseCase: GetMonthlyPrayerTimesUseCase
    private lateinit var saveMonthlyPrayerTimesUseCase: SaveMonthlyPrayerTimesUseCase
    private lateinit var activeLocationProvider: ActiveLocationProvider
    private lateinit var calculator: PrayerLogicEngine
    private lateinit var formatter: PrayerFormatter
    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var settingsRepository: SettingsRepository
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val backgroundSaveScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
    private lateinit var viewModel: PrayerTimesViewModel
    private lateinit var viewModelStore: ViewModelStore

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
        getMonthlyPrayerTimesUseCase = mockk()
        saveMonthlyPrayerTimesUseCase = mockk()
        activeLocationProvider = ActiveLocationProvider()
        activeLocationProvider.set(mockLocation)
        calculator = mockk(relaxed = true)
        formatter = mockk(relaxed = true)
        getSettingsUseCase = mockk()
        settingsRepository = mockk()

        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns success(mockPrayerList)
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns null
        coEvery { saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        every { settingsRepository.observeSettings() } returns flowOf(Settings())
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, null)
        every { formatter.withLocalizedNames(any()) } answers { firstArg() }
        every { formatter.getInitialTimeInfo(any(), any(), any(), any()) } returns TimeUiState(
            gregorianDayAndName = "1 Monday",
            hijriDate = "1 Muharram 1448"
        )
        every { formatter.getInitialTimeInfo(any(), any()) } returns TimeUiState(gregorianShortDate = "August 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        buildViewModel()
        viewModelStore = ViewModelStore()
        viewModelStore.put("viewModel", viewModel)
    }

    private fun buildViewModel(locationResolutionTimeoutMs: Long = 15_000L) {
        viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            getMonthlyPrayerTimesUseCase,
            saveMonthlyPrayerTimesUseCase,
            activeLocationProvider,
            calculator,
            formatter,
            getSettingsUseCase,
            settingsRepository,
            analyticsTracker,
            backgroundSaveScope,
            UnconfinedTestDispatcher(),
            locationResolutionTimeoutMs
        )
    }

    @AfterEach
    fun tearDown() {
        viewModelStore.clear()
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
    fun `month load requests daily times without persisting per-day cache`() = runTest {
        viewModel.loadMonthlyPrayerTimes()

        coVerify(atLeast = currentMonthDays()) {
            getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), false)
        }
    }

    private fun currentMonthDays(): Int {
        val zoneId = getZoneIdFromLocation("TR")
        return LocalDateTime.now(zoneId).date.yearMonth.numberOfDays
    }

    @Test
    fun `monthly save runs in background after success is emitted`() = runTest {
        val saveGate = CompletableDeferred<Unit>()
        coEvery {
            saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
        } coAnswers { saveGate.await(); Unit }

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            var state = withTimeout(5_000) { awaitItem() }
            while (state is PrayerTimesUiState.Loading) {
                state = withTimeout(5_000) { awaitItem() }
            }
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)

            assertThat(saveGate.isCompleted).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        saveGate.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `null location stays Loading and errors only after timeout`() = runTest {
        activeLocationProvider.set(null)
        buildViewModel(locationResolutionTimeoutMs = 100)

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(PrayerTimesUiState.Loading)
            val state = withTimeout(5_000) { awaitItem() }
            assertThat(state).isInstanceOf(PrayerTimesUiState.Error::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) {
            analyticsTracker.logEvent(
                AnalyticsEvents.PRAYER_TIMES_ERROR,
                mapOf(AnalyticsParams.REASON to "no_active_location")
            )
        }
    }

    @Test
    fun `location arriving before timeout cancels the pending error`() = runTest {
        activeLocationProvider.set(null)
        buildViewModel(locationResolutionTimeoutMs = 10_000)

        viewModel.loadMonthlyPrayerTimes()
        activeLocationProvider.set(mockLocation)

        viewModel.uiState.test {
            var state = withTimeout(5_000) { awaitItem() }
            while (state is PrayerTimesUiState.Loading) {
                state = withTimeout(5_000) { awaitItem() }
            }
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            delay(200)
            assertThat(viewModel.uiState.value).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) {
            analyticsTracker.logEvent(
                AnalyticsEvents.PRAYER_TIMES_ERROR,
                mapOf(AnalyticsParams.REASON to "no_active_location")
            )
        }
    }

    @Test
    fun `cold load streams today first then completes full sorted month`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val today = LocalDateTime.now(zoneId).date.dayOfMonth
        val days = currentMonthDays()
        val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()
        coEvery {
            getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
        } coAnswers {
            val date = firstArg<LocalDateTime>()
            gates.getOrPut(date.date.dayOfMonth) { CompletableDeferred() }.await()
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(PrayerTimesUiState.Loading)

            gates.getValue(today).complete(Unit)
            var todayOnly = withTimeout(5_000) { awaitItem() }
            while ((todayOnly as? PrayerTimesUiState.Success)?.monthlyPrayers?.size != 1) {
                todayOnly = withTimeout(5_000) { awaitItem() }
            }
            assertThat(todayOnly.monthlyPrayers.map { it.dayOfMonth }).containsExactly(today)

            gates.filterKeys { it != today }.values.forEach { it.complete(Unit) }

            var final = withTimeout(5_000) { awaitItem() }
            while ((final as? PrayerTimesUiState.Success)?.monthlyPrayers?.size != days) {
                final = withTimeout(5_000) { awaitItem() }
            }
            assertThat(final.monthlyPrayers.map { it.dayOfMonth })
                .isEqualTo((1..days).toList())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failing monthly save keeps success state and logs analytics`() = runTest {
        coEvery {
            saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any())
        } throws RuntimeException("disk full")

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            var state = withTimeout(5_000) { awaitItem() }
            while (state is PrayerTimesUiState.Loading) {
                state = withTimeout(5_000) { awaitItem() }
            }
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        coVerify(exactly = 1) {
            analyticsTracker.logEvent(
                AnalyticsEvents.PRAYER_TIMES_ERROR,
                mapOf(AnalyticsParams.REASON to "monthly_save_failed")
            )
        }
    }

    @Test
    fun `loads month with hijri adjustment from settings`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET", hijriAdjustment = 5)

        viewModel.loadMonthlyPrayerTimes()

        verify(atLeast = 1) { formatter.getInitialTimeInfo(any(), any(), any(), 5) }
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
    fun `navigating to a shorter month when today is the 31st does not throw`() = runTest {
        mockkStatic("com.kutluoglu.core.common.LocalDateTimeExtKt")
        try {
            every { LocalDateTime.now(any()) } returns LocalDateTime(2026, 8, 31, 12, 0)

            viewModel.loadMonthlyPrayerTimes()
            viewModel.onEvent(PrayerTimesEvent.OnNextMonth)

            viewModel.uiState.test {
                var state = withTimeout(5_000) { awaitItem() }
                while (state is PrayerTimesUiState.Loading) {
                    state = withTimeout(5_000) { awaitItem() }
                }
                assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
                val success = state as PrayerTimesUiState.Success
                assertThat(success.selectedMonth).isEqualTo(YearMonth(2026, 9))
                assertThat(success.monthlyPrayers.size).isEqualTo(30)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            unmockkStatic("com.kutluoglu.core.common.LocalDateTimeExtKt")
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } answers {
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
    fun `loads month from persistent cache without per-day fetches`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val cachedMonth = (1..currentMonth.numberOfDays).map { day ->
            DailyPrayer(
                dayOfMonth = day,
                gregorianDate = "$day Monday",
                hijriDate = "$day Muharram 1448",
                prayers = mockPrayerList
            )
        }
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns cachedMonth
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            assertThat(success.monthlyPrayers.size).isEqualTo(currentMonth.numberOfDays)
            assertThat(success.monthlyPrayers.first().dayOfMonth).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `persisted month hijri dates are recomputed with the adjustment`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val cachedMonth = (1..currentMonth.numberOfDays).map { day ->
            DailyPrayer(
                dayOfMonth = day,
                gregorianDate = "$day Monday",
                hijriDate = "01 Muharram 1448",
                prayers = mockPrayerList
            )
        }
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns cachedMonth
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET", hijriAdjustment = 5)
        every { formatter.formatHijriDate(any(), any()) } returns "06 Muharram 1448"

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = state as PrayerTimesUiState.Success
            assertThat(success.monthlyPrayers.first().hijriDate).isEqualTo("06 Muharram 1448")
            verify(atLeast = 1) { formatter.formatHijriDate(any(), 5) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing hijri adjustment reloads the month`() = runTest {
        val settingsFlow = MutableStateFlow(Settings(calculationMethod = "TURKEY_DIYANET", hijriAdjustment = 0))
        every { settingsRepository.observeSettings() } returns settingsFlow
        var loadCount = 0
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
            loadCount++
            null
        }

        viewModel.loadMonthlyPrayerTimes()
        settingsFlow.value = Settings(calculationMethod = "TURKEY_DIYANET", hijriAdjustment = 3)
        runCurrent()

        assertThat(loadCount).isGreaterThan(1)
    }

    @Test
    fun `saves month to persistent cache after per-day computation`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns null

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) {
            saveMonthlyPrayerTimesUseCase.invoke(
                currentMonth,
                mockLocation.latitude,
                mockLocation.longitude,
                zoneId,
                CalculationMethod.TURKEY_DIYANET,
                any()
            )
        }
    }

    @Test
    fun `navigation during an in-flight load is not dropped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } answers {
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

    @Test
    fun `month position is remembered per location`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns success(mockPrayerList)

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()
        viewModel.onEvent(PrayerTimesEvent.OnNextMonth)

        activeLocationProvider.set(locB)
        viewModel.loadMonthlyPrayerTimes()

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()

        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            assertThat(success.selectedMonth).isEqualTo(nextMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `location switch during in-flight load does not emit stale pairing`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val prayerListB = mockPrayerList.map { it.copy(name = "${it.name}B") }
        val gate = CompletableDeferred<Unit>()
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } coAnswers {
            callCount++
            val latitude = arg<Double>(1)
            if (callCount == 1) gate.await()
            if (latitude == locA.latitude) success(mockPrayerList) else success(prayerListB)
        }

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()
        activeLocationProvider.set(locB)
        viewModel.loadMonthlyPrayerTimes()
        gate.complete(Unit)

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            assertThat(success.locationState.locationData).isEqualTo(locB)
            assertThat(success.monthlyPrayers.first().prayers.first().name).isEqualTo("FajrB")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `location change emits Loading before new data`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val gate = CompletableDeferred<Unit>()
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } coAnswers {
            val latitude = arg<Double>(1)
            if (latitude == locB.latitude) gate.await()
            success(mockPrayerList)
        }

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val first = awaitItem()
            assertThat(first).isInstanceOf(PrayerTimesUiState.Success::class.java)

            activeLocationProvider.set(locB)
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(PrayerTimesUiState.Loading::class.java)

            gate.complete(Unit)
            val success = awaitItem()
            assertThat(success).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val successState = success as PrayerTimesUiState.Success
            assertThat(successState.locationState.locationData).isEqualTo(locB)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `location change triggers reload without manual call`() = runTest {
        val locA = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        val locB = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null)
        val prayerListB = mockPrayerList.map { it.copy(name = "${it.name}B") }
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } coAnswers {
            val latitude = arg<Double>(1)
            if (latitude == locB.latitude) success(prayerListB) else success(mockPrayerList)
        }

        activeLocationProvider.set(locA)
        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val first = awaitItem()
            assertThat(first).isInstanceOf(PrayerTimesUiState.Success::class.java)

            activeLocationProvider.set(locB)
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(PrayerTimesUiState.Loading::class.java)

            val second = awaitItem()
            assertThat(second).isInstanceOf(PrayerTimesUiState.Success::class.java)
            val success = second as PrayerTimesUiState.Success
            assertThat(success.locationState.locationData).isEqualTo(locB)
            assertThat(success.monthlyPrayers.first().prayers.first().name).isEqualTo("FajrB")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `per-day computation runs concurrently`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val active = AtomicInteger(0)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } coAnswers {
            val now = active.incrementAndGet()
            maxConcurrent.updateAndGet { maxOf(it, now) }
            started.incrementAndGet()
            gate.await()
            active.decrementAndGet()
            success(mockPrayerList)
        }

        val store = ViewModelStore()
        val viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            getMonthlyPrayerTimesUseCase,
            saveMonthlyPrayerTimesUseCase,
            activeLocationProvider,
            calculator,
            formatter,
            getSettingsUseCase,
            settingsRepository,
            analyticsTracker,
            backgroundSaveScope,
            Dispatchers.Default
        )
        store.put("viewModel", viewModel)
        viewModel.loadMonthlyPrayerTimes()

        val zoneId = getZoneIdFromLocation("TR")
        val days = LocalDateTime.now(zoneId).date.yearMonth.numberOfDays
        try {
            withTimeout(5_000) {
                while (started.get() < days) {
                    delay(10)
                }
            }
        } finally {
            gate.complete(Unit)
            store.clear()
        }

        assertThat(maxConcurrent.get()).isGreaterThan(1)
    }

    @Test
    fun `calculation method change clears month cache and reloads`() = runTest {
        val settingsFlow = MutableStateFlow(Settings(calculationMethod = "TURKEY_DIYANET"))
        every { settingsRepository.observeSettings() } returns settingsFlow
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        val callsAfterInitial = callCount

        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "MWL")
        settingsFlow.value = Settings(calculationMethod = "MWL")

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(callCount).isGreaterThan(callsAfterInitial)
    }

    @Test
    fun `language change clears month cache and reloads`() = runTest {
        val settingsFlow = MutableStateFlow(Settings(language = "system"))
        every { settingsRepository.observeSettings() } returns settingsFlow
        var callCount = 0
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } answers {
            callCount++
            success(mockPrayerList)
        }

        viewModel.loadMonthlyPrayerTimes()
        val callsAfterInitial = callCount

        coEvery { getSettingsUseCase() } returns Settings(language = "en")
        settingsFlow.value = Settings(language = "en")

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(PrayerTimesUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(callCount).isGreaterThan(callsAfterInitial)
    }

    @Test
    fun `persisted month prayer names are re-localized on load`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val cachedMonth = (1..currentMonth.numberOfDays).map { day ->
            DailyPrayer(
                dayOfMonth = day,
                gregorianDate = "$day Monday",
                hijriDate = "$day Muharram 1448",
                prayers = mockPrayerList
            )
        }
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns cachedMonth
        val localized = mockPrayerList.map { it.copy(name = "${it.name}L") }
        every { formatter.withLocalizedNames(any()) } returns localized

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            assertThat(success.monthlyPrayers.first().prayers.first().name).isEqualTo("FajrL")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `persisted month gregorian dates are re-formatted on load`() = runTest {
        val zoneId = getZoneIdFromLocation("TR")
        val currentMonth = LocalDateTime.now(zoneId).date.yearMonth
        val cachedMonth = (1..currentMonth.numberOfDays).map { day ->
            DailyPrayer(
                dayOfMonth = day,
                gregorianDate = "stale date",
                hijriDate = "$day Muharram 1448",
                prayers = mockPrayerList
            )
        }
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns cachedMonth

        viewModel.loadMonthlyPrayerTimes()

        viewModel.uiState.test {
            val state = awaitItem()
            val success = state as PrayerTimesUiState.Success
            val expected = currentMonth.onDay(1).toJavaLocalDate().format(gregorianDayAndNameFormatter())
            assertThat(success.monthlyPrayers.first().gregorianDate).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
