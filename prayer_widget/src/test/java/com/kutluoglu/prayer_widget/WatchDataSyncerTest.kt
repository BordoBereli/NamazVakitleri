package com.kutluoglu.prayer_widget

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_widget.data.WatchTileDataMapper
import com.kutluoglu.prayer_widget.data.WidgetData
import com.kutluoglu.prayer_widget.data.WidgetDataProvider
import com.kutluoglu.prayer_widget.data.WidgetResult
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchDataSyncerTest {

    private val dataProvider = mockk<WidgetDataProvider>()
    private val dataClient = mockk<DataClient>(relaxed = true)
    private val mapper = WatchTileDataMapper()

    private val sampleData = WidgetData(
        nextPrayerName = "İkindi",
        nextPrayerTime = "15:30",
        countdownText = "2s 14d",
        ringProgress = 0.5f,
        locationName = "İstanbul",
        gregorianDate = "16.09.2026",
        hijriDate = "4 Rebiülahir 1448",
        prayers = emptyList(),
        currentPrayerEpochMillis = 1_699_999_000_000L,
        nextPrayerEpochMillis = 1_700_000_000_000L
    )

    @Test
    fun `sync pushes urgent data item on success`() = runTest {
        coEvery { dataProvider.load() } returns WidgetResult.Success(sampleData)
        val requestSlot = slot<PutDataRequest>()
        every { dataClient.putDataItem(capture(requestSlot)) } returns mockk()

        val syncer = WatchDataSyncer(dataProvider, dataClient, mapper)
        syncer.sync()

        coVerify { dataProvider.load() }
        assertThat(requestSlot.captured.uri.path).isEqualTo(WatchTileDataCodec.PATH)
        assertThat(requestSlot.captured.isUrgent).isTrue()
    }

    @Test
    fun `sync does not push on error`() = runTest {
        coEvery { dataProvider.load() } returns WidgetResult.Error

        val syncer = WatchDataSyncer(dataProvider, dataClient, mapper)
        syncer.sync()

        coVerify(exactly = 0) { dataClient.putDataItem(any()) }
    }
}
