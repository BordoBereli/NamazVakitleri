package com.kutluoglu.prayer.data.source

import com.kutluoglu.prayer.data.source.location.LocationDataStoreImp
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Single
class DataStoreFactory(
    private val locationDataStore: LocationDataStoreImp
) {
    fun retrieveLocationDataStore() = locationDataStore
}