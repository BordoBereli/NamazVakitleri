package com.kutluoglu.prayer_feature.settings.hijri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HijriAdjustmentViewModel(
) : ViewModel() {

    private val _confirmedAdjustment = MutableSharedFlow<Int>()
    val confirmedAdjustment: SharedFlow<Int> = _confirmedAdjustment.asSharedFlow()

    fun confirmAdjustment(days: Int) {
        viewModelScope.launch {
            _confirmedAdjustment.emit(days)
        }
    }
}
