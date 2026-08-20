package com.kutluoglu.prayer_feature.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.common.analytics.AnalyticsEvents
import com.kutluoglu.core.common.analytics.AnalyticsParams
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MyLocationsViewModel(
    private val locationsCoordinator: LocationsCoordinator,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationsState>(LocationsState())
    val uiState: StateFlow<LocationsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationsCoordinator.observeState().collect { _uiState.value = it }
        }
    }

    fun onEvent(event: MyLocationsEvent) {
        when (event) {
            is MyLocationsEvent.RemoveLocation -> viewModelScope.launch {
                analyticsTracker.logEvent(
                    AnalyticsEvents.LOCATION_REMOVED,
                    mapOf(AnalyticsParams.LOCATION_ID to event.id)
                )
                locationsCoordinator.removeLocation(event.id)
            }
            is MyLocationsEvent.SetGpsEnabled -> viewModelScope.launch {
                analyticsTracker.logEvent(
                    AnalyticsEvents.GPS_TOGGLED,
                    mapOf(AnalyticsParams.ENABLED to event.enabled)
                )
                locationsCoordinator.setGpsEnabled(event.enabled)
                if (event.enabled) {
                    locationsCoordinator.refreshGps()
                }
            }
            is MyLocationsEvent.SelectLocation -> viewModelScope.launch {
                analyticsTracker.logEvent(
                    AnalyticsEvents.LOCATION_SELECTED,
                    mapOf(AnalyticsParams.LOCATION_ID to event.id)
                )
                locationsCoordinator.selectLocation(event.id)
            }
            is MyLocationsEvent.ReorderLocations -> viewModelScope.launch {
                analyticsTracker.logEvent(AnalyticsEvents.LOCATION_REORDERED)
                locationsCoordinator.reorderLocations(event.ids)
            }
        }
    }
}
