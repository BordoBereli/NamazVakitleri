package com.kutluoglu.prayer_feature.home.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.getZoneIdFromLocation
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import kotlin.Result.Companion.success

class PrayerTimesLoaderTest {

    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = mockk()
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

    @Test
    fun `load builds prayerState timeState locationState on success`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns success(listOf(fajr, dhuhr))
        every { formatter.withLocalizedNames(any()) } returns listOf(fajr, dhuhr)
        every { formatter.getInitialTimeInfo(any(), any(), any()) } returns TimeUiState(gregorianFullDate = "02 Ağustos 2026")
        every { formatter.locationInfo(any()) } returns "Istanbul, TR"
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(fajr, dhuhr)

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val result = loader.load(location)

        assertThat(result.isSuccess).isTrue()
        val loaded = result.getOrThrow()
        assertThat(loaded.prayerState.currentPrayer).isEqualTo(fajr)
        assertThat(loaded.prayerState.nextPrayer).isEqualTo(dhuhr)
        assertThat(loaded.prayerState.prayers[0].isCurrent).isTrue()
        assertThat(loaded.timeState.gregorianFullDate).isEqualTo("02 Ağustos 2026")
        assertThat(loaded.locationState.locationInfoText).isEqualTo("Istanbul, TR")
    }

    @Test
    fun `load maps failure to a failed Result`() = runTest {
        coEvery { getPrayerTimesUseCase.invoke(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("fetch failed"))

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val result = loader.load(location)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("fetch failed")
    }

    @Test
    fun `computePrayerState marks only the current prayer as isCurrent`() = runTest {
        val date = LocalDate(2026, 8, 2)
        val fajr = Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = date)
        val dhuhr = Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = date)
        every { calculator.findCurrentAndNextPrayer(any(), any()) } returns Pair(dhuhr, null)

        val loader = PrayerTimesLoader(getPrayerTimesUseCase, calculator, formatter)
        val zoneId = getZoneIdFromLocation(location.countryCode)
        val state = loader.computePrayerState(listOf(fajr, dhuhr), zoneId)

        assertThat(state.prayers[0].isCurrent).isFalse()
        assertThat(state.prayers[1].isCurrent).isTrue()
        assertThat(state.currentPrayer).isEqualTo(dhuhr)
    }
}
