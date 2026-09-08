package com.kutluoglu.prayer_notifications.push

import android.util.Log
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import org.koin.core.annotation.Single

/**
 * Parses a data-only FCM payload and forwards valid announcements to the
 * [NotificationDisplayer]. Blank or missing title/body payloads are ignored.
 */
@Single
class PushMessageHandler(
    private val notificationDisplayer: NotificationDisplayer
) {
    fun handle(data: Map<String, String>) {
        val title = data["title"]?.trim().takeUnless { it.isNullOrEmpty() }
        val body = data["body"]?.trim().takeUnless { it.isNullOrEmpty() }
        if (title == null || body == null) {
            Log.w(TAG, "Ignoring push message: title or body missing/blank")
            return
        }
        val message = PushMessage(
            title = title,
            body = body,
            type = data["type"] ?: "announcement",
            channel = data["channel"] ?: "announcements",
            screen = data["screen"]
        )
        notificationDisplayer.showPushNotification(message.title, message.body)
    }

    private companion object {
        const val TAG = "PushMessageHandler"
    }
}