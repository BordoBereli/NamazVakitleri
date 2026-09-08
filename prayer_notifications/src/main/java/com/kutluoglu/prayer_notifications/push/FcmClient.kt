package com.kutluoglu.prayer_notifications.push

/**
 * Thin seam over Firebase Cloud Messaging topic APIs so that topic
 * subscription logic can be unit tested without a FirebaseApp.
 */
interface FcmClient {
    suspend fun subscribe(topic: String)
    suspend fun unsubscribe(topic: String)
}