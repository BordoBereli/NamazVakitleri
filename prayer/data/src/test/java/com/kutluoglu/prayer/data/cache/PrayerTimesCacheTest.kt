package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PrayerTimesCacheTest {

    private lateinit var cache: PrayerTimesCache
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        cache = PrayerTimesCache(dataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `get returns null for a missing cache key`() = runBlocking {
        val result = cache.get("missing-key")

        assertThat(result).isNull()
    }

    @Test
    fun `put then get returns the cached prayers`() = runBlocking {
        val prayers = listOf(
            Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1)),
            Prayer("Isha", "العشاء", LocalTime.parse("20:42"), LocalDate(2024, 1, 1))
        )

        cache.put("2024-01-01|41.0|29.0|Europe/Istanbul", prayers)

        val result = cache.get("2024-01-01|41.0|29.0|Europe/Istanbul")
        assertThat(result).isEqualTo(prayers)
    }

    @Test
    fun `clear removes all cached prayers`() = runBlocking {
        cache.put("key-1", listOf(Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))))
        cache.put("key-2", listOf(Prayer("Isha", "العشاء", LocalTime.parse("20:42"), LocalDate(2024, 1, 1))))

        cache.clear()

        assertThat(cache.get("key-1")).isNull()
        assertThat(cache.get("key-2")).isNull()
    }

    @Test
    fun `getMonth returns null for a missing cache key`() = runBlocking {
        val result = cache.getMonth("2024-01|41.0|29.0|Europe/Istanbul")

        assertThat(result).isNull()
    }

    @Test
    fun `putMonth then getMonth returns the cached daily prayers`() = runBlocking {
        val dailyPrayers = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1)),
                    Prayer("Isha", "العشاء", LocalTime.parse("20:42"), LocalDate(2024, 1, 1))
                )
            ),
            DailyPrayer(
                dayOfMonth = 2,
                gregorianDate = "2 Tuesday",
                hijriDate = "2 Muharram 1448",
                prayers = listOf(
                    Prayer("Fajr", "الفجر", LocalTime.parse("05:01"), LocalDate(2024, 1, 2)),
                    Prayer("Isha", "العشاء", LocalTime.parse("20:41"), LocalDate(2024, 1, 2))
                )
            )
        )

        cache.putMonth("2024-01|41.0|29.0|Europe/Istanbul", dailyPrayers)

        val result = cache.getMonth("2024-01|41.0|29.0|Europe/Istanbul")
        assertThat(result).isEqualTo(dailyPrayers)
    }

    @Test
    fun `clear removes cached months too`() = runBlocking {
        cache.putMonth(
            "2024-01|41.0|29.0|Europe/Istanbul",
            listOf(
                DailyPrayer(
                    dayOfMonth = 1,
                    gregorianDate = "1 Monday",
                    hijriDate = "1 Muharram 1448",
                    prayers = listOf(
                        Prayer("Fajr", "الفجر", LocalTime.parse("05:00"), LocalDate(2024, 1, 1))
                    )
                )
            )
        )

        cache.clear()

        assertThat(cache.getMonth("2024-01|41.0|29.0|Europe/Istanbul")).isNull()
    }
}
