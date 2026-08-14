package com.kutluoglu.prayer_feature.home

/**
 * A sealed interface to represent user-initiated events on the Home screen.
 *
 */
sealed interface HomeEvent {
    object OnRefresh : HomeEvent
    object OnPermissionsGranted : HomeEvent
    object OnUpdateLocationConfirmed : HomeEvent
    object OnLoadQuranVerse : HomeEvent
    object OnVerseClicked : HomeEvent
    object OnVerseDetailDismissed : HomeEvent
    data class OnLocationSelected(val locationId: String) : HomeEvent
}