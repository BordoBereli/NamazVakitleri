package com.kutluoglu.prayer.data

import com.kutluoglu.prayer.data.cache.PrayerTimesCache
import com.kutluoglu.prayer.data.repository.prayer.PrayerDataStore
import com.kutluoglu.prayer.data.source.prayer.PrayerDataStoreImp
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.services.PrayerCalculationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId

class PrayerDataStoreImsakTest {

    private val service = mockk<PrayerCalculationService>(relaxed = true)
    private val cache = mockk<PrayerTimesCache>(relaxed = true)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    private val store: PrayerDataStore = PrayerDataStoreImp(service, cache, scope)

    @Test
    fun `returns imsak as first prayer from calculation`() = runTest {
        val date = LocalDateTime(2026, 9, 2, 0, 0)
        coEvery { cache.get(any()) } returns null
        coEvery { service.calculateDailyPrayerTimes(any(), any(), any(), any(), any(), any()) } returns listOf(
            Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 9, 2), isImsak = true),
            Prayer("Sunrise", "الشروق", LocalTime(6, 0), LocalDate(2026, 9, 2))
        )
        val prayers = store.getPrayerTimes(
            date = date, latitude = 41.0, longitude = 29.0,
            zoneId = ZoneId.of("Europe/Istanbul"),
            calculationMethod = CalculationMethod.TURKEY_DIYANET
        )
        assertEquals("Imsak", prayers.first().name)
        assertTrue(prayers.first().isImsak)
    }
}
