package com.kutluoglu.prayer_feature.home

/**
 * A sealed class to represent user-initiated events on the Home screen.
 */
sealed class HomeEvent {
    object OnRefresh : HomeEvent()
    object OnCountDown : HomeEvent()
}