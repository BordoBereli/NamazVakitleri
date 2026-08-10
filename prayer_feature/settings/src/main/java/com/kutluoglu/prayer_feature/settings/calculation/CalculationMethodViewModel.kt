package com.kutluoglu.prayer_feature.settings.calculation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.model.CalculationMethod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CalculationMethodViewModel(
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalculationMethodUiState>(CalculationMethodUiState.Loading)
    val uiState: StateFlow<CalculationMethodUiState> = _uiState.asStateFlow()

    private val _selectedMethod = MutableSharedFlow<String>()
    val selectedMethod: SharedFlow<String> = _selectedMethod.asSharedFlow()

    private var currentMethodId: String = "TURKEY_DIYANET"

    init {
        loadMethods()
    }

    fun setCurrentMethod(methodId: String) {
        currentMethodId = methodId
        loadMethods()
    }

    fun onEvent(event: CalculationMethodEvent) {
        when (event) {
            is CalculationMethodEvent.Retry -> loadMethods()
            is CalculationMethodEvent.SelectMethod -> selectMethod(event.method)
        }
    }

    private fun loadMethods() {
        try {
            _uiState.value = CalculationMethodUiState.MethodsLoaded(
                methods = CalculationMethod.methods,
                selectedMethod = currentMethodId
            )
        } catch (e: Exception) {
            _uiState.value = CalculationMethodUiState.Error(e.message ?: "Failed to load methods")
        }
    }

    private fun selectMethod(method: CalculationMethod) {
        currentMethodId = method.id
        _uiState.value = CalculationMethodUiState.MethodsLoaded(
            methods = CalculationMethod.methods,
            selectedMethod = method.id
        )
        viewModelScope.launch {
            _selectedMethod.emit(method.id)
        }
    }
}
