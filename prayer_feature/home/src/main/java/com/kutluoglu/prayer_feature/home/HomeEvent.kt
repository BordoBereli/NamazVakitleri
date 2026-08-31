package com.kutluoglu.prayer_feature.home

/**
 * A sealed interface to represent user-initiated events on the Home screen.
 *
 */
sealed interface HomeEvent {
    object OnRefresh : HomeEvent
    object OnPermissionsGranted : HomeEvent
    object OnUseMyLocation : HomeEvent
    object OnLoadQuranVerse : HomeEvent
    object OnVerseClicked : HomeEvent
    object OnVerseDetailDismissed : HomeEvent
    object OnToggleVerseSaved : HomeEvent
    data class OnLocationSelected(val locationId: String) : HomeEvent
}
