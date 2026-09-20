package com.kutluoglu.prayer_widget

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives sync-request messages from the watch tile and re-pushes fresh tile
 * data via [WatchDataSync]. Registered in the manifest so Google Play services
 * can start it even when the app is in the background.
 */
class WatchDataSyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchTileDataCodec.SYNC_REQUEST_PATH) return
        Log.d(TAG, "Sync request received from watch, re-pushing tile data")
        scope.launch { WatchDataSync.sync() }
    }

    private companion object {
        const val TAG = "WatchDataSyncListener"
    }
}
