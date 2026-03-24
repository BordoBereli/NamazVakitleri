package com.kutluoglu.prayer.usecases.location

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveLocationUseCase(
    private val repository: LocationRepository
) {
    operator fun invoke(): Flow<LocationData> = repository.observeLocation()
}