package com.kutluoglu.prayer_feature.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<LanguageUiState>(LanguageUiState.Loading)
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableSharedFlow<String>()
    val selectedLanguage: SharedFlow<String> = _selectedLanguage.asSharedFlow()

    private var currentLanguageCode: String = "tr"

    init {
        loadLanguages()
    }

    fun setCurrentLanguage(languageCode: String) {
        currentLanguageCode = languageCode
        loadLanguages()
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
            _selectedLanguage.emit(language.code)
        }
    }
}
