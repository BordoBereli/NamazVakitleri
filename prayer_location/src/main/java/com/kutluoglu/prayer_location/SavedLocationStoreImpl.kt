package com.kutluoglu.prayer_location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationDataStore
import com.kutluoglu.prayer_location.data.legacy.LegacyLocationMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Backs [SavedLocationStore] with the legacy single-location persistence store.
 * Kept for migration/back-compat; the modern multi-location store
 * ([com.kutluoglu.prayer_location.data.LocationsDataStore]) is the source of
 * truth for the active location going forward.
 */
@Single(binds = [SavedLocationStore::class])
class SavedLocationStoreImpl(
    private val legacyStore: LegacyLocationDataStore,
    private val mapper: LegacyLocationMapper
) : SavedLocationStore {

    override suspend fun saveLocation(location: LocationData) {
        legacyStore.saveLocation(mapper.mapFromDomain(location))
    }

    override suspend fun getSavedLocation(): LocationData? =
        legacyStore.getSavedLocation()?.let(mapper::mapToDomain)

    override fun observeLocation(): Flow<LocationData?> =
        legacyStore.observeLocation().map { model -> model?.let(mapper::mapToDomain) }
}
