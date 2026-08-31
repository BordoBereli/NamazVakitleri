package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.quran.AyahData

sealed class SavedVersesUiState {
    data object Loading : SavedVersesUiState()
    data class Success(
        val verses: List<AyahData>,
        val selectedVerse: AyahData? = null,
        val isDetailVisible: Boolean = false
    ) : SavedVersesUiState()
    data class Error(val message: String) : SavedVersesUiState()
}
