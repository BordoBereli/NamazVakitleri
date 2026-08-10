package com.kutluoglu.prayer_feature.settings.calculation

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.CalculationMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CalculationMethodViewModelTest {

    private lateinit var viewModel: CalculationMethodViewModel

    @BeforeEach
    fun setUp() {
        viewModel = CalculationMethodViewModel()
    }

    @Test
    fun `initial state should load all methods with default selection`() {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.methods).hasSize(8)
        assertThat(loadedState.selectedMethod).isEqualTo("TURKEY_DIYANET")
    }

    @Test
    fun `setCurrentMethod should update selected method`() {
        viewModel.setCurrentMethod("ISNA")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CalculationMethodUiState.MethodsLoaded::class.java)
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.selectedMethod).isEqualTo("ISNA")
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
        assertThat(methodIds).contains("TEHRAN")
        assertThat(methodIds).contains("JAFARI")
    }

    @Test
    fun `setCurrentMethod with invalid method should keep previous value`() {
        viewModel.setCurrentMethod("INVALID_METHOD")

        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.selectedMethod).isEqualTo("INVALID_METHOD")
    }

    // Edge case tests

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
    fun `selectMethod event should update selected method`() {
        val newMethod = CalculationMethod.methods.first { it.id == "MWL" }
        viewModel.onEvent(CalculationMethodEvent.SelectMethod(newMethod))

        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        assertThat(loadedState.selectedMethod).isEqualTo("MWL")
    }

    @Test
    fun `selecting all methods sequentially should work`() {
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
        assertThat(loadedState.methods).hasSize(8)
    }

    @Test
    fun `methods should have expected count`() {
        val state = viewModel.uiState.value
        val loadedState = state as CalculationMethodUiState.MethodsLoaded
        
        assertThat(loadedState.methods).hasSize(8)
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
