package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.source.prayer.PrayerDataStoreImp
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerDataStoreImpTest {

    private lateinit var prayerCalculationService: PrayerCalculationService
    private lateinit var prayerTimesCache: PrayerTimesCache
    private lateinit var dataStore: PrayerDataStoreImp

    @BeforeEach
    fun setUp() {
        prayerCalculationService = mockk()
        prayerTimesCache = mockk(relaxed = true)
        dataStore = PrayerDataStoreImp(prayerCalculationService, prayerTimesCache)
    }

    @Test
    fun `getPrayerTimes returns cached prayers without recalculating when cache hit`() = runTest {
        // GIVEN a cache hit for the day/location key
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val cachedPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        coEvery { prayerTimesCache.get(any()) } returns cachedPrayers

        // WHEN requesting prayer times
        val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

        // THEN the cached prayers are returned and no calculation happens
        assertThat(result).isEqualTo(cachedPrayers)
        coVerify(exactly = 1) { prayerTimesCache.get("2024-01-01|41.0|29.0|Europe/Istanbul") }
        coVerify(exactly = 0) {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `getPrayerTimes calculates and caches when cache miss`() = runTest {
        // GIVEN a cache miss
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val calculatedPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        coEvery { prayerTimesCache.get(any()) } returns null
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returns calculatedPrayers

        // WHEN requesting prayer times
        val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

        // THEN it calculates with the standard Turkey method and stores the result
        assertThat(result).isEqualTo(calculatedPrayers)
        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, testDate,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 1) {
            prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul", calculatedPrayers)
        }
    }
}
