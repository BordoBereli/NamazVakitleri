package com.kutluoglu.prayer_feature.settings.language

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class LanguageSelectionViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateLanguageUseCase: UpdateLanguageUseCase
    private lateinit var viewModel: LanguageSelectionViewModel

    @BeforeEach
    fun setUp() {
        getSettingsUseCase = mockk()
        updateLanguageUseCase = mockk()
        coEvery { getSettingsUseCase() } returns Settings()
        coEvery { updateLanguageUseCase(any()) } returns Unit
        viewModel = LanguageSelectionViewModel(getSettingsUseCase, updateLanguageUseCase)
    }

    @Test
    fun `init loads current language from settings and pre-selects it`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(language = "en")

        val viewModel = LanguageSelectionViewModel(getSettingsUseCase, updateLanguageUseCase)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LanguageUiState.LanguagesLoaded::class.java)
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.selectedLanguage).isEqualTo("en")
    }

    @Test
    fun `selectLanguage persists the selected language`() = runTest {
        val english = languages.first { it.code == "en" }
        viewModel.onEvent(LanguageEvent.SelectLanguage(english))

        coVerify { updateLanguageUseCase("en") }
    }

    @Test
    fun `selectLanguage updates selected language in state`() = runTest {
        val english = languages.first { it.code == "en" }
        viewModel.onEvent(LanguageEvent.SelectLanguage(english))

        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.selectedLanguage).isEqualTo("en")
    }

    @Test
    fun `initial state should load all languages with default selection`() {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LanguageUiState.LanguagesLoaded::class.java)
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.languages).hasSize(16)
        assertThat(loadedState.selectedLanguage).isEqualTo("system")
    }

    @Test
    fun `languages should contain system entry first`() {
        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded
        assertThat(loadedState.languages.first().code).isEqualTo("system")
    }

    @Test
    fun `selecting system entry persists system`() = runTest {
        val system = languages.first { it.code == "system" }
        viewModel.onEvent(LanguageEvent.SelectLanguage(system))
        coVerify { updateLanguageUseCase("system") }
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
    fun `each language should have name and nativeName`() {
        val state = viewModel.uiState.value
        val loadedState = state as LanguageUiState.LanguagesLoaded

        loadedState.languages.forEach { language ->
            assertThat(language.name).isNotEmpty()
            assertThat(language.nativeName).isNotEmpty()
        }
    }
}
