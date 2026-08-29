package com.kutluoglu.prayer_notifications.domain.usecases

import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import org.koin.core.annotation.Factory

@Factory
class CancelNotificationsUseCase(
    private val scheduler: AlarmScheduler
) {
    operator fun invoke() = scheduler.cancelAll()
}
