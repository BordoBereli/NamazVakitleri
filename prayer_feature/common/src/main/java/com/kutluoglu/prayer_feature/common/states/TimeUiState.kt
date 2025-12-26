package com.kutluoglu.prayer_feature.common.states

data class TimeUiState(
        val hijriDate: String = "",
        val gregorianFullDate: String = "",
        val gregorianShortDate: String = "",
        val currentTime: String = ""
)