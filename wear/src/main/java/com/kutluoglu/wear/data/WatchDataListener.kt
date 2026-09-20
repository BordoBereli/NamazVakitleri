package com.kutluoglu.wear.data

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives prayer-tile data items pushed by the phone app and persists them to the
 * local DataStore. The tile reads from the DataStore, so it works even when the
 * on-demand [DataClient.getDataItems] query returns nothing.
 */
class WatchDataListener(
    private val dataStore: TileDataStore
) : DataClient.OnDataChangedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val uri = event.dataItem.uri
            if (uri.path != WatchTileDataCodec.PATH) continue
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val data = WatchTileDataCodec.fromDataMap(dataMap) ?: continue
            scope.launch {
                dataStore.save(WatchTileDataCodec.toJson(data))
                Log.d("WatchDataListener", "Saved tile data from phone")
            }
        }
    }
}
