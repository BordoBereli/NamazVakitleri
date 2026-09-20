package com.kutluoglu.wear.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.domain.PrayerTimeEngine
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class WatchTileDataBuilderTest {

    private val latitude = 41.0082
    private val longitude = 28.9784
    private val zoneId = ZoneId.of("Europe/Istanbul")
    private val date = LocalDateTime(2026, 9, 18, 10, 0)
    private val calculationMethod = CalculationMethod.TURKEY_DIYANET
    private val juristicMethod = JuristicMethod.STANDARD
    private val locationName = "İstanbul"
    private val prayerNames = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")

    // Fixed at 10:00 in Istanbul on Friday 2026-09-18 (between Sunrise and Dhuhr).
    private val clock = Clock.fixed(
        Instant.parse("2026-09-18T07:00:00Z"),
        zoneId
    )

    private val builder = WatchTileDataBuilder(
        prayerTimeEngine = PrayerTimeEngine(),
        prayerLogicEngine = PrayerLogicEngine(clock)
    )

    @Test
    fun `build returns six prayers with localized names`() {
        val data = buildData()

        assertThat(data).isNotNull()
        assertThat(data!!.prayers).hasSize(6)
        assertThat(data.prayers.map { it.name }).containsExactlyElementsIn(prayerNames)
        assertThat(data.locationName).isEqualTo(locationName)
    }

    @Test
    fun `next prayer is after current time`() {
        val data = buildData()!!

        val currentTimeMillis = clock.instant().toEpochMilli()
        assertThat(data.nextPrayerEpochMillis).isGreaterThan(currentTimeMillis)
        assertThat(data.nextPrayerName).isIn(prayerNames)
    }

    @Test
    fun `isJumuah is true when next prayer is Dhuhr on Friday`() {
        val data = buildData()!!

        assertThat(data.nextPrayerName).isEqualTo("Öğle")
        assertThat(data.isJumuah).isTrue()
        assertThat(data.prayers.first { it.name == "Öğle" }.isJumuah).isTrue()
        assertThat(data.prayers.first { it.name == "Öğle" }.isNext).isTrue()
    }

    @Test
    fun `isJumuah is false when next prayer is Dhuhr on a non-Friday`() {
        val data = buildData(date = LocalDateTime(2026, 9, 17, 10, 0))!!

        assertThat(data.nextPrayerName).isEqualTo("Öğle")
        assertThat(data.isJumuah).isFalse()
    }

    @Test
    fun `prayer times are formatted as HH mm`() {
        val data = buildData()!!

        data.prayers.forEach { prayer ->
            assertThat(prayer.time).matches("\\d{2}:\\d{2}")
        }
    }

    @Test
    fun `syncedAtEpochMillis is approximately now`() {
        val data = buildData()!!

        val now = System.currentTimeMillis()
        assertThat(Math.abs(data.syncedAtEpochMillis - now)).isLessThan(5_000L)
    }

    private fun buildData(date: LocalDateTime = this.date): WatchTileData? = builder.build(
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
        date = date,
        calculationMethod = calculationMethod,
        juristicMethod = juristicMethod,
        locationName = locationName,
        prayerNames = prayerNames
    )
}
