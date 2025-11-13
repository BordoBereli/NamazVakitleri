package com.kutluoglu.prayer.usecases.location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.ILocationRepository
import org.koin.core.annotation.Factory

/**
 * Created by F.K. on 11.11.2025.
 *
 */

@Factory
class SaveLocationUseCase(
    private val repository: ILocationRepository
) {
    suspend operator fun invoke(locationData: LocationData) {
        repository.saveLocation(locationData)
    }
}