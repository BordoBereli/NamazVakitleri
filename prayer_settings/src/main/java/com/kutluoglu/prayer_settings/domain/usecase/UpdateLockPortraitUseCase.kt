package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateLockPortraitUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(lockPortrait: Boolean) {
        repository.updateLockPortrait(lockPortrait)
    }
}
