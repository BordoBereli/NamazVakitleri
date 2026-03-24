package com.kutluoglu.prayer_feature.settings.calculation

import com.kutluoglu.prayer_settings.domain.model.CalculationMethod

sealed class CalculationMethodUiState {
    data object Loading : CalculationMethodUiState()
    data class MethodsLoaded(val methods: List<CalculationMethod>, val selectedMethod: String) : CalculationMethodUiState()
    data class Error(val message: String) : CalculationMethodUiState()
}

sealed class CalculationMethodEvent {
    data object Retry : CalculationMethodEvent()
    data class SelectMethod(val method: CalculationMethod) : CalculationMethodEvent()
}
