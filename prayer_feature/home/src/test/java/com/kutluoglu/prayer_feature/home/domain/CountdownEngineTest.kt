package com.kutluoglu.prayer_feature.home.domain

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownEngineTest {

    private val calculator: PrayerLogicEngine = mockk(relaxed = true)
    private val formatter: PrayerFormatter = mockk(relaxed = true)
    private val zoneId = ZoneId.of("Europe/Istanbul")

    private val today = LocalDate(2026, 8, 2)
    private val nextPrayer = Prayer(
        name = "Öğle",
        arabicName = "الظهر",
        time = LocalTime(12, 30),
        date = today
    )
    private val prayerState = PrayerUiState(
        prayers = listOf(nextPrayer),
        currentPrayer = Prayer("İmsak", "الفجر", LocalTime(5, 0), today),
        nextPrayer = nextPrayer
    )

    private fun engineAt(instant: Instant) = CountdownEngine(
        calculator = calculator,
        formatter = formatter,
        clock = Clock.fixed(instant, zoneId)
    )

    @Test
    fun `tick emits countdown state every second`() = runTest {
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(prayerState.currentPrayer, nextPrayer)
        coEvery { calculator.calculateTimeRemaining(nextPrayer.time, zoneId) } returns Duration.ofHours(2)
        every { formatter.getFormattedCurrentTime(zoneId) } returns "10:30:00"
        every { formatter.formatTimeRemaining(Duration.ofHours(2)) } returns "02:00:00"

        val engine = engineAt(Instant.parse("2026-08-02T07:30:00Z"))
        engine.start(prayerState, zoneId, scope = this)

        advanceTimeBy(1_000)
        runCurrent()

        assertThat(engine.countdownState.value.timeRemaining).isEqualTo("02:00:00")
        assertThat(engine.countdownState.value.currentTime).isEqualTo("10:30:00")

        engine.stop()
    }

    @Test
    fun `tick emits prayerPassedSignal when current time has passed the next prayer`() = runTest {
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(prayerState.nextPrayer, null)
        coEvery { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ZERO
        every { formatter.getFormattedCurrentTime(zoneId) } returns "12:31:00"

        val engine = engineAt(Instant.parse("2026-08-02T09:31:00Z"))
        engine.prayerPassedSignal.test {
            engine.start(prayerState, zoneId, scope = this)

            advanceTimeBy(1_000)
            runCurrent()

            awaitItem()
            engine.stop()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tick emits dayChangedSignal when the prayer date is stale`() = runTest {
        val yesterday = LocalDate(2026, 8, 1)
        val stalePrayerState = prayerState.copy(
            currentPrayer = prayerState.currentPrayer?.copy(date = yesterday),
            nextPrayer = nextPrayer.copy(date = today)
        )
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(null, stalePrayerState.nextPrayer)
        coEvery { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ofHours(1)
        every { formatter.getFormattedCurrentTime(zoneId) } returns "01:00:00"
        every { formatter.formatTimeRemaining(any()) } returns "01:00:00"

        val engine = engineAt(Instant.parse("2026-08-01T22:00:00Z"))
        engine.dayChangedSignal.test {
            engine.start(stalePrayerState, zoneId, scope = this)

            advanceTimeBy(1_000)
            runCurrent()

            awaitItem()
            engine.stop()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
