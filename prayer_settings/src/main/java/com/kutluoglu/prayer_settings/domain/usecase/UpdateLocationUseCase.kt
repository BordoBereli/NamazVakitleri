package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateLocationUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(location: LocationSettings) {
        repository.updateLocation(location)
    }
}
