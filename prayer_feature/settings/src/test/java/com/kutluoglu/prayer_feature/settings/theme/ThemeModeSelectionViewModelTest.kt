package com.kutluoglu.prayer_feature.settings.theme

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateThemeModeUseCase
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
class ThemeModeSelectionViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateThemeModeUseCase: UpdateThemeModeUseCase
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private lateinit var viewModel: ThemeModeSelectionViewModel

    @BeforeEach
    fun setUp() {
        getSettingsUseCase = mockk()
        updateThemeModeUseCase = mockk()
        coEvery { getSettingsUseCase() } returns Settings()
        coEvery { updateThemeModeUseCase(any()) } returns Unit
        viewModel = ThemeModeSelectionViewModel(getSettingsUseCase, updateThemeModeUseCase, analyticsTracker)
    }

    @Test
    fun `init loads current theme mode and pre-selects it`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(themeMode = "light")

        val viewModel = ThemeModeSelectionViewModel(getSettingsUseCase, updateThemeModeUseCase, analyticsTracker)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ThemeModeUiState.ThemeModesLoaded::class.java)
        val loadedState = state as ThemeModeUiState.ThemeModesLoaded
        assertThat(loadedState.selectedThemeMode).isEqualTo("light")
    }

    @Test
    fun `selectThemeMode persists the selected mode`() = runTest {
        val light = themeModes.first { it.id == "light" }
        viewModel.onEvent(ThemeModeEvent.SelectThemeMode(light))

        coVerify { updateThemeModeUseCase("light") }
    }

    @Test
    fun `selectThemeMode updates selected mode in state`() = runTest {
        val light = themeModes.first { it.id == "light" }
        viewModel.onEvent(ThemeModeEvent.SelectThemeMode(light))

        val state = viewModel.uiState.value
        val loadedState = state as ThemeModeUiState.ThemeModesLoaded
        assertThat(loadedState.selectedThemeMode).isEqualTo("light")
    }

    @Test
    fun `initial state loads all theme modes with dark default`() {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ThemeModeUiState.ThemeModesLoaded::class.java)
        val loadedState = state as ThemeModeUiState.ThemeModesLoaded
        assertThat(loadedState.themeModes).hasSize(3)
        assertThat(loadedState.selectedThemeMode).isEqualTo("dark")
    }

    @Test
    fun `theme modes contain dark light and system`() {
        val state = viewModel.uiState.value
        val loadedState = state as ThemeModeUiState.ThemeModesLoaded
        val ids = loadedState.themeModes.map { it.id }

        assertThat(ids).containsExactly("dark", "light", "system")
    }
}
