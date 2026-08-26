package com.kutluoglu.namazvakitleri.notifications

import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationScheduler
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Observes settings and location changes that affect prayer times and reschedules the
 * notification alarms (including the countdown notification) whenever they change.
 *
 * Without this, changing the location, calculation method, or timezone only refreshes the
 * in-app countdown while the countdown notification keeps targeting the old prayer time.
 */
@OptIn(FlowPreview::class)
class NotificationRescheduler(
    private val scheduler: PrayerNotificationScheduler,
    private val settingsRepository: SettingsRepository,
    private val locationsCoordinator: LocationsCoordinator,
    private val debounceMillis: Long = 500
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.observeSettings()
                .map { Triple(it.location, it.calculationMethod, it.hijriAdjustment) }
                .distinctUntilChanged()
                .drop(1)
                .debounce(debounceMillis)
                .collect { scheduler.scheduleAll() }
        }
        scope.launch {
            locationsCoordinator.observeState()
                .distinctUntilChanged()
                .drop(1)
                .debounce(debounceMillis)
                .collect { scheduler.scheduleAll() }
        }
    }
}
