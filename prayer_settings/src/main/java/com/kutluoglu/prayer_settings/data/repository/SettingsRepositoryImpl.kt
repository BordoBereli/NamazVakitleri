package com.kutluoglu.prayer_settings.data.repository

import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    private val _settingsFlow = MutableStateFlow(Settings())
    
    override fun observeSettings(): Flow<Settings> = _settingsFlow.asStateFlow()
    
    override suspend fun getSettings(): Settings {
        return settingsDataStore.observeSettings().first()
    }
    
    override suspend fun updateLocation(location: LocationSettings) {
        settingsDataStore.updateLocation(location)
        _settingsFlow.value = settingsDataStore.observeSettings().first()
    }
    
    override suspend fun updateCalculationMethod(method: String) {
        settingsDataStore.updateCalculationMethod(method)
        _settingsFlow.value = settingsDataStore.observeSettings().first()
    }
    
    override suspend fun updateLanguage(language: String) {
        settingsDataStore.updateLanguage(language)
        _settingsFlow.value = settingsDataStore.observeSettings().first()
    }
    
    override suspend fun updateHijriAdjustment(days: Int) {
        settingsDataStore.updateHijriAdjustment(days)
        _settingsFlow.value = settingsDataStore.observeSettings().first()
    }
}
