package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateHijriAdjustmentUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(days: Int) {
        repository.updateHijriAdjustment(days)
    }
}
