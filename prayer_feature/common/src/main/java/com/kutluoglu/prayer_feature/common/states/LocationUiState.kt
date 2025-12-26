package com.kutluoglu.prayer_feature.common.states

import com.kutluoglu.prayer.model.location.LocationData

data class LocationUiState(
        val locationData: LocationData,
        val locationInfoText: String
)