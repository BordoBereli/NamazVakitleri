package com.kutluoglu.prayer_feature.settings.calculation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.model.CalculationMethod
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
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
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateCalculationMethodUseCase: UpdateCalculationMethodUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalculationMethodUiState>(CalculationMethodUiState.Loading)
    val uiState: StateFlow<CalculationMethodUiState> = _uiState.asStateFlow()

    private val _selectedMethod = MutableSharedFlow<String>()
    val selectedMethod: SharedFlow<String> = _selectedMethod.asSharedFlow()

    private var currentMethodId: String = "TURKEY_DIYANET"

    init {
        loadCurrentMethod()
    }

    private fun loadCurrentMethod() {
        viewModelScope.launch {
            try {
                currentMethodId = getSettingsUseCase().calculationMethod
            } catch (e: Exception) {
                Log.e("CalculationMethodVM", "Failed to load current method -> ${e.message}")
            }
            loadMethods()
        }
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
            updateCalculationMethodUseCase(method.id)
            _selectedMethod.emit(method.id)
        }
    }
}
