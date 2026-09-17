package com.kutluoglu.wear.shared.data

import com.google.android.gms.wearable.DataMap
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import org.junit.jupiter.api.Test

class WatchTileDataCodecTest {

    private val sample = WatchTileData(
        locationName = "İstanbul",
        nextPrayerName = "İkindi",
        nextPrayerEpochMillis = 1_700_000_000_000L,
        currentPrayerEpochMillis = 1_699_999_000_000L,
        isJumuah = false,
        prayers = listOf(WatchPrayer(name = "İkindi", time = "15:30", isNext = true)),
        syncedAtEpochMillis = 1_699_998_000_000L
    )

    @Test
    fun `toDataMap then fromDataMap round-trips`() {
        val dataMap = WatchTileDataCodec.toDataMap(sample)

        val decoded = WatchTileDataCodec.fromDataMap(dataMap)

        assertThat(decoded).isEqualTo(sample)
    }

    @Test
    fun `fromDataMap returns null for empty map`() {
        assertThat(WatchTileDataCodec.fromDataMap(DataMap())).isNull()
    }

    @Test
    fun `toJson then fromJson round-trips`() {
        val json = WatchTileDataCodec.toJson(sample)

        assertThat(WatchTileDataCodec.fromJson(json)).isEqualTo(sample)
    }

    @Test
    fun `path constant starts with slash`() {
        assertThat(WatchTileDataCodec.PATH).startsWith("/")
    }
}
