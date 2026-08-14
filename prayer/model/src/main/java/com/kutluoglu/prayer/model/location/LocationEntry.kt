package com.kutluoglu.prayer.model.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String
)
