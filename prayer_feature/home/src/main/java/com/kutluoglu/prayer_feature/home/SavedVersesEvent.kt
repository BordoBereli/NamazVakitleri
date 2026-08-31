package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData

sealed class SavedVersesEvent {
    data class OnRemove(val verse: AyahData) : SavedVersesEvent()
    data class OnReorder(val verses: List<AyahData>) : SavedVersesEvent()
    data class OnSelect(val verse: AyahData) : SavedVersesEvent()
    data object OnDismissDetail : SavedVersesEvent()
}
