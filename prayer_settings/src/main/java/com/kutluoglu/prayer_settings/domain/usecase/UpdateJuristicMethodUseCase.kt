package com.kutluoglu.prayer_settings.domain.usecase

import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import org.koin.core.annotation.Factory

@Factory
class UpdateJuristicMethodUseCase(
    private val dataStore: SettingsDataStore
) {
    suspend operator fun invoke(method: String) {
        dataStore.updateJuristicMethod(method)
    }
}
