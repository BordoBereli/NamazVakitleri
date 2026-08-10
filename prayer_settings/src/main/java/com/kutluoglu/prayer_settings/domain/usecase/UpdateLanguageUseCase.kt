package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateLanguageUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(language: String) {
        repository.updateLanguage(language)
    }
}
