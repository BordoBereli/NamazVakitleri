package com.kutluoglu.prayer.data.source

import com.kutluoglu.prayer.data.source.location.LocationDataStore
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class DataStoreFactory(
    private val locationDataStore: LocationDataStore
) {
    fun retrieveLocationDataStore() = locationDataStore
}