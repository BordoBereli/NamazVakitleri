package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup

sealed class SavedVersesEvent {
    data class OnRemove(val verse: AyahData) : SavedVersesEvent()
    data class OnReorderGroups(val groups: List<SavedVerseGroup>) : SavedVersesEvent()
    data class OnReorderWithinGroup(val surahNumber: Int, val verses: List<AyahData>) : SavedVersesEvent()
    data class OnToggleCollapse(val surahNumber: Int) : SavedVersesEvent()
    data class OnSearch(val query: String) : SavedVersesEvent()
    data class OnSelect(val verse: AyahData) : SavedVersesEvent()
    data object OnDismissDetail : SavedVersesEvent()
}
