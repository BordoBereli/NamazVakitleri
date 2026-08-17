package com.kutluoglu.prayer.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.createBy
import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.source.prayer.PrayerDataStoreImp
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
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

    @Test
    fun `calculation runs off main thread on cache miss`() = runTest {
        // GIVEN a cache miss
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val calculatedPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        coEvery { prayerTimesCache.get(any()) } returns null
        val callerThread = Thread.currentThread().name
        var calculationThread: String? = null
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } answers {
            calculationThread = Thread.currentThread().name
            calculatedPrayers
        }

        // WHEN requesting prayer times
        dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

        // THEN the CPU-bound calculation must not run on the calling (main) thread
        assertThat(calculationThread).isNotNull()
        assertThat(calculationThread).isNotEqualTo(callerThread)
    }

    @Test
    fun `clearCache clears the prayer times cache`() = runTest {
        dataStore.clearCache()

        coVerify(exactly = 1) { prayerTimesCache.clear() }
    }

    @Test
    fun `getMonthlyPrayerTimes returns the cached month from the cache`() = runTest {
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val cachedMonth = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))
                )
            )
        )
        coEvery { prayerTimesCache.getMonth(any()) } returns cachedMonth

        val result = dataStore.getMonthlyPrayerTimes(month, 41.0, 29.0, zoneId)

        assertThat(result).isEqualTo(cachedMonth)
        coVerify(exactly = 1) {
            prayerTimesCache.getMonth("2024-01|41.0|29.0|Europe/Istanbul")
        }
    }

    @Test
    fun `saveMonthlyPrayerTimes stores the month in the cache`() = runTest {
        val month = YearMonth(2024, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val monthToSave = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))
                )
            )
        )

        dataStore.saveMonthlyPrayerTimes(month, 41.0, 29.0, zoneId, monthToSave)

        coVerify(exactly = 1) {
            prayerTimesCache.putMonth("2024-01|41.0|29.0|Europe/Istanbul", monthToSave)
        }
    }

    @Test
    fun `getPrayerTimes pre-caches tomorrow's prayers on cache miss`() = runTest {
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val tomorrow = testDate.date.plus(1, DateTimeUnit.DAY)
            .atTime(testDate.hour, testDate.minute, testDate.second, testDate.nanosecond)
        val todayPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        val tomorrowPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:01"), tomorrow.date)
        )
        coEvery { prayerTimesCache.get(any()) } returns null
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returnsMany listOf(todayPrayers, tomorrowPrayers)

        val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

        assertThat(result).isEqualTo(todayPrayers)
        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, testDate,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, tomorrow,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 1) {
            prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul", todayPrayers)
        }
        coVerify(exactly = 1) {
            prayerTimesCache.put("2024-01-02|41.0|29.0|Europe/Istanbul", tomorrowPrayers)
        }
    }

    @Test
    fun `getPrayerTimes skips pre-caching when tomorrow is already cached`() = runTest {
        val testDate = LocalDateTime.createBy(2024, 1, 1)
        val zoneId = ZoneId.of("Europe/Istanbul")
        val tomorrow = testDate.date.plus(1, DateTimeUnit.DAY)
            .atTime(testDate.hour, testDate.minute, testDate.second, testDate.nanosecond)
        val todayPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), testDate.date)
        )
        val tomorrowPrayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:01"), tomorrow.date)
        )
        coEvery { prayerTimesCache.get("2024-01-01|41.0|29.0|Europe/Istanbul") } returns null
        coEvery { prayerTimesCache.get("2024-01-02|41.0|29.0|Europe/Istanbul") } returns tomorrowPrayers
        coEvery {
            prayerCalculationService.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any())
        } returns todayPrayers

        val result = dataStore.getPrayerTimes(testDate, 41.0, 29.0, zoneId)

        assertThat(result).isEqualTo(todayPrayers)
        coVerify(exactly = 1) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, testDate,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 0) {
            prayerCalculationService.calculateDailyPrayerTimes(
                41.0, 29.0, zoneId, tomorrow,
                CalculationMethod.TURKEY_DIYANET, JuristicMethod.STANDARD
            )
        }
        coVerify(exactly = 1) {
            prayerTimesCache.put("2024-01-01|41.0|29.0|Europe/Istanbul", todayPrayers)
        }
        coVerify(exactly = 0) {
            prayerTimesCache.put("2024-01-02|41.0|29.0|Europe/Istanbul", any())
        }
    }
}
