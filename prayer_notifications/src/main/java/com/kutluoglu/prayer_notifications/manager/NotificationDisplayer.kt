package com.kutluoglu.prayer_notifications.manager

import android.app.Notification
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import androidx.media3.session.MediaSession

/**
 * Contract for anything that can build and post system notifications.
 *
 * This is the single seam through which alarm/scheduling code interacts with the
 * notification display concern, so that alarm scheduling never depends on the
 * concrete notification manager.
 */
interface NotificationDisplayer {

    companion object {
        const val NOTIFICATION_ID_ADHAN = 1009
    }

    fun createChannels(settings: NotificationSettings = NotificationSettings())

    fun showPrayerNotification(prayerName: String, settings: NotificationSettings)

    fun buildAdhanNotification(prayerName: String): Notification

    /**
     * Builds the adhan foreground notification with an optional active [MediaSession].
     *
     * When a session is provided the notification is rendered as a media-style notification
     * (transport controls, CATEGORY_TRANSPORT, ongoing) so the system UI recognizes it as
     * an active media session.
     */
    fun buildAdhanNotification(prayerName: String, mediaSession: MediaSession?): Notification

    /**
     * Posts (or re-posts) the adhan foreground notification, optionally media-styled when
     * an active [MediaSession] is provided.
     */
    fun showAdhanNotification(prayerName: String, mediaSession: MediaSession? = null)

    fun showCountdownNotification(
        nextPrayerName: String,
        nextPrayerTimeMillis: Long,
        previousPrayerTimeMillis: Long?,
        remainingMillis: Long
    )

    fun cancelCountdown()

    fun showTestNotification()

    fun showJumuahNotification()

    fun showPrePrayerNotification(prayerName: String, minutes: Int)

    fun showDailyReminderNotification(summary: String)

    fun showSpecialDayNotification(day: SpecialDay)

    fun showPreSpecialDayNotification(day: SpecialDay)

    fun showSahurEndNotification()

    fun showIftarNotification()

    fun showPushNotification(title: String, body: String)
}
