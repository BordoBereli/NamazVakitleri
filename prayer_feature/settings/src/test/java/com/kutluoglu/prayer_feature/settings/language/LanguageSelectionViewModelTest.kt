package com.kutluoglu.prayer_feature.settings.language

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LanguageSelectionViewModelTest {

    private lateinit var viewModel: LanguageSelectionViewModel

    @BeforeEach
    fun setUp() {
        viewModel = LanguageSelectionViewModel()
    }

    @Test
    fun `initial state should load all languages with default selection`() {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LanguageUiState.LanguagesLoaded::class.java)
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.languages).hasSize(15)
        assertThat(loadedState.selectedLanguage).isEqualTo("tr")
    }

    @Test
    fun `setCurrentLanguage should update selected language`() {
        viewModel.setCurrentLanguage("en")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LanguageUiState.LanguagesLoaded::class.java)
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.selectedLanguage).isEqualTo("en")
    }

    @Test
    fun `languages should contain Turkish English and Arabic`() {
        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded
        val languageCodes = loadedState.languages.map { it.code }
        
        assertThat(languageCodes).contains("tr")
        assertThat(languageCodes).contains("en")
        assertThat(languageCodes).contains("ar")
    }

    @Test
    fun `setCurrentLanguage with invalid code should update to that code`() {
        viewModel.setCurrentLanguage("xx")

        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.selectedLanguage).isEqualTo("xx")
    }

    @Test
    fun `each language should have name and nativeName`() {
        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded
        
        loadedState.languages.forEach { language ->
            assertThat(language.name).isNotEmpty()
            assertThat(language.nativeName).isNotEmpty()
        }
    }
}
