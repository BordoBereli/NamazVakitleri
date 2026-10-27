package com.kutluoglu.prayer_feature.settings.background

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        refresh()
    }

    fun onEvent(event: BackgroundLocationsEvent) {
        when (event) {
            is BackgroundLocationsEvent.ApplySavedBackground -> viewModelScope.launch {
                locationsCoordinator.setBackgroundPath(event.locationId, event.path)
                refresh()
            }
            is BackgroundLocationsEvent.ResetBackground -> viewModelScope.launch {
                locationsCoordinator.setBackgroundPath(event.locationId, null)
                refresh()
            }
            is BackgroundLocationsEvent.DeleteBackground -> viewModelScope.launch {
                deletePaths(listOf(event.path))
            }
            is BackgroundLocationsEvent.DeleteBackgrounds -> viewModelScope.launch {
                deletePaths(event.paths)
            }
            BackgroundLocationsEvent.DeleteAllBackgrounds -> viewModelScope.launch {
                // The GPS entry is cleared unconditionally: it may not be part
                // of the visible list (GPS disabled) while still holding a path.
                val referencingIds = _uiState.value.locations
                    .filter { it.backgroundPath != null }
                    .map { it.id }
                    .toSet() + LocationsCoordinator.GPS_LOCATION_ID
                withContext(Dispatchers.IO) {
                    backgroundImageStore.deleteAllBackgrounds()
                }
                referencingIds.forEach { id ->
                    locationsCoordinator.setBackgroundPath(id, null)
                }
                refresh()
            }
        }
    }

    /**
     * Deletes [paths] from the gallery and clears the background path of any
     * location referencing one of them.
     */
    private suspend fun deletePaths(paths: List<String>) {
        val referencingIds = _uiState.value.locations
            .filter { it.backgroundPath in paths }
            .map { it.id }
        withContext(Dispatchers.IO) {
            paths.forEach { backgroundImageStore.deleteBackground(it) }
        }
        referencingIds.forEach { id ->
            locationsCoordinator.setBackgroundPath(id, null)
        }
        refresh()
    }

    /**
     * Re-reads the saved-backgrounds gallery from disk. Called on init and
     * from the screen on lifecycle ON_START so the gallery reflects files
     * saved on the adjust screen while this screen was off-screen.
     */
    fun refresh() {
        viewModelScope.launch {
            val paths = withContext(Dispatchers.IO) {
                backgroundImageStore.listBackgrounds().map { it.absolutePath }
            }
            _uiState.update { current -> current.copy(savedBackgrounds = paths) }
        }
    }

    private fun List<LocationEntry>.toUi(): List<BackgroundLocationUi> =
        map {
            BackgroundLocationUi(
                id = it.id,
                displayName = it.displayName,
                backgroundPath = it.backgroundPath
            )
        }
}
