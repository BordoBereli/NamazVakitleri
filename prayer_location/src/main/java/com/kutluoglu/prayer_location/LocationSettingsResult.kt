package com.kutluoglu.prayer_location

import android.app.PendingIntent

sealed interface LocationSettingsResult {
    data object Satisfied : LocationSettingsResult
    data class ResolutionRequired(val resolution: PendingIntent?) : LocationSettingsResult
    data object Unavailable : LocationSettingsResult
}
