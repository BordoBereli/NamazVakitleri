package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class CancelNotificationsUseCase(
    private val scheduler: PrayerNotificationScheduler
) {
    fun invoke() = scheduler.cancelAll()
}
