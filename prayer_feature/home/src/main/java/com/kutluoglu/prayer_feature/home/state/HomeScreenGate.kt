package com.kutluoglu.prayer_feature.home.state

import android.app.PendingIntent

sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Empty : HomeScreenGate
    data object Ready : HomeScreenGate
    data class GpsDisabled(val resolution: PendingIntent? = null) : HomeScreenGate
}
