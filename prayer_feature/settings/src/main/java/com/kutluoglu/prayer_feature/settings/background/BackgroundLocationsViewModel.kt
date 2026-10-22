package com.kutluoglu.prayer_feature.settings.background

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class BackgroundLocationsViewModel(
    private val locationsCoordinator: LocationsCoordinator,
    private val backgroundImageStore: BackgroundImageStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackgroundLocationsUiState())
    val uiState: StateFlow<BackgroundLocationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationsCoordinator.observeState().collect { state ->
                _uiState.update { current ->
                    current.copy(locations = state.entries.toUi())
                }
            }
        }
        refreshSavedBackgrounds()
    }

    fun onEvent(event: BackgroundLocationsEvent) {
        when (event) {
            is BackgroundLocationsEvent.PickImage -> Unit // handled screen-internal (photo picker)
            is BackgroundLocationsEvent.ApplySavedBackground -> viewModelScope.launch {
                locationsCoordinator.setBackgroundPath(event.locationId, event.path)
                refreshSavedBackgrounds()
            }
            is BackgroundLocationsEvent.ResetBackground -> viewModelScope.launch {
                locationsCoordinator.setBackgroundPath(event.locationId, null)
                refreshSavedBackgrounds()
            }
            is BackgroundLocationsEvent.DeleteBackground -> viewModelScope.launch {
                val referencingIds = _uiState.value.locations
                    .filter { it.backgroundPath == event.path }
                    .map { it.id }
                backgroundImageStore.deleteBackground(event.path)
                referencingIds.forEach { id ->
                    locationsCoordinator.setBackgroundPath(id, null)
                }
                refreshSavedBackgrounds()
            }
        }
    }

    private fun refreshSavedBackgrounds() {
        viewModelScope.launch {
            val paths = backgroundImageStore.listBackgrounds().map { it.absolutePath }
            _uiState.update { current -> current.copy(savedBackgrounds = paths) }
        }
    }

    private fun List<LocationEntry>.toUi(): List<BackgroundLocationUi> =
        filter { it.id != LocationsCoordinator.GPS_LOCATION_ID }
            .map {
                BackgroundLocationUi(
                    id = it.id,
                    displayName = it.displayName,
                    backgroundPath = it.backgroundPath
                )
            }
}
