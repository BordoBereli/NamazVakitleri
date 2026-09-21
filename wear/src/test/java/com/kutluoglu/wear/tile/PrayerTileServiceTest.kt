package com.kutluoglu.wear.tile

import android.content.Context
import androidx.wear.tiles.RequestBuilders
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.wear.data.TileDataRepository
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PrayerTileServiceTest {

    private val repository = mockk<TileDataRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val now = System.currentTimeMillis()
    private val sampleData = WatchTileData(
        locationName = "İstanbul",
        nextPrayerName = "Akşam",
        nextPrayerEpochMillis = now + 3_600_000L,
        currentPrayerEpochMillis = now - 3_600_000L,
        isJumuah = false,
        prayers = listOf(
            WatchPrayer(name = "Öğle", time = "13:00", isNext = false),
            WatchPrayer(name = "Akşam", time = "18:30", isNext = true)
        ),
        syncedAtEpochMillis = now
    )

    @Test
    fun `tile response has a single ring page and freshness interval`() = runTest {
        coEvery { repository.getTileData() } returns sampleData
        val service = PrayerTileService(repositoryOverride = repository)

        val tile = service.buildTileForTest(
            requestParams = RequestBuilders.TileRequest.Builder().build(),
            context = context
        )

        val timeline = checkNotNull(tile.tileTimeline)
        assertThat(timeline.timelineEntries).hasSize(1)
        assertThat(tile.freshnessIntervalMillis).isEqualTo(60_000L)
    }

    @Test
    fun `tile response renders placeholder when no data`() = runTest {
        coEvery { repository.getTileData() } returns null
        val service = PrayerTileService(repositoryOverride = repository)

        val tile = service.buildTileForTest(
            requestParams = RequestBuilders.TileRequest.Builder().build(),
            context = context
        )

        val timeline = checkNotNull(tile.tileTimeline)
        assertThat(timeline.timelineEntries).hasSize(1)
    }

    @Test
    fun `isStale is true when synced more than five minutes ago`() {
        val now = System.currentTimeMillis()

        assertThat(TileLayouts.isStale(syncedAtEpochMillis = now - 6 * 60_000L, nowEpochMillis = now))
            .isTrue()
    }

    @Test
    fun `isStale is false when synced within five minutes`() {
        val now = System.currentTimeMillis()

        assertThat(TileLayouts.isStale(syncedAtEpochMillis = now - 1 * 60_000L, nowEpochMillis = now))
            .isFalse()
    }
}
