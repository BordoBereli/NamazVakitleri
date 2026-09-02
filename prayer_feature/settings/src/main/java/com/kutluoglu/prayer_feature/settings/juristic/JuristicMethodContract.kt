package com.kutluoglu.prayer_feature.settings.juristic

sealed interface JuristicMethodEvent {
    data class SelectMethod(val method: String) : JuristicMethodEvent
}
