package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import org.koin.core.annotation.Factory

@Factory
class CancelNotificationsUseCase(
    private val scheduler: PrayerNotificationScheduler
) {
    operator fun invoke() = scheduler.cancelAll()
}
