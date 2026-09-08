package com.kutluoglu.prayer_notifications.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.koin.core.annotation.Single

@Single(binds = [FcmClient::class])
class FirebaseMessagingClient(
    private val messaging: FirebaseMessaging
) : FcmClient {
    override suspend fun subscribe(topic: String) {
        messaging.subscribeToTopic(topic).await()
    }

    override suspend fun unsubscribe(topic: String) {
        messaging.unsubscribeFromTopic(topic).await()
    }
}