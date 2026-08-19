package com.kutluoglu.prayer_feature.prayertimes

import android.util.Log
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.getZoneIdFromLocation
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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

        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns success(mockPrayerList)
        coEvery { getMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns null
        coEvery { saveMonthlyPrayerTimesUseCase.invoke(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "TURKEY_DIYANET")
        every { settingsRepository.observeSettings() } returns flowOf(Settings())
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, null)
        every { formatter.withLocalizedNames(any()) } answers { firstArg() }
        every { formatter.getInitialTimeInfo(any(), any(), any()) } returns TimeUiState(
            gregorianDayAndName = "1 Monday",
            hijriDate = "1 Muharram 1448"
        )
        every { formatter.getInitialTimeInfo(any()) } returns TimeUiState(gregorianShortDate = "August 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"

        viewModel = PrayerTimesViewModel(
            getPrayerTimesUseCase,
            getMonthlyPrayerTimesUseCase,
            saveMonthlyPrayerTimesUseCase,
            activeLocationProvider,
            calculator,
            formatter,
            getSettingsUseCase,
            settingsRepository,
            UnconfinedTestDispatcher()
        )
        viewModelStore = ViewModelStore()
        viewModelStore.put("viewModel", viewModel)
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } returns success(mockPrayerList)

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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } coAnswers {
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
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any(), any()) } answers {
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
}
