package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateCompassAutoRotateUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(compassAutoRotate: Boolean) {
        repository.updateCompassAutoRotate(compassAutoRotate)
    }
}
