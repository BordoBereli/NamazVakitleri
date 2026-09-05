package com.kutluoglu.prayer_feature.qibla


sealed interface QiblaEvent {
    object OnStart : QiblaEvent
    object OnStop : QiblaEvent
    object ToggleLockPortrait : QiblaEvent
    object ToggleCompassAutoRotate : QiblaEvent
}
