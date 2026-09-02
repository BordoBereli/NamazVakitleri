package com.kutluoglu.prayer_feature.settings.imsak

sealed interface ImsakOffsetEvent {
    data class OnOffsetChanged(val minutes: Int) : ImsakOffsetEvent
    data object OnConfirm : ImsakOffsetEvent
}
