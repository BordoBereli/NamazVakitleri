package com.kutluoglu.prayer_feature.home.state

import android.app.PendingIntent
import androidx.annotation.StringRes

sealed interface HomeScreenGate {
    data object Loading : HomeScreenGate
    data class Error(val message: String) : HomeScreenGate
    data object Empty : HomeScreenGate
    data object Ready : HomeScreenGate
    data class GpsDisabled(val resolution: PendingIntent? = null) : HomeScreenGate
    data class Locating(@StringRes val messageRes: Int) : HomeScreenGate
}
