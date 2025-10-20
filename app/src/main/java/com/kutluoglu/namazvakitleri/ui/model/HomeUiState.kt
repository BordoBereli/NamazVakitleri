package com.kutluoglu.namazvakitleri.ui.model

import com.kutluoglu.prayer.model.Prayer

data class HomeUiState(
    val prayers: List<Prayer> = emptyList(),
    val currentPrayer: Prayer? = null,
    val nextPrayer: Prayer? = null,
    val timeRemaining: String = "",
    val hijriDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)
