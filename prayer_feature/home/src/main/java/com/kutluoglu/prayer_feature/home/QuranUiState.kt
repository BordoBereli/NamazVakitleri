package com.kutluoglu.prayer_feature.home

import com.kutluoglu.prayer.model.quran.AyahData

data class QuranUiState(
    val verse: AyahData? = null,
    val isSheetVisible: Boolean = false
)
