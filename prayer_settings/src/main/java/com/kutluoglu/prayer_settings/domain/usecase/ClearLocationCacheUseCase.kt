package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import org.koin.core.annotation.Factory

@Factory
class ClearLocationCacheUseCase(
    private val locationRepository: LocationRepository? = null
) {
    suspend operator fun invoke() {
        locationRepository?.clearCache()
    }
}
