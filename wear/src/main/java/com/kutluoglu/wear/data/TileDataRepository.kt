package com.kutluoglu.wear.data

import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class TileDataRepository(
    private val dataClient: DataClient,
    private val dataStore: TileDataStore
) {

    suspend fun getTileData(): WatchTileData? {
        val fromClient = readFromDataClient()
        if (fromClient != null) {
            dataStore.save(WatchTileDataCodec.toJson(fromClient))
            return fromClient
        }
        return dataStore.read()?.let { WatchTileDataCodec.fromJson(it) }
    }

    private suspend fun readFromDataClient(): WatchTileData? {
        val uri = Uri.parse("wear://*${WatchTileDataCodec.PATH}")
        return try {
            val buffer = dataClient.getDataItems(uri, DataClient.FILTER_PREFIX).await()
            buffer.use {
                if (it.count == 0) return null
                val dataMap = DataMapItem.fromDataItem(it[0]).dataMap
                WatchTileDataCodec.fromDataMap(dataMap)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
