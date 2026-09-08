package com.kutluoglu.prayer_notifications.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives FCM data messages and token refreshes. Thin shim: parsing lives in
 * [PushMessageHandler], topic management in [TopicSubscriptionManager].
 */
class PushMessagingService : FirebaseMessagingService(), KoinComponent {

    private val messageHandler: PushMessageHandler by inject()
    private val topicSubscriptionManager: TopicSubscriptionManager by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        messageHandler.handle(message.data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { topicSubscriptionManager.registerGlobal() }
                .onFailure { Log.e(TAG, "Failed to register global topic -> ${it.message}") }
        }
    }

    private companion object {
        const val TAG = "PushMessagingService"
    }
}