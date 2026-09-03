package com.kutluoglu.prayer_widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
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
 * Observes settings and location changes that affect the widget's displayed data and
 * refreshes all widget instances whenever they change.
 *
 * Without this, changing the location, calculation method, juristic method, hijri
 * adjustment, or language only updates the in-app UI while the home-screen widget keeps
 * showing stale data until its next periodic refresh.
 */
@OptIn(FlowPreview::class)
class WidgetRefresher(
    private val settingsRepository: SettingsRepository,
    private val locationsCoordinator: LocationsCoordinator,
    private val refreshWidgets: suspend () -> Unit,
    private val debounceMillis: Long = 500
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.observeSettings()
                .map { SettingsKey(it.location, it.calculationMethod, it.juristicMethod, it.hijriAdjustment, it.language) }
                .distinctUntilChanged()
                .drop(1)
                .debounce(debounceMillis)
                .collect { refreshWidgets() }
        }
        scope.launch {
            locationsCoordinator.observeState()
                .distinctUntilChanged()
                .drop(1)
                .debounce(debounceMillis)
                .collect { refreshWidgets() }
        }
    }

    companion object {
        fun create(
            settingsRepository: SettingsRepository,
            locationsCoordinator: LocationsCoordinator,
            context: Context
        ): WidgetRefresher = WidgetRefresher(
            settingsRepository = settingsRepository,
            locationsCoordinator = locationsCoordinator,
            refreshWidgets = { PrayerWidget().updateAll(context) }
        )
    }

    private data class SettingsKey(
        val location: LocationSettings,
        val calculationMethod: String,
        val juristicMethod: String,
        val hijriAdjustment: Int,
        val language: String
    )
}
