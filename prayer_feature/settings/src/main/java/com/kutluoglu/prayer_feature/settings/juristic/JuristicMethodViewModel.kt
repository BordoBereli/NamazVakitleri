package com.kutluoglu.prayer_feature.settings.juristic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateJuristicMethodUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class JuristicMethodViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateJuristicMethodUseCase: UpdateJuristicMethodUseCase
) : ViewModel() {

    private val _currentMethod = MutableStateFlow("STANDARD")
    val currentMethod: StateFlow<String> = _currentMethod.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _currentMethod.value = getSettingsUseCase().juristicMethod
            } catch (e: Exception) {
                Log.e("JuristicMethodVM", "Failed to load current method -> ${e.message}")
            }
        }
    }

    fun onEvent(event: JuristicMethodEvent) {
        when (event) {
            is JuristicMethodEvent.SelectMethod -> {
                _currentMethod.value = event.method
                viewModelScope.launch { updateJuristicMethodUseCase(event.method) }
            }
        }
    }
}
