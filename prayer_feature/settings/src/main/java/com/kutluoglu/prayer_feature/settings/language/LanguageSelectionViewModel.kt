package com.kutluoglu.prayer_feature.settings.language

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LanguageSelectionViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LanguageUiState>(LanguageUiState.Loading)
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableSharedFlow<String>()
    val selectedLanguage: SharedFlow<String> = _selectedLanguage.asSharedFlow()

    private var currentLanguageCode: String = "system"

    init {
        loadCurrentLanguage()
    }

    private fun loadCurrentLanguage() {
        viewModelScope.launch {
            try {
                currentLanguageCode = getSettingsUseCase().language
            } catch (e: Exception) {
                Log.e("LanguageSelectionVM", "Failed to load current language -> ${e.message}")
            }
            loadLanguages()
        }
    }

    fun onEvent(event: LanguageEvent) {
        when (event) {
            is LanguageEvent.SelectLanguage -> selectLanguage(event.language)
        }
    }

    private fun loadLanguages() {
        _uiState.value = LanguageUiState.LanguagesLoaded(
            languages = languages,
            selectedLanguage = currentLanguageCode
        )
    }

    private fun selectLanguage(language: Language) {
        currentLanguageCode = language.code
        _uiState.value = LanguageUiState.LanguagesLoaded(
            languages = languages,
            selectedLanguage = language.code
        )
        viewModelScope.launch {
            updateLanguageUseCase(language.code)
            _selectedLanguage.emit(language.code)
        }
    }
}
