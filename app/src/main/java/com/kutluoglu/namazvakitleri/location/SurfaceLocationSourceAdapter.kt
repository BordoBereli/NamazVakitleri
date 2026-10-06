package com.kutluoglu.namazvakitleri.location

import com.kutluoglu.prayer.domain.SurfaceLocationSource
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_location.LocationsCoordinator

/**
 * Composition-root implementation of [SurfaceLocationSource] that delegates to
 * [prayer_location]'s [LocationsCoordinator]. This is what lets `:prayer:domain`
 * resolve the selected location without depending on `prayer_location`.
 */
class SurfaceLocationSourceAdapter(
    private val locationsCoordinator: LocationsCoordinator
) : SurfaceLocationSource {

    override suspend fun resolveSelected(): LocationData? = locationsCoordinator.resolveSelected()
}
