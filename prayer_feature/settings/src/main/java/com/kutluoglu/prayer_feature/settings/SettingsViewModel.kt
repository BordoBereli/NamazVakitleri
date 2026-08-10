package com.kutluoglu.prayer_feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.usecases.prayer.ClearPrayerTimesCacheUseCase
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.usecase.ClearLocationCacheUseCase
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateLocationUseCase: UpdateLocationUseCase,
    private val updateCalculationMethodUseCase: UpdateCalculationMethodUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val updateHijriAdjustmentUseCase: UpdateHijriAdjustmentUseCase,
    private val clearLocationCacheUseCase: ClearLocationCacheUseCase,
    private val clearPrayerTimesCacheUseCase: ClearPrayerTimesCacheUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState>
        get() = _uiState.stateIn(
            scope = viewModelScope,
            initialValue = SettingsUiState.Loading,
            started = SharingStarted.WhileSubscribed(5_000)
        )

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings -> loadSettings()
            is SettingsEvent.UpdateLocation -> updateLocation(event.location)
            is SettingsEvent.UpdateCalculationMethod -> updateCalculationMethod(event.method)
            is SettingsEvent.UpdateLanguage -> updateLanguage(event.language)
            is SettingsEvent.UpdateHijriAdjustment -> updateHijriAdjustment(event.days)
            is SettingsEvent.ClearCache -> clearCache()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val settings = getSettingsUseCase()
                _uiState.value = SettingsUiState.Success(settings)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun updateLocation(location: LocationSettings) {
        viewModelScope.launch {
            try {
                updateLocationUseCase(location)
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update location")
            }
        }
    }

    private fun updateCalculationMethod(method: String) {
        viewModelScope.launch {
            try {
                updateCalculationMethodUseCase(method)
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update calculation method")
            }
        }
    }

    private fun updateLanguage(language: String) {
        viewModelScope.launch {
            try {
                updateLanguageUseCase(language)
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update language")
            }
        }
    }

    private fun updateHijriAdjustment(days: Int) {
        viewModelScope.launch {
            try {
                updateHijriAdjustmentUseCase(days)
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update hijri adjustment")
            }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            try {
                clearLocationCacheUseCase()
                clearPrayerTimesCacheUseCase()
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to clear cache")
            }
        }
    }
}
