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

    private val builder = builderWith(clock)

    @Test
    fun `build returns six prayers with localized names`() {
        val data = buildData()
        assertThat(data).isNotNull()
        val result = data!!

        assertThat(result.prayers).hasSize(6)
        assertThat(result.prayers.map { it.name }).containsExactlyElementsIn(prayerNames)
        assertThat(result.locationName).isEqualTo(locationName)
    }

    @Test
    fun `next prayer is after current time`() {
        val data = buildData()
        assertThat(data).isNotNull()
        val result = data!!

        val currentTimeMillis = clock.instant().toEpochMilli()
        assertThat(result.nextPrayerEpochMillis).isGreaterThan(currentTimeMillis)
        assertThat(result.nextPrayerName).isIn(prayerNames)
    }

    @Test
    fun `isJumuah is true when next prayer is Dhuhr on Friday`() {
        val data = buildData()
        assertThat(data).isNotNull()
        val result = data!!

        assertThat(result.nextPrayerName).isEqualTo("Öğle")
        assertThat(result.isJumuah).isTrue()
        assertThat(result.prayers.first { it.name == "Öğle" }.isJumuah).isTrue()
        assertThat(result.prayers.first { it.name == "Öğle" }.isNext).isTrue()
    }

    @Test
    fun `isJumuah is false when next prayer is Dhuhr on a non-Friday`() {
        val data = buildData(date = LocalDateTime(2026, 9, 17, 10, 0))
        assertThat(data).isNotNull()
        val result = data!!

        assertThat(result.nextPrayerName).isEqualTo("Öğle")
        assertThat(result.isJumuah).isFalse()
    }

    @Test
    fun `prayer times are formatted as HH mm`() {
        val data = buildData()
        assertThat(data).isNotNull()
        val result = data!!

        result.prayers.forEach { prayer ->
            assertThat(prayer.time).matches("\\d{2}:\\d{2}")
        }
    }

    @Test
    fun `syncedAtEpochMillis is approximately now`() {
        val data = buildData()
        assertThat(data).isNotNull()
        val result = data!!

        val now = System.currentTimeMillis()
        assertThat(Math.abs(result.syncedAtEpochMillis - now)).isLessThan(5_000L)
    }

    @Test
    fun `before first prayer returns sunrise as next and isha as current`() {
        // 03:00 in Istanbul, before Sunrise.
        val beforeSunriseClock = Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), zoneId)
        val data = buildData(builder = builderWith(beforeSunriseClock))
        assertThat(data).isNotNull()
        val result = data!!

        val nowMillis = beforeSunriseClock.instant().toEpochMilli()
        assertThat(result.nextPrayerName).isEqualTo("Güneş")
        assertThat(result.nextPrayerEpochMillis).isGreaterThan(nowMillis)
        // The engine reports Isha (today) as the "current" prayer before Sunrise.
        assertThat(result.currentPrayerEpochMillis).isGreaterThan(nowMillis)
    }

    @Test
    fun `after last prayer returns tomorrow sunrise as next`() {
        // 23:00 in Istanbul, after Isha.
        val afterIshaClock = Clock.fixed(Instant.parse("2026-09-18T20:00:00Z"), zoneId)
        val data = buildData(builder = builderWith(afterIshaClock))
        assertThat(data).isNotNull()
        val result = data!!

        val nowMillis = afterIshaClock.instant().toEpochMilli()
        assertThat(result.nextPrayerName).isEqualTo("Güneş")
        assertThat(result.nextPrayerEpochMillis).isGreaterThan(nowMillis)
        val nextDate = Instant.ofEpochMilli(result.nextPrayerEpochMillis)
            .atZone(zoneId)
            .toLocalDate()
        assertThat(nextDate).isEqualTo(java.time.LocalDate.of(2026, 9, 19))
    }

    private fun builderWith(clock: Clock): WatchTileDataBuilder = WatchTileDataBuilder(
        prayerTimeEngine = PrayerTimeEngine(),
        prayerLogicEngine = PrayerLogicEngine(clock)
    )

    private fun buildData(
        builder: WatchTileDataBuilder = this.builder,
        date: LocalDateTime = this.date
    ): WatchTileData? = builder.build(
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
