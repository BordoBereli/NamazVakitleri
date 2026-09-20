package com.kutluoglu.wear.data

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TileDataRepositoryTest {

    private val dataClient = mockk<DataClient>(relaxed = true)
    private val dataStore = mockk<TileDataStore>(relaxed = true)
    private val messageClient = mockk<MessageClient>(relaxed = true)
    private val nodeClient = mockk<NodeClient>(relaxed = true)
    private val repository = TileDataRepository(dataClient, dataStore, messageClient, nodeClient)

    private val sampleData = WatchTileData(
        locationName = "İstanbul",
        nextPrayerName = "Akşam",
        nextPrayerEpochMillis = 1_700_000_000_000,
        currentPrayerEpochMillis = 1_699_999_000_000,
        isJumuah = false,
        prayers = listOf(WatchPrayer(name = "Öğle", time = "13:00", isNext = false)),
        syncedAtEpochMillis = 1_700_000_000_000
    )

    @Test
    fun `returns data from data client and caches it`() = runTest {
        val dataItem = mockk<DataItem>()
        val dataMap = WatchTileDataCodec.toDataMap(sampleData)
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 1
        every { buffer[0] } returns dataItem
        mockkStatic(DataMapItem::class)
        every { DataMapItem.fromDataItem(dataItem).dataMap } returns dataMap
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)

        val result = repository.getTileData()

        assertThat(result).isEqualTo(sampleData)
        coVerify { dataStore.save(any()) }
    }

    @Test
    fun `falls back to cache when data client empty`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        coEvery { dataStore.read() } returns WatchTileDataCodec.toJson(sampleData)

        val result = repository.getTileData()

        assertThat(result).isEqualTo(sampleData)
    }

    @Test
    fun `returns null when client empty and no cache`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        coEvery { dataStore.read() } returns null

        val result = repository.getTileData()

        assertThat(result).isNull()
    }

    @Test
    fun `requestSync sends message to first connected node`() = runTest {
        val node = mockk<Node>()
        every { node.id } returns "phone-node"
        coEvery { nodeClient.connectedNodes } returns Tasks.forResult(listOf(node))
        coEvery { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)

        repository.requestSync()

        coVerify { messageClient.sendMessage("phone-node", WatchTileDataCodec.SYNC_REQUEST_PATH, ByteArray(0)) }
    }

    @Test
    fun `requestSync does nothing when no nodes connected`() = runTest {
        coEvery { nodeClient.connectedNodes } returns Tasks.forResult(emptyList())

        repository.requestSync()

        coVerify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }
}
