package com.kutluoglu.namazvakitleri.analytics

import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.core.common.analytics.AnalyticsUserProperties
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Observes settings and locations and mirrors them onto analytics user properties
 * so every event can be segmented by language, calculation method, location setup, etc.
 */
class AnalyticsUserPropertiesManager(
    private val analyticsTracker: AnalyticsTracker,
    private val settingsRepository: SettingsRepository,
    private val locationsCoordinator: LocationsCoordinator
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.observeSettings().collect { settings ->
                analyticsTracker.setUserProperty(AnalyticsUserProperties.LANGUAGE, settings.language)
                analyticsTracker.setUserProperty(AnalyticsUserProperties.CALCULATION_METHOD, settings.calculationMethod)
                analyticsTracker.setUserProperty(AnalyticsUserProperties.HIJRI_ADJUSTMENT, settings.hijriAdjustment.toString())
            }
        }
        scope.launch {
            locationsCoordinator.observeState().collect { state ->
                analyticsTracker.setUserProperty(AnalyticsUserProperties.LOCATION_COUNT, state.entries.size.toString())
                analyticsTracker.setUserProperty(AnalyticsUserProperties.GPS_ENABLED, state.gpsEnabled.toString())
                val active = state.entries.firstOrNull { it.id == state.selectedId }
                analyticsTracker.setUserProperty(
                    AnalyticsUserProperties.ACTIVE_LOCATION_TYPE,
                    if (active?.isAutoGps == true) "gps" else "manual"
                )
            }
        }
    }
}
