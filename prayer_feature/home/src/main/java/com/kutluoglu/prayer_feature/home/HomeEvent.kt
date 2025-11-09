package com.kutluoglu.prayer_feature.home

/**
 * A sealed interface to represent user-initiated events on the Home screen.
 *
 */
sealed interface HomeEvent {
    object OnRefresh : HomeEvent
    object OnCountDown : HomeEvent
    object OnPermissionsGranted : HomeEvent
    object OnUpdateLocationConfirmed : HomeEvent
    object OnLoadQuranVerse : HomeEvent
    object OnVerseClicked : HomeEvent
    object OnVerseDetailDismissed : HomeEvent
}