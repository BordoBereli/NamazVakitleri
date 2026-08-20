package com.kutluoglu.prayer_feature.settings.calculation

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.CalculationMethod
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
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
class CalculationMethodViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateCalculationMethodUseCase: UpdateCalculationMethodUseCase
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private lateinit var viewModel: CalculationMethodViewModel

    @BeforeEach
    fun setUp() {
        getSettingsUseCase = mockk()
        updateCalculationMethodUseCase = mockk()
        coEvery { getSettingsUseCase() } returns Settings()
        coEvery { updateCalculationMethodUseCase(any()) } returns Unit
        viewModel = CalculationMethodViewModel(getSettingsUseCase, updateCalculationMethodUseCase, analyticsTracker)
    }

    @Test
    fun `init loads current method from settings and pre-selects it`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(calculationMethod = "ISNA")

        val viewModel = CalculationMethodViewModel(getSettingsUseCase, updateCalculationMethodUseCase, analyticsTracker)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.selectedMethod).isEqualTo("ISNA")
    }

    @Test
    fun `selectMethod persists the selected method`() = runTest {
        val newMethod = CalculationMethod.methods.first { it.id == "MWL" }
        viewModel.onEvent(CalculationMethodEvent.SelectMethod(newMethod))

        coVerify { updateCalculationMethodUseCase("MWL") }
    }

    @Test
    fun `selectMethod updates selected method in state`() = runTest {
        val newMethod = CalculationMethod.methods.first { it.id == "MWL" }
        viewModel.onEvent(CalculationMethodEvent.SelectMethod(newMethod))

        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.selectedMethod).isEqualTo("MWL")
    }

    @Test
    fun `initial state should load all methods with default selection`() {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.methods).hasSize(6)
        assertThat(loadedState.selectedMethod).isEqualTo("TURKEY_DIYANET")
    }

    @Test
    fun `methods should contain all expected calculation methods`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        val methodIds = loadedState.methods.map { it.id }

        assertThat(methodIds).contains("TURKEY_DIYANET")
        assertThat(methodIds).contains("MWL")
        assertThat(methodIds).contains("ISNA")
        assertThat(methodIds).contains("EGYPT")
        assertThat(methodIds).contains("MAKKAH")
        assertThat(methodIds).contains("KARACHI")
    }

    @Test
    fun `initial state methods should not be empty`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.methods).isNotEmpty()
    }

    @Test
    fun `all methods should have valid id and name`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded

        loadedState.methods.forEach { method ->
            assertThat(method.id).isNotEmpty()
            assertThat(method.name).isNotEmpty()
        }
    }

    @Test
    fun `selecting all methods sequentially should work`() = runTest {
        CalculationMethod.methods.forEach { method ->
            viewModel.onEvent(CalculationMethodEvent.SelectMethod(method))

            val state = viewModel.uiState.value
            val loadedState = state as CalculationMethodUiState.MethodsLoaded
            assertThat(loadedState.selectedMethod).isEqualTo(method.id)
        }
    }

    @Test
    fun `retry should reload methods`() {
        viewModel.onEvent(CalculationMethodEvent.Retry)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.methods).hasSize(6)
    }

    @Test
    fun `methods should have expected count`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded

        assertThat(loadedState.methods).hasSize(6)
    }

    @Test
    fun `each method should have unique id`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded

        val ids = loadedState.methods.map { it.id }
        assertThat(ids.toSet()).hasSize(ids.size)
    }

    @Test
    fun `default selection should be first method in list`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded

        assertThat(loadedState.selectedMethod).isEqualTo(loadedState.methods.first().id)
    }

    @Test
    fun `methods should include Turkey method`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded

        val turkeyMethod = loadedState.methods.find { it.id == "TURKEY_DIYANET" }
        assertThat(turkeyMethod).isNotNull()
        assertThat(turkeyMethod?.name).contains("Turkey")
    }
}
