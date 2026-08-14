package com.kutluoglu.prayer_feature.settings.location

sealed class MyLocationsEvent {
    data class RemoveLocation(val id: String) : MyLocationsEvent()
    data class SetGpsEnabled(val enabled: Boolean) : MyLocationsEvent()
    data class SelectLocation(val id: String) : MyLocationsEvent()
    data class ReorderLocations(val ids: List<String>) : MyLocationsEvent()
}
