package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup

sealed class SavedVersesUiState {
    data object Loading : SavedVersesUiState()
    data class Success(
        val groups: List<SavedVerseGroup>,
        val filteredGroups: List<SavedVerseGroup>,
        val collapsedSurahs: Set<Int>,
        val query: String = "",
        val selectedVerse: AyahData? = null,
        val isDetailVisible: Boolean = false
    ) : SavedVersesUiState()
    data class Error(val message: String) : SavedVersesUiState()
}
