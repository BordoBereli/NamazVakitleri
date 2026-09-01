package com.kutluoglu.prayer.model.quran

import kotlinx.serialization.Serializable

@Serializable
data class SavedVerseGroup(
    val surah: SurahInfo,
    val verses: List<AyahData>
)
