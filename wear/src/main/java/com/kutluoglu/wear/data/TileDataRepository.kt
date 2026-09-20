package com.kutluoglu.wear.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.kutluoglu.core.common.now
import com.kutluoglu.wear.R
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.LocalDateTime

class TileDataRepository(
    private val dataClient: DataClient,
    private val dataStore: TileDataStore,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val tileDataBuilder: WatchTileDataBuilder,
    private val locationProvider: WatchLocationProvider,
    private val settingsProvider: WatchSettingsProvider,
    private val context: Context
) {

    private var lastSyncRequestAt = 0L

    /**
     * Returns the tile data to render, preferring the freshest source available:
     * 1. the Google data layer ([DataClient]), 2. the local DataStore cache when
     * it is still current, 3. on-device computation as a last-resort fallback.
     *
     * The cache is treated as stale once its next prayer time has passed
     * ([WatchTileData.nextPrayerEpochMillis] < now), so on devices with a broken
     * data layer the local computation re-runs daily instead of serving
     * yesterday's prayer times forever.
     */
    suspend fun getTileData(): WatchTileData? {
        val fromClient = readFromDataClient()
        if (fromClient != null) {
            dataStore.save(WatchTileDataCodec.toJson(fromClient))
            return fromClient
        }
        val fromCache = dataStore.read()?.let { WatchTileDataCodec.fromJson(it) }
        if (fromCache != null && !isStale(fromCache)) return fromCache
        return computeLocally()
    }

    private fun isStale(data: WatchTileData): Boolean =
        data.nextPrayerEpochMillis < System.currentTimeMillis()

    /**
     * Computes tile data on-device as a last-resort fallback for devices where
     * the Google data layer is unavailable (e.g. some Samsung Galaxy Watches
     * report the wearable network as DISCONNECTED), so the tile always shows
     * prayer times without a phone round-trip. Best-effort: never throws, and
     * returns null only if computation genuinely fails. A cache-write failure
     * is logged but does not discard the successfully computed data.
     */
    private suspend fun computeLocally(): WatchTileData? {
        val data = runCatching {
            val location = locationProvider.getLocation()
            val settings = settingsProvider.getSettings()
            val date = LocalDateTime.now(location.zoneId)
            val prayerNames = context.resources.getStringArray(R.array.prayers).toList()
            tileDataBuilder.build(
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = location.zoneId,
                date = date,
                calculationMethod = settings.calculationMethod,
                juristicMethod = settings.juristicMethod,
                locationName = location.locationName,
                prayerNames = prayerNames
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Log.e(TAG, "computeLocally failed -> ${e.message}")
        }.getOrNull()

        if (data != null) {
            runCatching {
                dataStore.save(WatchTileDataCodec.toJson(data))
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "cache save failed -> ${e.message}")
            }
            Log.d(TAG, "computed locally -> location=${data.locationName} next=${data.nextPrayerName}")
        }
        return data
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
