package com.kutluoglu.wear.shared.data

import com.google.android.gms.wearable.DataMap
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.serialization.json.Json

object WatchTileDataCodec {

    const val PATH = "/prayer-tile"

    /**
     * Message path the watch uses to ask the phone to push fresh tile data.
     * The phone listens via a WearableListenerService and re-runs the watch
     * data sync on receipt.
     */
    const val SYNC_REQUEST_PATH = "/prayer-tile-sync-request"

    private const val KEY_PAYLOAD = "payload"

    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(data: WatchTileData): String = json.encodeToString(WatchTileData.serializer(), data)

    fun fromJson(jsonString: String): WatchTileData? =
        runCatching { json.decodeFromString(WatchTileData.serializer(), jsonString) }.getOrNull()

    fun toDataMap(data: WatchTileData): DataMap =
        DataMap().apply { putString(KEY_PAYLOAD, toJson(data)) }

    fun fromDataMap(dataMap: DataMap): WatchTileData? =
        dataMap.getString(KEY_PAYLOAD)?.let { fromJson(it) }
}
