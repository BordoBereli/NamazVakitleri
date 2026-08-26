package com.kutluoglu.prayer_feature.home.state

sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Empty : HomeScreenGate
    data object Ready : HomeScreenGate
}
