package com.kutluoglu.prayer_location.data

import com.kutluoglu.prayer.model.location.LocationEntry

data class LocationsState(
    val entries: List<LocationEntry> = emptyList(),
    val gpsEnabled: Boolean = false,
    val selectedId: String? = null
)
