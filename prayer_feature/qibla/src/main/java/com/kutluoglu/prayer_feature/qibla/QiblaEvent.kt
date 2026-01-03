package com.kutluoglu.prayer_feature.qibla


/**
 * Created by F.K. on 1.01.2026.
 *
 */

sealed interface QiblaEvent {
    object OnStart : QiblaEvent
    object OnStop : QiblaEvent
}