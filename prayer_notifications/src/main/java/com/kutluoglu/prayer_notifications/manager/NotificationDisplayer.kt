package com.kutluoglu.prayer_notifications.manager

import android.app.Notification
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay

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
}
