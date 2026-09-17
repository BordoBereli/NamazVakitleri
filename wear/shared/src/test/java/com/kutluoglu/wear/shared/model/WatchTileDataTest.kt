package com.kutluoglu.wear.shared.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class WatchTileDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serializes and deserializes WatchTileData round-trip`() {
        val original = WatchTileData(
            locationName = "İstanbul",
            nextPrayerName = "İkindi",
            nextPrayerEpochMillis = 1_700_000_000_000L,
            currentPrayerEpochMillis = 1_699_999_000_000L,
            isJumuah = false,
            prayers = listOf(
                WatchPrayer(name = "Öğle", time = "12:45", isNext = false, isJumuah = false),
                WatchPrayer(name = "İkindi", time = "15:30", isNext = true, isJumuah = false)
            ),
            syncedAtEpochMillis = 1_699_998_000_000L
        )

        val decoded = json.decodeFromString<WatchTileData>(json.encodeToString(WatchTileData.serializer(), original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `serializes empty prayer list`() {
        val original = WatchTileData(
            locationName = "Ankara",
            nextPrayerName = "Akşam",
            nextPrayerEpochMillis = 1L,
            currentPrayerEpochMillis = 0L,
            isJumuah = false,
            prayers = emptyList(),
            syncedAtEpochMillis = 0L
        )

        val decoded = json.decodeFromString<WatchTileData>(json.encodeToString(WatchTileData.serializer(), original))

        assertThat(decoded.prayers).isEmpty()
    }
}
