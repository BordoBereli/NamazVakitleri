package com.kutluoglu.prayer_feature.settings.hijri

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HijriAdjustmentViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateHijriAdjustmentUseCase: UpdateHijriAdjustmentUseCase
) : ViewModel() {

    private val _currentAdjustment = MutableStateFlow(0)
    val currentAdjustment: StateFlow<Int> = _currentAdjustment.asStateFlow()

    private val _confirmedAdjustment = MutableSharedFlow<Int>()
    val confirmedAdjustment: SharedFlow<Int> = _confirmedAdjustment.asSharedFlow()

    init {
        loadCurrentAdjustment()
    }

    private fun loadCurrentAdjustment() {
        viewModelScope.launch {
            try {
                _currentAdjustment.value = getSettingsUseCase().hijriAdjustment
            } catch (e: Exception) {
                Log.e("HijriAdjustmentVM", "Failed to load current adjustment -> ${e.message}")
            }
        }
    }

    fun confirmAdjustment(days: Int) {
        viewModelScope.launch {
            updateHijriAdjustmentUseCase(days)
            _confirmedAdjustment.emit(days)
        }
    }
}
