package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.core.annotation.Factory
import java.time.Clock
import java.time.ZoneId

/**
 * Drives the per-second countdown tick. Publishes [countdownState] and signals when a prayer
 * time has elapsed ([prayerPassedSignal]) or the date changed ([dayChangedSignal]).
 */
@Factory
class CountdownEngine(
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val _countdownState = MutableStateFlow(CountdownUiState())
    val countdownState: StateFlow<CountdownUiState> = _countdownState

    private val _prayerPassedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val prayerPassedSignal: SharedFlow<Unit> = _prayerPassedSignal

    private val _dayChangedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dayChangedSignal: SharedFlow<Unit> = _dayChangedSignal

    private var countdownJob: Job? = null
    private var zoneId: ZoneId? = null
    private var prayerState: PrayerUiState? = null

    /** Launches the tick loop. Call [stop] before reusing with a new prayer set. */
    fun start(prayerState: PrayerUiState, zoneId: ZoneId, scope: CoroutineScope): Job {
        this.prayerState = prayerState
        this.zoneId = zoneId
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                updateCountdown()
                delay(1_000)
            }
        }
        return countdownJob ?: Job()
    }

    fun stop() {
        countdownJob?.cancel()
        countdownJob = null
        prayerState = null
        zoneId = null
    }

    fun isRunning(): Boolean = countdownJob?.isActive == true

    private fun currentDateTime(zoneId: ZoneId): LocalDateTime =
        java.time.LocalDateTime.now(clock.withZone(zoneId)).toKotlinLocalDateTime()

    private suspend fun updateCountdown() {
        val currentState = prayerState ?: return
        val currentZoneId = zoneId ?: return
        val nextPrayer = currentState.nextPrayer
        val currentTime = currentDateTime(currentZoneId)
        val currentTimeString = formatter.getFormattedCurrentTime(currentZoneId)

        if (nextPrayer != null) {
            val currentPrayer = currentState.currentPrayer
            if (currentPrayer != null && nextPrayer.date != currentPrayer.date) {
                if (isDayChanged(currentPrayer.date, currentTime.date)) {
                    countdownJob?.cancel()
                    _dayChangedSignal.emit(Unit)
                    return
                }
            }

            val nextPrayerDateTime = LocalDateTime(date = nextPrayer.date, time = nextPrayer.time)
            if (currentTime >= nextPrayerDateTime) {
                _prayerPassedSignal.emit(Unit)
                return
            }

            val duration = calculator.calculateTimeRemaining(nextPrayer.time, currentZoneId)
            _countdownState.value = CountdownUiState(
                timeRemaining = formatter.formatTimeRemaining(duration),
                currentTime = currentTimeString
            )
        } else {
            val currentDeviceDate = currentTime.date
            val prayerDate = currentState.prayers.firstOrNull()?.date
            if (isDayChanged(prayerDate, currentDeviceDate)) {
                countdownJob?.cancel()
                _dayChangedSignal.emit(Unit)
            } else {
                _countdownState.value = CountdownUiState(
                    timeRemaining = "--:--:--",
                    currentTime = currentTimeString
                )
            }
        }
    }

    private fun isDayChanged(
        prayerDate: LocalDate?,
        currentDeviceDate: LocalDate
    ): Boolean = prayerDate != null && currentDeviceDate != prayerDate
}
