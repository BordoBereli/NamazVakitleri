package com.kutluoglu.wear.data

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.wear.R
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId

class TileDataRepositoryTest {

    private val dataClient = mockk<DataClient>(relaxed = true)
    private val dataStore = mockk<TileDataStore>(relaxed = true)
    private val messageClient = mockk<MessageClient>(relaxed = true)
    private val nodeClient = mockk<NodeClient>(relaxed = true)
    private val tileDataBuilder = mockk<WatchTileDataBuilder>(relaxed = true)
    private val locationProvider = mockk<WatchLocationProvider>(relaxed = true)
    private val settingsProvider = mockk<WatchSettingsProvider>(relaxed = true)
    private val resources = mockk<Resources>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val repository = TileDataRepository(
        dataClient,
        dataStore,
        messageClient,
        nodeClient,
        tileDataBuilder,
        locationProvider,
        settingsProvider,
        context
    )

    private val sampleData = WatchTileData(
        locationName = "İstanbul",
        nextPrayerName = "Akşam",
        nextPrayerEpochMillis = 4_100_000_000_000,
        currentPrayerEpochMillis = 4_099_999_000_000,
        isJumuah = false,
        prayers = listOf(WatchPrayer(name = "Öğle", time = "13:00", isNext = false)),
        syncedAtEpochMillis = 4_100_000_000_000
    )

    @BeforeEach
    fun setUp() {
        every { context.resources } returns resources
        every { resources.getStringArray(R.array.prayers) } returns
            arrayOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
        every { locationProvider.getLocation() } returns WatchLocation(
            latitude = 41.0082,
            longitude = 28.9784,
            zoneId = ZoneId.of("Europe/Istanbul"),
            locationName = "İstanbul"
        )
        coEvery { settingsProvider.getSettings() } returns WatchSettings(
            calculationMethod = CalculationMethod.TURKEY_DIYANET,
            juristicMethod = JuristicMethod.STANDARD
        )
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
    }

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
    fun `does not compute locally when data client returns data`() = runTest {
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
        verify(exactly = 0) { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) }
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
    fun `recomputes locally when cached data is stale`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        val staleData = sampleData.copy(nextPrayerEpochMillis = 1_000_000_000_000)
        coEvery { dataStore.read() } returns WatchTileDataCodec.toJson(staleData)
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } returns sampleData

        val result = repository.getTileData()

        assertThat(result).isEqualTo(sampleData)
        verify { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `computes locally when data client and cache are empty`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        coEvery { dataStore.read() } returns null
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } returns sampleData

        val result = repository.getTileData()

        assertThat(result).isEqualTo(sampleData)
        coVerify { dataStore.save(WatchTileDataCodec.toJson(sampleData)) }
        verify {
            tileDataBuilder.build(
                latitude = 41.0082,
                longitude = 28.9784,
                zoneId = ZoneId.of("Europe/Istanbul"),
                date = any(),
                calculationMethod = CalculationMethod.TURKEY_DIYANET,
                juristicMethod = JuristicMethod.STANDARD,
                locationName = "İstanbul",
                prayerNames = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
            )
        }
    }

    @Test
    fun `serves locally computed result from cache on subsequent call`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        var cachedJson: String? = null
        coEvery { dataStore.read() } answers { cachedJson }
        coEvery { dataStore.save(any()) } answers { cachedJson = firstArg() }
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } returns sampleData

        val first = repository.getTileData()
        val second = repository.getTileData()

        assertThat(first).isEqualTo(sampleData)
        assertThat(second).isEqualTo(sampleData)
        verify(exactly = 1) { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `returns null when local computation fails`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        coEvery { dataStore.read() } returns null
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } throws
            RuntimeException("boom")

        val result = repository.getTileData()

        assertThat(result).isNull()
        coVerify(exactly = 0) { dataStore.save(any()) }
    }

    @Test
    fun `returns computed data even when cache save fails`() = runTest {
        val buffer = mockk<DataItemBuffer>(relaxed = true)
        every { buffer.count } returns 0
        coEvery { dataClient.getDataItems(any(), any()) } returns Tasks.forResult(buffer)
        coEvery { dataStore.read() } returns null
        every { tileDataBuilder.build(any(), any(), any(), any(), any(), any(), any(), any()) } returns sampleData
        coEvery { dataStore.save(any()) } throws RuntimeException("io error")

        val result = repository.getTileData()

        assertThat(result).isEqualTo(sampleData)
    }

    @Test
    fun `returns null when client empty and no cache and no local data`() = runTest {
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
