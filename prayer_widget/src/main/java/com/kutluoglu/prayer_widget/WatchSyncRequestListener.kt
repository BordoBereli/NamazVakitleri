package com.kutluoglu.prayer_widget

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

/**
 * Runtime-registered message listener that re-pushes fresh tile data when the
 * watch requests a sync. Registered directly with Google Play services at
 * runtime, bypassing the manifest-declared [WatchDataSyncListenerService] path
 * (GMS only routes listener services for Play Store / OEM apps, so sideloaded
 * installs never receive messages through it). Complements that service.
 */
@Factory
class WatchSyncRequestListener(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : MessageClient.OnMessageReceivedListener {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchTileDataCodec.SYNC_REQUEST_PATH) return
        Log.d(TAG, "Sync request received from watch, re-pushing tile data")
        scope.launch { WatchDataSync.sync() }
    }

    private companion object {
        const val TAG = "WatchSyncRequestListener"
    }
}
