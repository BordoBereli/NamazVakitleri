package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SavedVersesSortOrder

sealed class SavedVersesEvent {
    data class OnRemove(val verse: AyahData) : SavedVersesEvent()
    data class OnReorderGroups(val groups: List<SavedVerseGroup>) : SavedVersesEvent()
    data class OnReorderWithinGroup(val surahNumber: Int, val verses: List<AyahData>) : SavedVersesEvent()
    data class OnToggleCollapse(val surahNumber: Int) : SavedVersesEvent()
    data object OnExpandAll : SavedVersesEvent()
    data object OnCollapseAll : SavedVersesEvent()
    data class OnChangeSortOrder(val order: SavedVersesSortOrder) : SavedVersesEvent()
    data class OnSearch(val query: String) : SavedVersesEvent()
    data class OnSelect(val verse: AyahData) : SavedVersesEvent()
    data object OnDismissDetail : SavedVersesEvent()
}
