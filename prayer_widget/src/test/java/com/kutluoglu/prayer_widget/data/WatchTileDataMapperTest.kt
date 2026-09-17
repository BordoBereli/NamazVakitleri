package com.kutluoglu.prayer_widget.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WatchTileDataMapperTest {

    private val mapper = WatchTileDataMapper()

    @Test
    fun `maps WidgetData to WatchTileData`() {
        val data = WidgetData(
            nextPrayerName = "İkindi",
            nextPrayerTime = "15:30",
            countdownText = "2s 14d",
            ringProgress = 0.5f,
            locationName = "İstanbul",
            gregorianDate = "16.09.2026",
            hijriDate = "4 Rebiülahir 1448",
            prayers = listOf(
                WidgetPrayer(name = "Öğle", time = "12:45", isNext = false),
                WidgetPrayer(name = "İkindi", time = "15:30", isNext = true)
            ),
            isJumuah = false,
            currentPrayerEpochMillis = 1_699_999_000_000L,
            nextPrayerEpochMillis = 1_700_000_000_000L
        )

        val mapped = mapper.map(data)

        assertThat(mapped.locationName).isEqualTo("İstanbul")
        assertThat(mapped.nextPrayerName).isEqualTo("İkindi")
        assertThat(mapped.nextPrayerEpochMillis).isEqualTo(1_700_000_000_000L)
        assertThat(mapped.currentPrayerEpochMillis).isEqualTo(1_699_999_000_000L)
        assertThat(mapped.isJumuah).isFalse()
        assertThat(mapped.prayers).hasSize(2)
        assertThat(mapped.prayers[1].isNext).isTrue()
        assertThat(mapped.prayers[1].time).isEqualTo("15:30")
        assertThat(mapped.syncedAtEpochMillis).isGreaterThan(0L)
    }
}
