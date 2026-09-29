package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData

sealed interface GpsRefreshResult {
    data class Success(val location: LocationData) : GpsRefreshResult
    data class GpsDisabled(val settings: LocationSettingsResult) : GpsRefreshResult
    data object Unavailable : GpsRefreshResult
}
