package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateThemeModeUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: String) {
        repository.updateThemeMode(mode)
    }
}
