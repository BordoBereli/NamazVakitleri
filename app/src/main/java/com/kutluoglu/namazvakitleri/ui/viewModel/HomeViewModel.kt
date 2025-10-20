package com.kutluoglu.namazvakitleri.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.namazvakitleri.ui.model.HomeUiState
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Using placeholder location values for now
            val latitude = 41.0082
            val longitude = 28.9784

            getPrayerTimesUseCase(LocalDate.now(), latitude, longitude)
                .onSuccess { prayerTimes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prayers = prayerTimes,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "An unknown error occurred"
                        )
                    }
                }
        }
    }
}