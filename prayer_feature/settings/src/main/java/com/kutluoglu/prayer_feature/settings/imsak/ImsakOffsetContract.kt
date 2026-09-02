package com.kutluoglu.prayer_feature.settings.imsak

sealed interface ImsakOffsetEvent {
    data class OnConfirm(val minutes: Int) : ImsakOffsetEvent
}
