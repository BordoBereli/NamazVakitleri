package com.kutluoglu.prayer_feature.home.state

data class CountdownUiState(
    val timeRemaining: String = "--:--:--",
    val currentTime: String = ""
)
