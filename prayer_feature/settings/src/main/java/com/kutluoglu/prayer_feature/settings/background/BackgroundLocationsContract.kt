package com.kutluoglu.prayer_feature.settings.background

data class BackgroundLocationUi(
    val id: String,
    val displayName: String,
    val backgroundPath: String?
)

data class BackgroundLocationsUiState(
    val locations: List<BackgroundLocationUi> = emptyList(),
    val savedBackgrounds: List<String> = emptyList()
)

sealed class BackgroundLocationsEvent {
    /** Sets [locationId]'s background to the existing file at [path]. */
    data class ApplySavedBackground(val locationId: String, val path: String) : BackgroundLocationsEvent()

    /** Clears [locationId]'s background path (the file is kept in the gallery). */
    data class ResetBackground(val locationId: String) : BackgroundLocationsEvent()

    /** Deletes the file at [path] and clears the path of any location referencing it. */
    data class DeleteBackground(val path: String) : BackgroundLocationsEvent()

    /**
     * Deletes the files at [paths] and clears the path of any location
     * referencing any of them.
     */
    data class DeleteBackgrounds(val paths: List<String>) : BackgroundLocationsEvent()

    /** Deletes every saved background file and clears the background of all locations. */
    data object DeleteAllBackgrounds : BackgroundLocationsEvent()
}
