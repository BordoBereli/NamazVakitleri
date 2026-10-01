package com.kutluoglu.prayer_notifications.scheduler

/**
 * Contract for scheduling and cancelling Android alarms that wake the app.
 *
 * This isolates the alarm scheduling concern from notification display, so that
 * scheduling code never post notifications directly and notification code never
 * talks to [android.app.AlarmManager].
 */
interface AlarmScheduler {

    fun scheduleAll()

    suspend fun scheduleAllSuspending()

    fun cancelAll(stopAdhan: Boolean = true)

    fun cancelDailyReschedule()

    /**
     * Cancels the countdown notification.
     */
    fun cancelCountdown()

    /**
     * Shows/updates the countdown notification (via the notification displayer).
     * The system chronometer ticks the remaining time; no alarm re-arming needed.
     */
    fun updateCountdown(
        targetMillis: Long,
        prayerName: String,
        previousTimeMillis: Long? = null
    )

    suspend fun scheduleDailyReminder()

    /**
     * Schedules a single debug "Test Adhan" prayer alarm to fire [delayMinutes] from
     * now (clamped to 0..15). Scheduling again cancels any previously scheduled test
     * alarm. Uses the exact production PRAYER alarm path, so playback runs through
     * [com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver] and respects the
     * current adhan-enabled setting. Skips scheduling (logs a warning) when the
     * master notifications-enabled setting is off.
     */
    fun scheduleTestAdhan(delayMinutes: Int)
}
