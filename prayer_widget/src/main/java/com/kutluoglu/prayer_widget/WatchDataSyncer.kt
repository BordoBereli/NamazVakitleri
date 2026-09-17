package com.kutluoglu.prayer_widget

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.kutluoglu.prayer_widget.data.WatchTileDataMapper
import com.kutluoglu.prayer_widget.data.WidgetDataProvider
import com.kutluoglu.prayer_widget.data.WidgetResult
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import kotlinx.coroutines.tasks.await
import org.koin.core.annotation.Factory

@Factory
class WatchDataSyncer(
    private val dataProvider: WidgetDataProvider,
    private val dataClient: DataClient,
    private val mapper: WatchTileDataMapper
) {
    suspend fun sync() {
        val result = dataProvider.load()
        if (result is WidgetResult.Success) {
            val dataMap = WatchTileDataCodec.toDataMap(mapper.map(result.data))
            val request = PutDataMapRequest.create(WatchTileDataCodec.PATH)
                .apply { this.dataMap.putAll(dataMap) }
                .asPutDataRequest()
                .setUrgent()
            runCatching { dataClient.putDataItem(request).await() }.onFailure {
                Log.e("WatchDataSyncer", "Failed to push watch data -> ${it.message}")
            }
        }
    }
}
