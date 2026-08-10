package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): Settings {
        return repository.getSettings()
    }
}
