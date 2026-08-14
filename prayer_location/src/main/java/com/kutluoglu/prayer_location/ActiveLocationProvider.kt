package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class ActiveLocationProvider {
    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location

    fun set(location: LocationData?) {
        _location.value = location
    }
}
