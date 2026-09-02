package com.kutluoglu.prayer_feature.settings.imsak

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateImsakOffsetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ImsakOffsetViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateImsakOffsetUseCase: UpdateImsakOffsetUseCase
) : ViewModel() {

    private val _currentOffset = MutableStateFlow(10)
    val currentOffset: StateFlow<Int> = _currentOffset.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _currentOffset.value = getSettingsUseCase().imsakOffsetMinutes
            } catch (e: Exception) {
                Log.e("ImsakOffsetVM", "Failed to load current offset -> ${e.message}")
            }
        }
    }

    fun onEvent(event: ImsakOffsetEvent) {
        when (event) {
            is ImsakOffsetEvent.OnOffsetChanged -> _currentOffset.value = event.minutes.coerceIn(5, 20)
            ImsakOffsetEvent.OnConfirm -> viewModelScope.launch {
                updateImsakOffsetUseCase(_currentOffset.value)
            }
        }
    }
}
