package com.kutluoglu.prayer_feature.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.usecases.prayer.GetMonthlyPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer.usecases.prayer.SaveMonthlyPrayerTimesUseCase
import com.kutluoglu.prayer_location.ActiveLocationProvider
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.yearMonth
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named
import java.time.ZoneId
import java.time.chrono.HijrahDate

private const val LOCATION_RESOLUTION_TIMEOUT_MS = 15_000L

@KoinViewModel
class PrayerTimesViewModel(
        private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
        private val getMonthlyPrayerTimesUseCase: GetMonthlyPrayerTimesUseCase,
        private val saveMonthlyPrayerTimesUseCase: SaveMonthlyPrayerTimesUseCase,
        private val activeLocationProvider: ActiveLocationProvider,
        private val calculator: PrayerLogicEngine,
        private val formatter: PrayerFormatter,
        private val getSettingsUseCase: GetSettingsUseCase,
        private val settingsRepository: SettingsRepository,
        private val analyticsTracker: AnalyticsTracker,
        @Named("prayerSaveScope") private val backgroundSaveScope: CoroutineScope,
        private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
        private val locationResolutionTimeoutMs: Long = LOCATION_RESOLUTION_TIMEOUT_MS
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerTimesUiState>(PrayerTimesUiState.Loading)
    val uiState: StateFlow<PrayerTimesUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrayerTimesUiState.Loading
        )

    private var activeLocationId: String? = null
    private var zoneId: ZoneId? = null
    private val monthCache = mutableMapOf<String, MutableMap<YearMonth, List<DailyPrayer>>>()
    private val selectedMonthByLocation = mutableMapOf<String, YearMonth>()
    private var isLoading = false
    private var pendingMonth: YearMonth? = null
    private var locationObservationJob: Job? = null
    private var settingsObserverJob: Job? = null
    private var locationTimeoutJob: Job? = null

    fun loadMonthlyPrayerTimes() {
        if (locationObservationJob?.isActive == true) return
        locationObservationJob = viewModelScope.launch {
            activeLocationProvider.location
                .collect { location ->
                    if (location == null) {
                        _uiState.value = PrayerTimesUiState.Loading
                        locationTimeoutJob?.cancel()
                        locationTimeoutJob = viewModelScope.launch {
                            delay(locationResolutionTimeoutMs)
                            _uiState.value =
                                PrayerTimesUiState.Error("Failed to get active location.")
                            analyticsTracker.logEvent(
                                AnalyticsEvents.PRAYER_TIMES_ERROR,
                                mapOf(AnalyticsParams.REASON to "no_active_location")
                            )
                        }
                    } else {
                        locationTimeoutJob?.cancel()
                        loadForLocation(location)
                    }
                }
        }
        settingsObserverJob = viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { SettingsKey(it.calculationMethod, it.hijriAdjustment, it.language, it.juristicMethod) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    monthCache.clear()
                    val location = activeLocationProvider.location.value
                    if (location != null) {
                        loadForLocation(location)
                    }
                }
        }
    }

    private fun loadForLocation(location: LocationData) {
        val newLocationId = location.locationId()
        if (newLocationId != activeLocationId) {
            _uiState.value = PrayerTimesUiState.Loading
        }
        activeLocationId = newLocationId
        val resolvedZoneId = getZoneIdFromLocation(location.countryCode)
        zoneId = resolvedZoneId
        val today = LocalDateTime.now(resolvedZoneId)
        val month = selectedMonthByLocation[activeLocationId] ?: today.date.yearMonth
        loadMonth(month)
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.OnPreviousMonth -> {
                analyticsTracker.logEvent(
                    AnalyticsEvents.MONTH_NAVIGATED,
                    mapOf(
                        AnalyticsParams.DIRECTION to "previous",
                        AnalyticsParams.TARGET_MONTH to selectedMonth().minus(1, DateTimeUnit.MONTH).toString()
                    )
                )
                navigateToMonth(selectedMonth().minus(1, DateTimeUnit.MONTH))
            }
            PrayerTimesEvent.OnNextMonth -> {
                analyticsTracker.logEvent(
                    AnalyticsEvents.MONTH_NAVIGATED,
                    mapOf(
                        AnalyticsParams.DIRECTION to "next",
                        AnalyticsParams.TARGET_MONTH to selectedMonth().plus(1, DateTimeUnit.MONTH).toString()
                    )
                )
                navigateToMonth(selectedMonth().plus(1, DateTimeUnit.MONTH))
            }
            PrayerTimesEvent.OnToday -> {
                analyticsTracker.logEvent(AnalyticsEvents.TODAY_PRESSED)
                navigateToMonth(currentMonth())
            }
        }
    }

    private fun selectedMonth(): YearMonth {
        val id = activeLocationId ?: return currentMonth()
        return selectedMonthByLocation[id] ?: currentMonth()
    }

    private fun navigateToMonth(month: YearMonth) {
        activeLocationId?.let { selectedMonthByLocation[it] = month }
        val current = _uiState.value
        if (current is PrayerTimesUiState.Success) {
            _uiState.value = current.copy(
                selectedMonth = month,
                isCurrentMonth = month == currentMonth()
            )
        }
        loadMonth(month)
    }

    private fun currentMonth(): YearMonth =
        LocalDateTime.now(zoneId ?: ZoneId.systemDefault()).date.yearMonth

    private fun loadMonth(month: YearMonth) {
        if (isLoading) {
            pendingMonth = month
            return
        }
        isLoading = true
        viewModelScope.launch {
            val locationId = activeLocationId ?: run {
                isLoading = false
                return@launch
            }
            val location = activeLocationProvider.location.value ?: run {
                isLoading = false
                return@launch
            }
            val resolvedZoneId = zoneId ?: run {
                isLoading = false
                return@launch
            }
            val settings = getSettingsUseCase()
            val calculationMethod = CalculationMethod.fromSettingsId(settings.calculationMethod)
            val hijriAdjustment = settings.hijriAdjustment
            val juristicMethod = JuristicMethod.fromSettingsId(settings.juristicMethod)
            try {
                val locationCache = monthCache.getOrPut(locationId) { mutableMapOf() }
                val cached = locationCache[month]
                if (cached != null) {
                    emitPartial(month, cached, buildPayload(locationId, location, resolvedZoneId, hijriAdjustment))
                    return@launch
                }
                val persistedMonth = getMonthlyPrayerTimesUseCase(
                    month = month,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoneId = resolvedZoneId,
                    calculationMethod = calculationMethod,
                    juristicMethod = juristicMethod
                )
                if (persistedMonth != null) {
                    val today = LocalDateTime.now(resolvedZoneId).date
                    val adjusted = persistedMonth.map { daily ->
                        daily.copy(
                            prayers = formatter.withLocalizedNames(daily.prayers),
                            gregorianDate = month.onDay(daily.dayOfMonth).toJavaLocalDate().format(gregorianDayAndNameFormatter()),
                            hijriDate = formatter.formatHijriDate(
                                HijrahDate.from(month.onDay(daily.dayOfMonth).toJavaLocalDate()),
                                hijriAdjustment
                            )
                        )
                    }
                    val refreshed = refreshCurrentPrayerFlags(adjusted, today, resolvedZoneId)
                    locationCache[month] = refreshed
                    emitPartial(month, refreshed, buildPayload(locationId, location, resolvedZoneId, hijriAdjustment))
                    return@launch
                }
                val today = LocalDateTime.now(resolvedZoneId)
                val payload = buildPayload(locationId, location, resolvedZoneId, hijriAdjustment)
                val monthlyPrayers = try {
                    coroutineScope {
                        val results = arrayOfNulls<DailyPrayer>(month.numberOfDays)
                        val mutex = Mutex()
                        val todayDayOfMonth =
                            if (today.date.yearMonth == month) today.dayOfMonth else 1
                        val orderedDays = listOf(todayDayOfMonth) +
                            (1..month.numberOfDays).filter { it != todayDayOfMonth }
                        orderedDays.map { day ->
                            async(computationDispatcher) {
                                val computed = computeDailyPrayer(
                                    day, month, location, resolvedZoneId, today, calculationMethod, hijriAdjustment, juristicMethod
                                )
                                mutex.withLock {
                                    results[day - 1] = computed
                                    emitPartial(month, results.filterNotNull().sortedBy { it.dayOfMonth }, payload)
                                }
                            }
                        }.awaitAll()
                        results.filterNotNull().sortedBy { it.dayOfMonth }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = PrayerTimesUiState.Error(
                        e.message ?: "Failed to load prayer times."
                    )
                    analyticsTracker.logEvent(
                        AnalyticsEvents.PRAYER_TIMES_ERROR,
                        mapOf(AnalyticsParams.REASON to (e.message ?: "unknown"))
                    )
                    return@launch
                }
                locationCache[month] = monthlyPrayers
                emitPartial(month, monthlyPrayers, payload)
                backgroundSaveScope.launch {
                    runCatching {
                        saveMonthlyPrayerTimesUseCase(
                            month = month,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            zoneId = resolvedZoneId,
                            calculationMethod = calculationMethod,
                            juristicMethod = juristicMethod,
                            prayers = monthlyPrayers
                        )
                    }.onFailure {
                        analyticsTracker.logEvent(
                            AnalyticsEvents.PRAYER_TIMES_ERROR,
                            mapOf(AnalyticsParams.REASON to "monthly_save_failed")
                        )
                    }
                }
            } finally {
                isLoading = false
                val next = pendingMonth
                pendingMonth = null
                if (next != null && (next != month || locationId != activeLocationId)) {
                    loadMonth(next)
                }
            }
        }
    }

    private suspend fun computeDailyPrayer(
        day: Int,
        month: YearMonth,
        location: LocationData,
        resolvedZoneId: ZoneId,
        today: LocalDateTime,
        calculationMethod: CalculationMethod,
        hijriAdjustment: Int,
        juristicMethod: JuristicMethod
    ): DailyPrayer {
        val date = month.onDay(day)
        val prayerTimes = getPrayerTimesUseCase(
            date = date.atTime(0, 0),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = resolvedZoneId,
            calculationMethod = calculationMethod,
            juristicMethod = juristicMethod,
            persistDailyCache = false
        ).getOrElse { throw it }
        val langDetectedPrayerTimes = formatter.withLocalizedNames(prayerTimes)
        val isToday = date == today.date
        val (currentPrayer, _) = if (isToday) {
            calculator.findCurrentAndNextPrayer(langDetectedPrayerTimes, resolvedZoneId)
        } else {
            Pair(null, null)
        }
        val prayersWithCurrent = langDetectedPrayerTimes.map {
            it.copy(isCurrent = isToday && it.name == currentPrayer?.name)
        }
        val timeState = formatter.getInitialTimeInfo(
            resolvedZoneId,
            date.toJavaLocalDate(),
            HijrahDate.from(date.toJavaLocalDate()),
            hijriAdjustment
        )
        return DailyPrayer(
            dayOfMonth = date.day,
            prayers = prayersWithCurrent,
            gregorianDate = timeState.gregorianDayAndName,
            hijriDate = timeState.hijriDate
        )
    }

    private data class SuccessPayload(
        val locationId: String,
        val timeState: TimeUiState,
        val locationState: LocationUiState
    )

    private data class SettingsKey(
        val calculationMethod: String,
        val hijriAdjustment: Int,
        val language: String,
        val juristicMethod: String
    )

    private suspend fun buildPayload(
        locationId: String,
        location: LocationData,
        resolvedZoneId: ZoneId,
        hijriAdjustment: Int
    ): SuccessPayload = SuccessPayload(
        locationId = locationId,
        timeState = formatter.getInitialTimeInfo(resolvedZoneId, hijriAdjustment = hijriAdjustment),
        locationState = LocationUiState(
            locationData = location,
            locationInfoText = formatter.locationInfo(location)
        )
    )

    private fun emitPartial(
        month: YearMonth,
        monthlyPrayers: List<DailyPrayer>,
        payload: SuccessPayload
    ) {
        if (month != selectedMonth() || payload.locationId != activeLocationId) return
        val today = LocalDateTime.now(zoneId ?: ZoneId.systemDefault())
        _uiState.value = PrayerTimesUiState.Success(
            monthlyPrayers = monthlyPrayers,
            currentDayOfMonth = today.day,
            selectedMonth = month,
            isCurrentMonth = month == today.date.yearMonth,
            timeState = payload.timeState,
            locationState = payload.locationState
        )
    }

    private fun refreshCurrentPrayerFlags(
        monthlyPrayers: List<DailyPrayer>,
        today: kotlinx.datetime.LocalDate,
        resolvedZoneId: ZoneId
    ): List<DailyPrayer> = monthlyPrayers.map { daily ->
        if (daily.dayOfMonth == today.day) {
            val (currentPrayer, _) = calculator.findCurrentAndNextPrayer(daily.prayers, resolvedZoneId)
            daily.copy(
                prayers = daily.prayers.map {
                    it.copy(isCurrent = it.name == currentPrayer?.name)
                }
            )
        } else {
            daily
        }
    }

    private fun LocationData.locationId(): String = "$latitude,$longitude"
}
