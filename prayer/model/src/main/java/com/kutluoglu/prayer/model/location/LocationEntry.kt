package com.kutluoglu.prayer.model.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntry(
    val id: String,
    val location: LocationData,
    val isAutoGps: Boolean = false,
    val displayName: String,
    val displayNameTr: String? = null,
    val displayNameAr: String? = null,
    val displayNameFa: String? = null
)
