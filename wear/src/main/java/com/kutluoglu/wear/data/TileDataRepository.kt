package com.kutluoglu.wear.data

import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class TileDataRepository(
    private val dataClient: DataClient,
    private val dataStore: TileDataStore,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient
) {

    private var lastSyncRequestAt = 0L

    suspend fun getTileData(): WatchTileData? {
        val fromClient = readFromDataClient()
        if (fromClient != null) {
            dataStore.save(WatchTileDataCodec.toJson(fromClient))
            return fromClient
        }
        return dataStore.read()?.let { WatchTileDataCodec.fromJson(it) }
    }

    /**
     * Asks the phone to push fresh tile data. Best-effort: on devices where the
     * Google data layer connection is unavailable (e.g. some Samsung Galaxy
     * Watches report the wearable network as DISCONNECTED), the message simply
     * fails and the tile keeps using the local cache. Throttled to avoid
     * spamming the phone on every tile request.
     */
    suspend fun requestSync() {
        val now = System.currentTimeMillis()
        if (now - lastSyncRequestAt < SYNC_REQUEST_THROTTLE_MILLIS) return
        lastSyncRequestAt = now
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
        Log.d(TAG, "requestSync: connectedNodes=${nodes.map { it.id }}")
        val phoneNode = nodes.firstOrNull()
        if (phoneNode == null) {
            Log.d(TAG, "requestSync: no phone node to send to")
            return
        }
        runCatching {
            messageClient.sendMessage(phoneNode.id, WatchTileDataCodec.SYNC_REQUEST_PATH, ByteArray(0)).await()
        }.onFailure {
            Log.e(TAG, "requestSync failed -> ${it.message}")
        }
    }

    private suspend fun readFromDataClient(): WatchTileData? {
        val uri = Uri.parse("wear://*${WatchTileDataCodec.PATH}")
        return try {
            val buffer = dataClient.getDataItems(uri, DataClient.FILTER_PREFIX).await()
            buffer.use {
                Log.d(TAG, "readFromDataClient: getDataItems count=${it.count}")
                if (it.count == 0) return null
                val dataMap = DataMapItem.fromDataItem(it[0]).dataMap
                WatchTileDataCodec.fromDataMap(dataMap)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "readFromDataClient failed -> ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "TileDataRepository"
        const val SYNC_REQUEST_THROTTLE_MILLIS = 5 * 60 * 1000L
    }
}
