package com.kutluoglu.prayer_feature.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MyLocationsViewModel(
    private val locationsCoordinator: LocationsCoordinator
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
                locationsCoordinator.removeLocation(event.id)
            }
            is MyLocationsEvent.SetGpsEnabled -> viewModelScope.launch {
                locationsCoordinator.setGpsEnabled(event.enabled)
            }
            is MyLocationsEvent.SelectLocation -> viewModelScope.launch {
                locationsCoordinator.selectLocation(event.id)
            }
        }
    }
}
