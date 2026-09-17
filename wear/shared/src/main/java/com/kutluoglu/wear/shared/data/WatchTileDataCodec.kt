package com.kutluoglu.wear.shared.data

import com.google.android.gms.wearable.DataMap
import com.kutluoglu.wear.shared.model.WatchTileData
import kotlinx.serialization.json.Json

object WatchTileDataCodec {

    const val PATH = "/prayer-tile"

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
